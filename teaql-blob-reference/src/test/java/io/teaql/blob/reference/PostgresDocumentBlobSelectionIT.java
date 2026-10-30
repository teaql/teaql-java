package io.teaql.blob.reference;

import io.teaql.core.EntityKey;
import io.teaql.core.MutationOperation;
import io.teaql.core.MutationOperationKind;
import io.teaql.core.MutationPlan;
import io.teaql.core.TransactionCompletionException;
import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobException;
import io.teaql.core.blob.BlobMutationParticipant;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.reference.BlobSelectionClaim;
import io.teaql.core.reference.BlobSelectionTarget;
import io.teaql.core.reference.BoundDocumentSnapshot;
import io.teaql.core.reference.DocumentBlobBindingSnapshot;
import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceCodec;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.secure.document.reference.AeadRoundTripReferenceProvider;
import io.teaql.secure.document.reference.DeploymentProfile;
import io.teaql.secure.document.reference.RoundTripReferenceKey;
import io.teaql.secure.document.reference.StaticRoundTripReferenceKeyProvider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static org.junit.Assert.*;

/** Real tqd2/tqs1 credentials and a caller-owned PostgreSQL business transaction. */
public class PostgresDocumentBlobSelectionIT {
    private static final EntityKey ENTITY=new EntityKey("Asset",42L);
    private static final String COMMAND="command-1";

    private static DefaultUserContext context(Fixture f,AtomicBoolean allowed,String actor) {
        var provider=AeadRoundTripReferenceProvider.governed(DeploymentProfile.TEST,"blob-it","test",
                new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("k1",new byte[32])),
                (current,principal,scope,identity) -> {
                    if (!allowed.get()) throw new RoundTripReferenceException(
                            RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,"AUTHORIZATION_REQUIRED");
                });
        DefaultUserContext context=f.context();
        context.putAttribute(RoundTripReferenceCodec.class.getName(),new RoundTripReferenceCodec(provider));
        context.putAttribute("actor",actor);
        context.withTrustedReferencePrincipal(new TrustedReferencePrincipal("oidc",actor,"Tenant",7));
        return context;
    }

    private static PostgresDocumentBlobSelectionParticipant participant(Fixture f) {
        return participant(f,false);
    }

    private static PostgresDocumentBlobSelectionParticipant participant(Fixture f,boolean allowCreate) {
        var authorization=new BlobBindingAuthorization(
                current -> current.getAttribute("scope",io.teaql.core.blob.BlobScope.class),
                (current,target,kind) -> !Boolean.FALSE.equals(current.getAttribute("field")),
                (current,target,blob) -> !Boolean.FALSE.equals(current.getAttribute("policy")));
        return new PostgresDocumentBlobSelectionParticipant(f.sql,f.catalog,f.participant,authorization,
                new PostgresDocumentBlobSelectionParticipant.DocumentAuthority() {
                    @Override public boolean sameActor(UserContext current,TrustedReferencePrincipal principal,
                            io.teaql.core.blob.BlobScope scope) {
                        return SCOPE.equals(scope) && principal.subject().equals(current.getAttribute("actor"));
                    }
                    @Override public boolean canCreate(UserContext current,BoundDocumentSnapshot document,
                            String clientKey,EntityKey resolvedEntity) {
                        return allowCreate && "client-43".equals(clientKey)
                                && new EntityKey("Asset",43L).equals(resolvedEntity);
                    }
                });
    }

    private static BoundDocumentSnapshot snapshot(Fixture f,BlobRef old) {
        var scope=new ReferenceDocumentScope("asset-edit-42","edit-asset","Asset",42,1);
        var row=new DocumentSnapshot.Entity("asset:42",new InternalEntityIdentity("Asset",42,1),
                Set.of("document","title"),Set.of("document","title"),
                Set.of(DocumentSnapshot.MutationKind.UPDATE));
        var document=new DocumentSnapshot("asset-model-v1","ASSET-42",scope,row.identity(),List.of(row),List.of());
        return new BoundDocumentSnapshot(document,List.of(new DocumentBlobBindingSnapshot("asset:42","document",
                "Asset",old,f.binding(42).version(),true,true)));
    }

    private static PostgresDocumentBlobSelectionParticipant.Request request(Fixture f,UserContext context,
            BlobRef old,BlobRef next) {
        BoundDocumentSnapshot document=snapshot(f,old);
        String token=context.issueBoundDocumentSnapshot(document,Duration.ofMinutes(5));
        var target=new BlobSelectionTarget(document.snapshot().scope(),"Asset","document",
                BlobSelectionTarget.Kind.EXISTING_ROW,"asset:42",COMMAND);
        String selection=context.issueBlobSelection(token,new BlobSelectionClaim(target,next),Duration.ofMinutes(2));
        return new PostgresDocumentBlobSelectionParticipant.Request(token,"Asset","edit-asset",COMMAND,
                List.of(new PostgresDocumentBlobSelectionParticipant.Selection(selection,target)),Map.of());
    }

    private static MutationPlan approved(BlobRef next) {
        return new MutationPlan(COMMAND,"request-1","Asset","change asset document",
                List.of(new MutationOperation(MutationOperationKind.UPDATE,ENTITY,1L,Map.of("document",next))));
    }

    private static void apply(Fixture f,UserContext context,BlobMutationParticipant.Plan plan,BlobRef next) {
        plan.requireTransaction(f.sql);
        f.sql.executeInTransaction(() -> {
            long businessTx=f.scalar("SELECT txid_current()");
            BlobMutationParticipant.Prepared prepared=plan.prepare(context,f.sql);
            assertEquals(businessTx,f.scalar("SELECT txid_current()"));
            assertEquals(1,f.sql.update("UPDATE asset_data SET document=?,version=2 WHERE id=42 AND version=1",
                    new Object[]{next.key()}));
            prepared.apply(context,f.sql,List.of(new BlobMutationParticipant.PersistedEntity(
                    ENTITY,ENTITY,2L,MutationOperationKind.UPDATE)));
            assertEquals(businessTx,f.scalar("SELECT txid_current()"));
        });
    }

    @Test public void replacementUsesAuthenticatedChoiceAndSamePhysicalTransaction() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var allowed=new AtomicBoolean(true); var context=context(f,allowed,"alice");
            var request=request(f,context,old,next);
            var plan=participant(f).plan(context,approved(next),request);
            apply(f,context,plan,next);
            f.assertOracle(42,next);
            assertEquals(2L,f.binding(42).version());
        }
    }

    @Test public void staleIssuedBindingFailsAfterConcurrentReplacement() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(),other=f.upload(); f.create(42,old);
            var context=context(f,new AtomicBoolean(true),"alice");
            var plan=participant(f).plan(context,approved(next),request(f,context,old,next));
            var asset=f.load(42); asset.updateProperty("document",other); f.save(asset);
            var failure=assertThrows(BlobException.class,() -> f.sql.executeInTransaction(
                    () -> plan.prepare(context,f.sql)));
            assertEquals(BlobException.Code.BLOB_IDEMPOTENCY_CONFLICT,failure.code());
            f.assertOracle(42,other);
        }
    }

    @Test public void missingOrRawSelectionAndOtherActorAreRejectedBeforeBusinessWrites() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var alice=context(f,new AtomicBoolean(true),"alice"); var bob=context(f,new AtomicBoolean(true),"bob");
            var request=request(f,alice,old,next);
            var missing=new PostgresDocumentBlobSelectionParticipant.Request(request.documentToken(),"Asset",
                    "edit-asset",COMMAND,List.of(),Map.of());
            assertEquals(BlobException.Code.BLOB_SELECTION_INVALID,assertThrows(BlobException.class,
                    () -> participant(f).plan(alice,approved(next),missing)).code());
            var raw=new PostgresDocumentBlobSelectionParticipant.Request(request.documentToken(),"Asset",
                    "edit-asset",COMMAND,List.of(new PostgresDocumentBlobSelectionParticipant.Selection(
                            next.key(),request.selections().get(0).target())),Map.of());
            assertEquals(BlobException.Code.BLOB_SELECTION_INVALID,assertThrows(BlobException.class,
                    () -> participant(f).plan(alice,approved(next),raw)).code());
            assertEquals(BlobException.Code.BLOB_SELECTION_SCOPE_MISMATCH,assertThrows(BlobException.class,
                    () -> participant(f).plan(bob,approved(next),request)).code());
            f.assertOracle(42,old);
        }
    }

    @Test public void caughtPrepareRevocationStillPoisonsOuterTransaction() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var allowed=new AtomicBoolean(true); var context=context(f,allowed,"alice");
            var plan=participant(f).plan(context,approved(next),request(f,context,old,next));
            assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.sql.update("UPDATE asset_data SET title='should rollback' WHERE id=42",new Object[0]);
                allowed.set(false);
                assertEquals(BlobException.Code.BLOB_BIND_DENIED,assertThrows(BlobException.class,
                        () -> plan.prepare(context,f.sql)).code());
            }));
            assertEquals("asset",f.row(42).get("title")); f.assertOracle(42,old);
        }
    }

    @Test public void wrongTransactionResourceCaughtByCallerStillRollsBack() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var context=context(f,new AtomicBoolean(true),"alice");
            var plan=participant(f).plan(context,approved(next),request(f,context,old,next));
            assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.sql.update("UPDATE asset_data SET title='should rollback' WHERE id=42",new Object[0]);
                assertEquals(BlobException.Code.BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,
                        () -> plan.prepare(context,new Object())).code());
            }));
            assertEquals("asset",f.row(42).get("title")); f.assertOracle(42,old);
        }
    }

    @Test public void revocationBetweenPrepareAndApplyRollsBackBusinessAndBinding() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var allowed=new AtomicBoolean(true); var context=context(f,allowed,"alice");
            var plan=participant(f).plan(context,approved(next),request(f,context,old,next));
            assertEquals(BlobException.Code.BLOB_BIND_DENIED,assertThrows(BlobException.class,
                    () -> f.sql.executeInTransaction(() -> {
                        var prepared=plan.prepare(context,f.sql);
                        f.sql.update("UPDATE asset_data SET document=?,version=2 WHERE id=42",new Object[]{next.key()});
                        allowed.set(false);
                        prepared.apply(context,f.sql,List.of(new BlobMutationParticipant.PersistedEntity(
                                ENTITY,ENTITY,2L,MutationOperationKind.UPDATE)));
                    })).code());
            f.assertOracle(42,old);
        }
    }

    @Test public void clientKeySelectionRequiresResolvedIdAndAuthorizedCompleteRelation() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var context=context(f,new AtomicBoolean(true),"alice");
            var parent=snapshot(f,old);
            var original=parent.snapshot();
            var relation=new DocumentSnapshot.Relation("assets",List.of(),
                    DocumentSnapshot.Completeness.COMPLETE,true,
                    Set.of(DocumentSnapshot.MutationKind.CREATE_CHILD));
            var document=new BoundDocumentSnapshot(new DocumentSnapshot(original.modelFingerprint(),
                    original.businessId(),original.scope(),original.aggregate(),
                    original.entities(),List.of(relation)),parent.bindings());
            String documentToken=context.issueBoundDocumentSnapshot(document,Duration.ofMinutes(5));
            var target=new BlobSelectionTarget(document.snapshot().scope(),"Asset","document",
                    BlobSelectionTarget.Kind.NEW_CLIENT_KEY,"client-43",COMMAND);
            String selection=context.issueBlobSelection(documentToken,new BlobSelectionClaim(target,next),Duration.ofMinutes(2));
            var selected=List.of(new PostgresDocumentBlobSelectionParticipant.Selection(selection,target));
            var planned=new EntityKey("Asset",43L);
            var approved=new MutationPlan(COMMAND,"request-1","Asset","create related asset",
                    List.of(new MutationOperation(MutationOperationKind.CREATE,planned,null,Map.of("document",next))));
            var unresolved=new PostgresDocumentBlobSelectionParticipant.Request(documentToken,"Asset",
                    "edit-asset",COMMAND,selected,Map.of());
            assertEquals(BlobException.Code.BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,
                    () -> participant(f,true).plan(context,approved,unresolved)).code());
            var request=new PostgresDocumentBlobSelectionParticipant.Request(documentToken,"Asset",
                    "edit-asset",COMMAND,selected,Map.of("client-43",planned));
            assertEquals(BlobException.Code.BLOB_BIND_DENIED,assertThrows(BlobException.class,
                    () -> participant(f).plan(context,approved,request)).code());
            var plan=participant(f,true).plan(context,approved,request);
            f.sql.executeInTransaction(() -> {
                var prepared=plan.prepare(context,f.sql);
                assertEquals(1,f.sql.update("INSERT INTO asset_data(id,version,document,title) VALUES(43,1,?,'child')",
                        new Object[]{next.key()}));
                prepared.apply(context,f.sql,List.of(new BlobMutationParticipant.PersistedEntity(
                        planned,planned,1L,MutationOperationKind.CREATE)));
            });
            f.assertOracle(43,next);
        }
    }
}
