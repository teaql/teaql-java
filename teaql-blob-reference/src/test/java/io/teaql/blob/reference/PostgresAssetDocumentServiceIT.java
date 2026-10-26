package io.teaql.blob.reference;

import io.teaql.core.EntityKey;
import io.teaql.core.MutationOperation;
import io.teaql.core.MutationOperationKind;
import io.teaql.core.MutationPlan;
import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobException;
import io.teaql.core.blob.BlobFieldTarget;
import io.teaql.core.blob.BlobMutationParticipant;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.blob.BlobScope;
import io.teaql.core.reference.BlobSelectionClaim;
import io.teaql.core.reference.BlobSelectionTarget;
import io.teaql.core.reference.BoundDocumentSnapshot;
import io.teaql.core.reference.ContextBoundDocumentService;
import io.teaql.core.reference.DocumentBlobBindingSnapshot;
import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.reference.VerifiedBoundDocumentSnapshot;
import io.teaql.core.view.ViewAuthorization;
import io.teaql.core.view.ViewOptions;
import io.teaql.core.view.ViewPreparationException;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.RuntimeTelemetry;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.secure.document.reference.AeadRoundTripReferenceProvider;
import io.teaql.secure.document.reference.DeploymentProfile;
import io.teaql.secure.document.reference.RoundTripReferenceKey;
import io.teaql.secure.document.reference.StaticRoundTripReferenceKeyProvider;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static org.junit.Assert.*;

/** Application-owned tqd2/tqs1 open/accept service over real business, binding and receipt rows. */
public class PostgresAssetDocumentServiceIT {
    private static final BlobFieldTarget FIELD=new BlobFieldTarget("Asset",42L,"document");
    private static final EntityKey ENTITY=new EntityKey("Asset",42L);
    private static final String PURPOSE="edit-asset";
    private static final String BUSINESS="ASSET-42";
    private static final String ROW="asset:42";
    private static final ViewOptions QUERY_VIEW=new ViewOptions(ViewOptions.Mode.GOVERNED,
            ViewOptions.BusinessCompletion.QUERY,null,Duration.ZERO,false);
    private static final ViewOptions COMMITTED_VIEW=new ViewOptions(ViewOptions.Mode.GOVERNED,
            ViewOptions.BusinessCompletion.COMMITTED,null,Duration.ZERO,false);

    private static final class Service implements ContextBoundDocumentService {
        final Fixture f;
        final PostgresDocumentBlobSelectionParticipant selections;
        Service(Fixture f) {
            this.f=f;
            f.sql.execute("CREATE TABLE asset_document_permissions(subject TEXT PRIMARY KEY, tenant_id BIGINT NOT NULL, can_edit BOOLEAN NOT NULL)");
            f.sql.update("INSERT INTO asset_document_permissions VALUES('alice',7,true)",new Object[0]);
            f.sql.execute("CREATE TABLE asset_document_commands(subject TEXT NOT NULL, command_id TEXT NOT NULL, digest BYTEA NOT NULL,"
                    + " previous_version BIGINT NOT NULL, new_version BIGINT NOT NULL, result_token TEXT NOT NULL,"
                    + " PRIMARY KEY(subject,command_id))");
            var authorization=new BlobBindingAuthorization(
                    context -> context.getAttribute("scope",BlobScope.class),
                    (context,target,kind) -> FIELD.equals(target) && kind==MutationOperationKind.UPDATE && allowed(context),
                    (context,target,blob) -> FIELD.equals(target) && allowed(context));
            selections=new PostgresDocumentBlobSelectionParticipant(f.sql,f.catalog,f.participant,
                    authorization,new PostgresDocumentBlobSelectionParticipant.DocumentAuthority() {
                        @Override public boolean sameActor(UserContext context,
                                TrustedReferencePrincipal principal,BlobScope scope) {
                            return SCOPE.equals(scope) && principal.subject().equals(context.getAttribute("actor"))
                                    && allowed(context);
                        }
                        @Override public boolean canCreate(UserContext context,BoundDocumentSnapshot document,
                                String clientKey,EntityKey resolvedEntity) { return false; }
                    });
        }

        boolean allowed(UserContext context) {
            var principal=context.trustedReferencePrincipal();
            if (!"Tenant".equals(principal.domainRootType()) || principal.domainRootId()!=7) return false;
            var rows=f.sql.queryForList("SELECT can_edit FROM asset_document_permissions WHERE subject=? AND tenant_id=?",
                    new Object[]{principal.subject(),principal.domainRootId()});
            return rows.size()==1 && Boolean.TRUE.equals(rows.get(0).get("can_edit"));
        }
        void authorize(UserContext context,TrustedReferencePrincipal principal) {
            if (!principal.equals(context.trustedReferencePrincipal()) || !allowed(context)) throw denied();
        }
        private static RoundTripReferenceException denied() {
            return new RoundTripReferenceException(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                    "AUTHORIZATION_REQUIRED");
        }
        private static RoundTripReferenceException invalid() {
            return new RoundTripReferenceException(RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED,
                    "DOCUMENT_VALIDATION_FAILED");
        }
        private static RoundTripReferenceException stale() {
            return new RoundTripReferenceException(RoundTripReferenceErrorCode.DOCUMENT_REVISION_CONFLICT,
                    "DOCUMENT_REVISION_CONFLICT");
        }
        private Map<String,Object> row() {
            var rows=f.sql.queryForList("SELECT id,version,document FROM asset_data WHERE id=42",new Object[0]);
            if (rows.size()!=1) throw invalid();
            return rows.get(0);
        }
        private ContextualDocument render(UserContext context,Duration lifetime) {
            var row=row();
            long id=((Number)row.get("id")).longValue(),version=((Number)row.get("version")).longValue();
            BlobRef key=row.get("document")==null?null:BlobRef.of((String)row.get("document"));
            var binding=f.catalog.binding(SCOPE,FIELD).orElseThrow(Service::invalid);
            if (!java.util.Objects.equals(key,binding.reference())) throw invalid();
            var scope=new ReferenceDocumentScope("asset-42-v"+version,PURPOSE,"Asset",id,version);
            var entity=new DocumentSnapshot.Entity(ROW,new InternalEntityIdentity("Asset",id,version),
                    Set.of("document"),Set.of("document"),Set.of(DocumentSnapshot.MutationKind.UPDATE));
            var snapshot=new DocumentSnapshot("asset-model-v1",BUSINESS,scope,entity.identity(),
                    List.of(entity),List.of());
            var bound=new BoundDocumentSnapshot(snapshot,List.of(new DocumentBlobBindingSnapshot(
                    ROW,"document","Asset",key,binding.version(),true,true)));
            return new ContextualDocument(BUSINESS,context.issueBoundDocumentSnapshot(bound,lifetime),
                    Map.of("rowKey",ROW,"hasDocument",key!=null));
        }
        @Override public ContextualDocument openDocument(UserContext context,
                TrustedReferencePrincipal principal,OpenRequest request) {
            if (!"Asset".equals(request.aggregateType()) || !PURPOSE.equals(request.purpose())
                    || !BUSINESS.equals(request.businessId())) throw invalid();
            var result=new AtomicReference<ContextualDocument>();
            f.sql.executeInTransaction(() -> {
                authorize(context,principal);
                result.set(render(context,request.lifetime()));
            });
            return result.get();
        }
        String choose(UserContext context,String documentToken,BlobRef next,String commandId) {
            authorize(context,context.trustedReferencePrincipal());
            var verified=context.consumeBoundDocumentSnapshot(documentToken,"Asset",PURPOSE);
            if (!BUSINESS.equals(verified.document().snapshot().businessId())) throw invalid();
            validatePreparedBoundDocument(context,context.trustedReferencePrincipal(),verified);
            var target=new BlobSelectionTarget(verified.document().snapshot().scope(),"Asset","document",
                    BlobSelectionTarget.Kind.EXISTING_ROW,ROW,commandId);
            if (f.catalog.find(SCOPE,next).isEmpty()) throw invalid();
            return context.issueBlobSelection(documentToken,new BlobSelectionClaim(target,next),Duration.ofMinutes(2));
        }
        @Override public AcceptedDocument acceptDocument(UserContext context,
                TrustedReferencePrincipal principal,AcceptRequest request) {
            if (!"Asset".equals(request.aggregateType()) || !PURPOSE.equals(request.purpose())) throw invalid();
            var submitted=request.submitted();
            if (!BUSINESS.equals(submitted.businessId()) || !submitted.body().keySet().equals(Set.of("rowKey","selection"))
                    || !ROW.equals(submitted.body().get("rowKey"))
                    || !(submitted.body().get("selection") instanceof String token)) throw invalid();
            var verified=context.consumeBoundDocumentSnapshot(submitted.documentToken(),"Asset",PURPOSE);
            var snapshot=verified.document().snapshot();
            if (!BUSINESS.equals(snapshot.businessId())) throw invalid();
            byte[] fingerprint=digest(submitted);
            var result=new AtomicReference<AcceptedDocument>();
            f.sql.executeInTransaction(() -> {
                authorize(context,principal);
                // Serialize replays before reading the receipt; the first commit releases this row lock.
                if (f.sql.queryForList("SELECT id FROM asset_data WHERE id=42 FOR UPDATE",new Object[0]).size()!=1)
                    throw invalid();
                var earlier=f.sql.queryForList("SELECT digest,previous_version,new_version,result_token "
                        + "FROM asset_document_commands WHERE subject=? AND command_id=?",
                        new Object[]{principal.subject(),submitted.commandId()});
                if (!earlier.isEmpty()) {
                    if (earlier.size()!=1 || !MessageDigest.isEqual(fingerprint,(byte[])earlier.get(0).get("digest")))
                        throw new RoundTripReferenceException(RoundTripReferenceErrorCode.DOCUMENT_COMMAND_CONFLICT,
                                "DOCUMENT_COMMAND_CONFLICT");
                    var receipt=earlier.get(0);
                    long nextVersion=((Number)receipt.get("new_version")).longValue();
                    result.set(new AcceptedDocument(new ContextualDocument(BUSINESS,(String)receipt.get("result_token"),
                            Map.of("rowKey",ROW,"hasDocument",true)),
                            ((Number)receipt.get("previous_version")).longValue(),
                            nextVersion,List.of(new InternalEntityIdentity("Asset",42,nextVersion))));
                    return;
                }
                validatePreparedBoundDocument(context,principal,verified);
                var row=row();
                long oldVersion=((Number)row.get("version")).longValue();
                if (snapshot.aggregate().id()!=42 || snapshot.aggregate().version()!=oldVersion) throw stale();
                var target=new BlobSelectionTarget(snapshot.scope(),"Asset","document",
                        BlobSelectionTarget.Kind.EXISTING_ROW,ROW,submitted.commandId());
                BlobRef next=context.consumeBlobSelection(submitted.documentToken(),token,target).claim().reference();
                var approved=new MutationPlan(submitted.commandId(),"asset-document-save","Asset",
                        "accept asset document file selection",List.of(new MutationOperation(
                                MutationOperationKind.UPDATE,ENTITY,oldVersion,Map.of("document",next))));
                var input=new PostgresDocumentBlobSelectionParticipant.Request(submitted.documentToken(),"Asset",PURPOSE,
                        submitted.commandId(),List.of(new PostgresDocumentBlobSelectionParticipant.Selection(token,target)),Map.of());
                BlobMutationParticipant.Plan plan=selections.plan(context,approved,input);
                plan.requireTransaction(f.sql);
                var prepared=plan.prepare(context,f.sql);
                if (f.sql.update("UPDATE asset_data SET document=?,version=? WHERE id=42 AND version=?",
                        new Object[]{next.key(),oldVersion+1,oldVersion})!=1) throw stale();
                prepared.apply(context,f.sql,List.of(new BlobMutationParticipant.PersistedEntity(
                        ENTITY,ENTITY,oldVersion+1,MutationOperationKind.UPDATE)));
                ContextualDocument document=render(context,Duration.ofMinutes(5));
                f.sql.update("INSERT INTO asset_document_commands(subject,command_id,digest,previous_version,new_version,result_token) "
                        + "VALUES(?,?,?,?,?,?)",new Object[]{principal.subject(),submitted.commandId(),fingerprint,
                                oldVersion,oldVersion+1,document.documentToken()});
                result.set(new AcceptedDocument(document,oldVersion,oldVersion+1,List.of(
                        new InternalEntityIdentity("Asset",42,oldVersion+1))));
            });
            return result.get();
        }
        @Override public boolean supportsViewPreparation() { return true; }
        @Override public void validatePreparedBoundDocument(UserContext context,
                TrustedReferencePrincipal principal,VerifiedBoundDocumentSnapshot verified) {
            authorize(context,principal);
            var snapshot=verified.document().snapshot();
            if (!"asset-model-v1".equals(snapshot.modelFingerprint()) || !BUSINESS.equals(snapshot.businessId())
                    || snapshot.aggregate().id()!=42) throw invalid();
            var row=row(); var binding=f.catalog.binding(SCOPE,FIELD).orElseThrow(Service::invalid);
            var issued=verified.document().bindings();
            if (snapshot.entities().size()!=1 || !ROW.equals(snapshot.entities().get(0).rowKey())
                    || snapshot.entities().get(0).identity().id()!=42
                    || issued.size()!=1 || !ROW.equals(issued.get(0).rowKey())
                    || !"document".equals(issued.get(0).canonicalField())
                    || !"Asset".equals(issued.get(0).entityType())
                    || !java.util.Objects.equals(
                    row.get("document")==null?null:BlobRef.of((String)row.get("document")),issued.get(0).reference())
                    || !java.util.Objects.equals(binding.reference(),issued.get(0).reference())
                    || binding.version()!=issued.get(0).bindingVersion()
                    || ((Number)row.get("version")).longValue()!=snapshot.aggregate().version()) throw stale();
        }
        void revoke() { f.sql.update("UPDATE asset_document_permissions SET can_edit=false WHERE subject='alice'",new Object[0]); }
    }

    private static byte[] digest(ContextBoundDocumentService.SubmittedDocument submitted) {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            for (String value:List.of(submitted.businessId(),submitted.documentToken(),submitted.commandId(),
                    (String)submitted.body().get("rowKey"),(String)submitted.body().get("selection"))) {
                byte[] bytes=value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array()); digest.update(bytes);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException failed) { throw new IllegalStateException(failed); }
    }

    private static TeaQLRuntime runtime(Fixture f) {
        byte[] key=new byte[32]; Arrays.fill(key,(byte)0x51);
        var provider=AeadRoundTripReferenceProvider.governed(DeploymentProfile.TEST,"asset-document-it","test",
                new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("k1",key)),
                (context,principal,scope,identity) -> {});
        ViewAuthorization authorization=new ViewAuthorization() {
            @Override public Identity identity(UserContext context) {
                var p=context.trustedReferencePrincipal();
                return new Identity(p.authenticationRealm(),p.subject(),p.domainRootType()+":"+p.domainRootId());
            }
            @Override public boolean canReadEntity(UserContext context,EntityTarget target,String path) { return true; }
            @Override public FieldDecision field(UserContext context,EntityTarget target,
                    String canonicalField,String path,ViewOptions.Mode mode) { return FieldDecision.show(); }
        };
        return TeaQLRuntime.builder().metadata(f.runtime.getMetadata()).roundTripReferenceProvider(provider)
                .viewAuthorization(authorization).telemetry(RuntimeTelemetry.NOOP).executionLogging(false).build();
    }
    private static DefaultUserContext context(TeaQLRuntime runtime,Service service) {
        var context=new DefaultUserContext(runtime);
        context.putAttribute("scope",SCOPE); context.putAttribute("actor","alice");
        context.withTrustedReferencePrincipal(new TrustedReferencePrincipal("oidc","alice","Tenant",7));
        context.withContextBoundDocumentService(service);
        return context;
    }
    private static ContextBoundDocumentService.AcceptRequest request(
            ContextBoundDocumentService.ContextualDocument opened,String selection,String command) {
        return new ContextBoundDocumentService.AcceptRequest("Asset",PURPOSE,
                new ContextBoundDocumentService.SubmittedDocument(BUSINESS,opened.documentToken(),command,
                        Map.of("rowKey",ROW,"selection",selection)));
    }
    private static String xid(Fixture f,String table,String where) {
        return (String)f.otherSql.queryForList("SELECT xmin::text AS xid FROM "+table+" WHERE "+where,new Object[0])
                .get(0).get("xid");
    }

    @Test public void openChooseAcceptViewAndReceiptCommitOnOnePostgresOwner() throws Exception {
        try (Fixture f=new Fixture();TeaQLRuntime runtime=runtime(f)) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var service=new Service(f); var context=context(runtime,service);
            var source=context.openDocumentForView(new ContextBoundDocumentService.OpenRequest(
                    "Asset",BUSINESS,PURPOSE,Duration.ofMinutes(5)));
            var oldView=runtime.prepareView(context,source,QUERY_VIEW);
            assertEquals(Map.of("rowKey",ROW,"hasDocument",true),oldView.value());
            var opened=context.openDocument(new ContextBoundDocumentService.OpenRequest(
                    "Asset",BUSINESS,PURPOSE,Duration.ofMinutes(5)));
            assertTrue(opened.documentToken().startsWith("tqd2."));
            String choice=service.choose(context,opened.documentToken(),next,"command-1");
            assertTrue(choice.startsWith("tqs1."));
            var input=request(opened,choice,"command-1");
            var accepted=context.acceptDocumentForView(input);
            var view=runtime.prepareView(context,accepted.document(),COMMITTED_VIEW);
            assertEquals(Map.of("rowKey",ROW,"hasDocument",true),view.value());
            assertSame(view,runtime.prepareView(context,view,COMMITTED_VIEW));
            f.assertOracle(42,next);
            assertEquals(1L,f.scalar("SELECT count(*) FROM asset_document_commands"));
            assertEquals(xid(f,"asset_data","id=42"),xid(f,"teaql_blob_binding","entity_type='Asset' AND entity_id=42"));
            assertEquals(xid(f,"asset_data","id=42"),xid(f,"asset_document_commands","command_id='command-1'"));
            var replay=context.acceptDocument(input);
            assertEquals(2,replay.newRevision());
            assertEquals(accepted.changedEntities(),replay.changedEntities());
            assertEquals(view.documentCredentials().get("documentToken"),replay.document().documentToken());
            assertEquals(1L,f.scalar("SELECT count(*) FROM asset_document_commands"));
            assertEquals(ViewPreparationException.Code.VIEW_PREPARATION_FAILED,
                    assertThrows(ViewPreparationException.class,
                            () -> runtime.prepareView(context,oldView,QUERY_VIEW)).code());
            service.revoke();
            assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,
                    assertThrows(ViewPreparationException.class,
                            () -> runtime.prepareView(context,view,COMMITTED_VIEW)).code());
        }
    }

    @Test public void receiptFailureRollsBackBusinessAndBindingThenRetrySucceeds() throws Exception {
        try (Fixture f=new Fixture();TeaQLRuntime runtime=runtime(f)) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var service=new Service(f); var context=context(runtime,service);
            var opened=context.openDocument(new ContextBoundDocumentService.OpenRequest(
                    "Asset",BUSINESS,PURPOSE,Duration.ofMinutes(5)));
            String choice=service.choose(context,opened.documentToken(),next,"fail-receipt");
            var input=request(opened,choice,"fail-receipt");
            f.sql.execute("ALTER TABLE asset_document_commands ADD CONSTRAINT reject_test_receipt "
                    + "CHECK (command_id <> 'fail-receipt')");
            assertThrows(RuntimeException.class,() -> context.acceptDocument(input));
            f.assertOracle(42,old);
            assertEquals(0L,f.scalar("SELECT count(*) FROM asset_document_commands"));
            f.sql.execute("ALTER TABLE asset_document_commands DROP CONSTRAINT reject_test_receipt");
            var accepted=context.acceptDocument(input);
            assertEquals(2,accepted.newRevision()); f.assertOracle(42,next);
            assertEquals(1L,f.scalar("SELECT count(*) FROM asset_document_commands"));
        }
    }

    @Test public void concurrentIdenticalAcceptsReturnOneCommittedReceipt() throws Exception {
        try (Fixture f=new Fixture();TeaQLRuntime runtime=runtime(f)) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var service=new Service(f); var alice=context(runtime,service);
            var opened=alice.openDocument(new ContextBoundDocumentService.OpenRequest(
                    "Asset",BUSINESS,PURPOSE,Duration.ofMinutes(5)));
            var request=request(opened,service.choose(alice,opened.documentToken(),next,"same-command"),
                    "same-command");
            var workers=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
            try {
                var first=workers.submit(() -> {
                    start.await(); return context(runtime,service).acceptDocument(request);
                });
                var second=workers.submit(() -> {
                    start.await(); return context(runtime,service).acceptDocument(request);
                });
                start.countDown();
                var a=first.get(10,TimeUnit.SECONDS); var b=second.get(10,TimeUnit.SECONDS);
                assertEquals(a.document().documentToken(),b.document().documentToken());
                assertEquals(a.changedEntities(),b.changedEntities());
                assertEquals(2,a.newRevision()); assertEquals(2,b.newRevision());
            } finally {
                workers.shutdownNow(); assertTrue(workers.awaitTermination(10,TimeUnit.SECONDS));
            }
            f.assertOracle(42,next);
            assertEquals(1L,f.scalar("SELECT count(*) FROM asset_document_commands"));
            assertEquals(xid(f,"asset_data","id=42"),xid(f,"teaql_blob_binding","entity_type='Asset' AND entity_id=42"));
            assertEquals(xid(f,"asset_data","id=42"),xid(f,"asset_document_commands","command_id='same-command'"));
        }
    }

    @Test public void rawKeyAndRevokedGrantNeverWriteBusinessOrBinding() throws Exception {
        try (Fixture f=new Fixture();TeaQLRuntime runtime=runtime(f)) {
            BlobRef old=f.upload(),next=f.upload(); f.create(42,old);
            var service=new Service(f); var alice=context(runtime,service);
            var opened=alice.openDocument(new ContextBoundDocumentService.OpenRequest(
                    "Asset",BUSINESS,PURPOSE,Duration.ofMinutes(5)));
            var raw=assertThrows(RoundTripReferenceException.class,
                    () -> alice.acceptDocument(request(opened,next.key(),"raw-key")));
            assertEquals(RoundTripReferenceErrorCode.INVALID_REFERENCE,raw.getCode());
            f.assertOracle(42,old);
            String choice=service.choose(alice,opened.documentToken(),next,"revoked");
            service.revoke();
            var revoked=assertThrows(RoundTripReferenceException.class,
                    () -> alice.acceptDocument(request(opened,choice,"revoked")));
            assertEquals(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,revoked.getCode());
            f.assertOracle(42,old);
            assertEquals(0L,f.scalar("SELECT count(*) FROM asset_document_commands"));
        }
    }
}
