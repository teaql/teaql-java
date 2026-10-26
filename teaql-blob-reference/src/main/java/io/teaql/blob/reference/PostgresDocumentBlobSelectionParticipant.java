package io.teaql.blob.reference;

import io.teaql.core.EntityKey;
import io.teaql.core.MutationOperationKind;
import io.teaql.core.MutationPlan;
import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobException;
import io.teaql.core.blob.BlobFieldTarget;
import io.teaql.core.blob.BlobMutationParticipant;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.blob.BlobScope;
import io.teaql.core.reference.BlobSelectionTarget;
import io.teaql.core.reference.BoundDocumentSnapshot;
import io.teaql.core.reference.DocumentBlobBindingSnapshot;
import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import static io.teaql.core.blob.BlobException.Code.*;

/**
 * Explicit document-file adapter for an application-reviewed mutation plan.
 * It never starts or commits a transaction; the host supplies the exact JDBC owner.
 */
public final class PostgresDocumentBlobSelectionParticipant {
    public interface DocumentAuthority {
        /** Explicit current mapping from document Actor to the Blob scope and host principal. */
        boolean sameActor(UserContext context, TrustedReferencePrincipal principal, BlobScope scope);
        /** The generic tqd2 relation has no child type; the host proves client-key membership. */
        boolean canCreate(UserContext context, BoundDocumentSnapshot document,
                String clientKey, EntityKey resolvedEntity);
    }

    public record Selection(String token, BlobSelectionTarget target) {
        public Selection {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(target, "target");
        }
        @Override public String toString() { return "Selection[redacted]"; }
    }

    /** Expected targets and resolved new IDs come from the submitted patch, not from tqs1. */
    public record Request(String documentToken, String aggregateType, String purpose,
            String commandId, List<Selection> selections, Map<String,EntityKey> newClientKeys) {
        public Request {
            Objects.requireNonNull(documentToken, "documentToken");
            Objects.requireNonNull(aggregateType, "aggregateType");
            Objects.requireNonNull(purpose, "purpose");
            Objects.requireNonNull(commandId, "commandId");
            selections=List.copyOf(selections);
            newClientKeys=Map.copyOf(newClientKeys);
        }
        @Override public String toString() { return "Request[redacted]"; }
    }

    private record Field(EntityKey entity,String name) {}
    private record Chosen(Selection selection,BlobRef reference) {}
    private record Issued(BlobFieldTarget target,DocumentBlobBindingSnapshot binding) {}
    private record NewRow(String clientKey,EntityKey entity) {}

    private final SqlExecutionAdapter sql;
    private final PostgresBlobCatalog catalog;
    private final PostgresBlobMutationParticipant delegate;
    private final BlobBindingAuthorization bindingAuthorization;
    private final DocumentAuthority documentAuthority;

    public PostgresDocumentBlobSelectionParticipant(SqlExecutionAdapter sql,
            PostgresBlobCatalog catalog,PostgresBlobMutationParticipant delegate,
            BlobBindingAuthorization bindingAuthorization,DocumentAuthority documentAuthority) {
        this.sql=Objects.requireNonNull(sql);
        this.catalog=Objects.requireNonNull(catalog);
        catalog.requireResource(sql);
        this.delegate=Objects.requireNonNull(delegate);
        this.bindingAuthorization=Objects.requireNonNull(bindingAuthorization);
        this.documentAuthority=Objects.requireNonNull(documentAuthority);
    }

    public BlobMutationParticipant.Plan plan(UserContext context,MutationPlan approved,Request request) {
        Objects.requireNonNull(context); Objects.requireNonNull(approved); Objects.requireNonNull(request);
        if (request.commandId().isBlank() || !request.commandId().equals(approved.executionId())
                || !request.aggregateType().equals(approved.rootEntityType())) throw selection();
        BoundDocumentSnapshot document=consumeDocument(context,request);
        BlobScope scope=requireActor(context);
        BlobBindingIntents intents=BlobBindingIntents.from(context,approved);
        Map<Field,Chosen> chosen=new HashMap<>();
        for (Selection input:request.selections()) {
            BlobSelectionTarget target=input.target();
            if (!target.commandId().equals(request.commandId())
                    || !target.scope().equals(document.snapshot().scope())) throw selection();
            BlobRef reference=consumeSelection(context,request,input);
            EntityKey entity;
            if (target.kind()==BlobSelectionTarget.Kind.EXISTING_ROW) {
                var identity=existing(document,target).identity();
                entity=new EntityKey(identity.entityType(),identity.id());
            } else {
                entity=newEntity(request,target);
            }
            if (chosen.putIfAbsent(new Field(entity,target.canonicalField()),new Chosen(input,reference))!=null) throw selection();
        }
        List<Issued> issued=new ArrayList<>(); List<NewRow> created=new ArrayList<>();
        Set<String> usedKeys=new HashSet<>();
        for (BlobBindingIntents.Mutation mutation:intents.mutations()) {
            if (mutation.kind()==MutationOperationKind.RECOVER) throw selection();
            String clientKey=null; DocumentSnapshot.Entity row=null;
            if (mutation.kind()==MutationOperationKind.CREATE) {
                clientKey=clientKey(request,mutation.entity());
                if (!createAllowed(context,document,clientKey,mutation.entity())) throw denied();
                usedKeys.add(clientKey); created.add(new NewRow(clientKey,mutation.entity()));
            } else {
                row=existing(document,mutation.entity());
                if (!Objects.equals(mutation.originalVersion(),row.identity().version())) throw selection();
                if (mutation.kind()==MutationOperationKind.DELETE && !removeAllowed(document,row.rowKey())) throw denied();
            }
            for (var entry:mutation.fields().entrySet()) {
                String field=entry.getKey();
                if (row!=null && mutation.kind()==MutationOperationKind.UPDATE
                        && (!row.writableFields().contains(field)
                            || !row.allowedMutations().contains(DocumentSnapshot.MutationKind.UPDATE))) throw denied();
                BlobRef next=mutation.kind()==MutationOperationKind.DELETE?null:mutation.assignments().get(field);
                Chosen credential=chosen.remove(new Field(mutation.entity(),field));
                if (next!=null) {
                    if (credential==null || !next.equals(credential.reference())
                            || credential.selection().target().kind()
                                !=(mutation.kind()==MutationOperationKind.CREATE
                                        ?BlobSelectionTarget.Kind.NEW_CLIENT_KEY:BlobSelectionTarget.Kind.EXISTING_ROW)
                            || row!=null && !row.rowKey().equals(credential.selection().target().targetKey())
                            || clientKey!=null && !clientKey.equals(credential.selection().target().targetKey())) throw selection();
                } else if (credential!=null) throw selection();
                if (row!=null) {
                    DocumentBlobBindingSnapshot binding=binding(document,row.rowKey(),field,
                            mutation.kind()!=MutationOperationKind.DELETE);
                    if (mutation.kind()==MutationOperationKind.UPDATE && next==null && !binding.nullable()) throw denied();
                    issued.add(new Issued(new BlobFieldTarget(mutation.entity().entity(),mutation.entity().id(),field),binding));
                }
            }
        }
        if (!chosen.isEmpty() || !usedKeys.equals(request.newClientKeys().keySet())) throw selection();
        BlobMutationParticipant.Plan base=delegate.plan(context,approved);
        List<Issued> frozenIssued=List.copyOf(issued); List<NewRow> frozenCreated=List.copyOf(created);
        List<Chosen> frozenSelections=request.selections().stream().map(input ->
                new Chosen(input,consumeSelection(context,request,input))).toList();
        return new BlobMutationParticipant.Plan() {
            @Override public void requireTransaction(Object resource) { base.requireTransaction(resource); }
            @Override public BlobMutationParticipant.Prepared prepare(UserContext current,Object resource) {
                return onOwner(resource,() -> {
                    validate(current,request,document,scope,frozenSelections,frozenCreated);
                    BlobMutationParticipant.Prepared prepared=base.prepare(current,resource);
                    for (Issued expected:frozenIssued) {
                        var actual=catalog.lockBinding(scope,expected.target());
                        if (actual.version()!=expected.binding().bindingVersion()
                                || !Objects.equals(actual.reference(),expected.binding().reference()))
                            throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
                    }
                    return (actor,owner,entities) -> onOwner(owner,() -> {
                        validate(actor,request,document,scope,frozenSelections,frozenCreated);
                        prepared.apply(actor,owner,entities);
                        return null;
                    });
                });
            }
        };
    }

    private void validate(UserContext context,Request request,BoundDocumentSnapshot expected,BlobScope scope,
            List<Chosen> selections,List<NewRow> created) {
        if (!scope.equals(requireActor(context)) || !expected.equals(consumeDocument(context,request))) throw denied();
        for (Chosen selected:selections) if (!selected.reference().equals(consumeSelection(context,request,selected.selection()))) throw selection();
        for (NewRow row:created) if (!createAllowed(context,expected,row.clientKey(),row.entity())) throw denied();
    }

    private BlobScope requireActor(UserContext context) {
        TrustedReferencePrincipal principal=context.trustedReferencePrincipal();
        BlobScope scope=bindingAuthorization.scope(context);
        if (!documentAuthority.sameActor(context,principal,scope)) throw denied();
        return scope;
    }

    private boolean createAllowed(UserContext context,BoundDocumentSnapshot document,
            String clientKey,EntityKey entity) {
        boolean complete=document.snapshot().relations().stream().anyMatch(relation ->
                relation.completeness()==DocumentSnapshot.Completeness.COMPLETE && relation.writable()
                && relation.allowedMutations().contains(DocumentSnapshot.MutationKind.CREATE_CHILD));
        return complete && documentAuthority.canCreate(context,document,clientKey,entity);
    }

    private static boolean removeAllowed(BoundDocumentSnapshot document,String rowKey) {
        return document.snapshot().relations().stream().anyMatch(relation ->
                relation.completeness()==DocumentSnapshot.Completeness.COMPLETE && relation.writable()
                && relation.rowKeys().contains(rowKey)
                && relation.allowedMutations().contains(DocumentSnapshot.MutationKind.REMOVE_CHILD));
    }

    private static String clientKey(Request request,EntityKey entity) {
        String found=null;
        for (var entry:request.newClientKeys().entrySet()) if (entry.getValue().equals(entity)) {
            if (found!=null) throw selection(); found=entry.getKey();
        }
        if (found==null || entity.id()==null || entity.id()<=0) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
        return found;
    }

    private static EntityKey newEntity(Request request,BlobSelectionTarget target) {
        EntityKey entity=request.newClientKeys().get(target.targetKey());
        if (entity==null || entity.id()==null || entity.id()<=0 || !entity.entity().equals(target.targetEntity()))
            throw new BlobException(BLOB_TRANSACTION_REQUIRED);
        return entity;
    }

    private static DocumentSnapshot.Entity existing(BoundDocumentSnapshot document,BlobSelectionTarget target) {
        DocumentSnapshot.Entity row=document.snapshot().entities().stream()
                .filter(entity -> entity.rowKey().equals(target.targetKey())).findFirst().orElseThrow(PostgresDocumentBlobSelectionParticipant::selection);
        if (!row.identity().entityType().equals(target.targetEntity())
                || !row.writableFields().contains(target.canonicalField())
                || !row.allowedMutations().contains(DocumentSnapshot.MutationKind.UPDATE)) throw denied();
        return row;
    }

    private static DocumentSnapshot.Entity existing(BoundDocumentSnapshot document,EntityKey entity) {
        List<DocumentSnapshot.Entity> rows=document.snapshot().entities().stream().filter(row ->
                row.identity().entityType().equals(entity.entity())
                && Objects.equals(row.identity().id(),entity.id())).toList();
        if (rows.size()!=1) throw denied();
        return rows.get(0);
    }

    private static DocumentBlobBindingSnapshot binding(BoundDocumentSnapshot document,String rowKey,String field,boolean writable) {
        return document.bindings().stream().filter(candidate -> candidate.rowKey().equals(rowKey)
                && candidate.canonicalField().equals(field) && (!writable || candidate.writable()))
                .findFirst().orElseThrow(PostgresDocumentBlobSelectionParticipant::denied);
    }

    private static BoundDocumentSnapshot consumeDocument(UserContext context,Request request) {
        try { return context.consumeBoundDocumentSnapshot(request.documentToken(),request.aggregateType(),request.purpose()).document(); }
        catch (RoundTripReferenceException failed) { throw mapped(failed); }
    }
    private static BlobRef consumeSelection(UserContext context,Request request,Selection selected) {
        try { return context.consumeBlobSelection(request.documentToken(),selected.token(),selected.target()).claim().reference(); }
        catch (RoundTripReferenceException failed) { throw mapped(failed); }
    }
    private static BlobException mapped(RoundTripReferenceException failed) {
        RoundTripReferenceErrorCode code=failed.getCode();
        if (code==RoundTripReferenceErrorCode.EXPIRED) return new BlobException(BLOB_SELECTION_EXPIRED);
        if (code==RoundTripReferenceErrorCode.SCOPE_MISMATCH) return new BlobException(BLOB_SELECTION_SCOPE_MISMATCH);
        if (code==RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED) return denied();
        return selection();
    }
    private <T> T onOwner(Object resource,Supplier<T> work) {
        if (!sql.hasActiveTransaction() || sql.activeTransactionIdentity()==null)
            throw new BlobException(BLOB_TRANSACTION_REQUIRED);
        List<T> result=new ArrayList<>(1);
        sql.executeInTransaction(() -> {
            catalog.requireActive(resource);
            result.add(work.get());
        });
        if (result.size()!=1) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
        return result.get(0);
    }
    private static BlobException selection() { return new BlobException(BLOB_SELECTION_INVALID); }
    private static BlobException denied() { return new BlobException(BLOB_BIND_DENIED); }
}
