package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

/** Save-boundary tests. Real database locking and catalog semantics have separate gates. */
public class BlobMutationIntegrationTest {
    private static final BlobRef OLD = BlobRef.of("blob_old");
    private static final BlobRef NEXT = BlobRef.of("blob_next");

    @Test public void approvalPrecedesPlanningAndAllEffectsPrecedeCommitAndAudit() {
        Fixture f = new Fixture();
        try (TeaQLRuntime runtime = f.runtime(true)) {
            DefaultUserContext context = new DefaultUserContext(runtime);
            Asset entity = asset();
            entity.updateProperty("document", NEXT);
            runtime.saveGraph(context, entity);
            Assert.assertEquals(List.of("approve", "plan", "compatible", "begin", "prepare", "business", "apply", "commit", "audit"), f.events);
            Assert.assertEquals(NEXT, f.executor.business);
            Assert.assertEquals(NEXT, f.executor.binding);
            Assert.assertSame(context, f.planContext);
            Assert.assertSame(context, f.prepareContext);
            Assert.assertSame(context, f.applyContext);
            Assert.assertEquals(1, f.approved.operations().size());
            Assert.assertEquals(NEXT, f.approved.operations().get(0).changedValues().get("document"));
            Assert.assertEquals(new EntityKey("Asset", 42L), f.persisted.get(0).plannedEntity());
            Assert.assertEquals(new EntityKey("Asset", 42L), f.persisted.get(0).persistedEntity());
            Assert.assertEquals(Long.valueOf(8), f.persisted.get(0).version());
            Assert.assertThrows(UnsupportedOperationException.class, () -> f.persisted.clear());
            Assert.assertTrue(entity.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
            Assert.assertEquals(Long.valueOf(8), entity.getVersion());
        }
    }

    @Test public void rejectionNeverPlansOrOpensATransaction() {
        Fixture f = new Fixture(); f.deny = true;
        try (TeaQLRuntime runtime = f.runtime(true)) {
            Asset entity = asset(); entity.updateProperty("document", NEXT);
            Assert.assertThrows(TeaQLRuntimeException.class, () -> runtime.saveGraph(new DefaultUserContext(runtime), entity));
            Assert.assertEquals(List.of("approve"), f.events);
            assertPending(entity);
            Assert.assertEquals(OLD, f.executor.business);
        }
    }

    @Test public void missingParticipantAndIncompatibleTransactionFailBeforeBusinessWrites() {
        for (String failure : List.of("missing", "incompatible", "no-resource", "no-transaction", "no-completion")) {
            Fixture f = new Fixture(); f.failure = failure;
            try (TeaQLRuntime runtime = f.runtime(!failure.equals("missing"))) {
                Asset entity = asset(); entity.updateProperty("document", NEXT);
                BlobException error = Assert.assertThrows(BlobException.class,
                        () -> runtime.saveGraph(new DefaultUserContext(runtime), entity));
                Assert.assertEquals(failure.equals("missing") ? BlobException.Code.BLOB_CAPABILITY_MISSING
                        : BlobException.Code.BLOB_TRANSACTION_REQUIRED, error.code());
                Assert.assertFalse(f.events.contains("begin"));
                Assert.assertFalse(f.events.contains("business"));
                Assert.assertEquals(OLD, f.executor.binding);
                assertPending(entity);
            }
        }
    }

    @Test public void prepareBusinessApplyAndCommitFailuresRollbackAndKeepTheIntentForRetry() {
        for (String failure : List.of("prepare", "business", "apply", "commit")) {
            Fixture f = new Fixture(); f.failure = failure;
            try (TeaQLRuntime runtime = f.runtime(true)) {
                Asset entity = asset(); entity.updateProperty("document", NEXT);
                Assert.assertThrows(IllegalStateException.class,
                        () -> runtime.saveGraph(new DefaultUserContext(runtime), entity));
                Assert.assertEquals(failure, OLD, f.executor.business);
                Assert.assertEquals(failure, OLD, f.executor.binding);
                Assert.assertFalse(f.executor.active);
                Assert.assertFalse(f.events.contains("audit"));
                Assert.assertEquals("rollback", f.events.get(f.events.size() - 1));
                Assert.assertEquals(Long.valueOf(7), entity.getVersion());
                assertPending(entity);
                f.failure = ""; f.events.clear();
                runtime.saveGraph(new DefaultUserContext(runtime), entity);
                Assert.assertEquals(NEXT, f.executor.business);
                Assert.assertEquals(NEXT, f.executor.binding);
                Assert.assertEquals(1, Collections.frequency(f.events, "business"));
                Assert.assertEquals(1, Collections.frequency(f.events, "commit"));
            }
        }
    }

    @Test public void unrelatedPartialUpdatesLeaveUnloadedAndLoadedUnchangedBlobsAlone() {
        for (boolean loaded : List.of(false, true)) {
            Fixture f = new Fixture();
            try (TeaQLRuntime runtime = f.runtime(false)) {
                Asset entity = asset();
                entity.__internalUseLoadState(entity.__internalLoadState().withLoaded("document", loaded));
                entity.updateProperty("title", "changed title");
                runtime.saveGraph(new DefaultUserContext(runtime), entity);
                Assert.assertFalse(f.events.contains("plan"));
                Assert.assertEquals(OLD, f.executor.binding);
                Assert.assertEquals(OLD, f.executor.business);
            }
        }
    }

    @Test public void explicitNullDeleteAndRecoveryReachParticipantWithoutClientOldKey() {
        for (MutationOperationKind kind : List.of(MutationOperationKind.UPDATE, MutationOperationKind.DELETE, MutationOperationKind.RECOVER)) {
            Fixture f = new Fixture(); f.inherited = true;
            try (TeaQLRuntime runtime = f.runtime(true)) {
                Asset entity = asset();
                if (kind == MutationOperationKind.UPDATE) entity.updateProperty("document", null);
                else {
                    entity.document = null; entity.__internalUseLoadState(entity.__internalLoadState().withLoaded("document", false));
                    if (kind == MutationOperationKind.DELETE) entity.markForDeletion();
                    else {
                        entity.__internalSet("version", -7L); entity.set$status(EntityStatus.PERSISTED_DELETED);
                        entity.markAsRecover();
                    }
                }
                runtime.saveGraph(new DefaultUserContext(runtime), entity);
                Assert.assertEquals(kind, f.approved.operations().get(0).kind());
                Assert.assertEquals(kind, f.persisted.get(0).kind());
                Assert.assertTrue(f.events.contains("prepare"));
                Assert.assertTrue(f.events.contains("apply"));
                if (kind == MutationOperationKind.UPDATE) {
                    Assert.assertTrue(f.approved.operations().get(0).changedValues().containsKey("document"));
                    Assert.assertNull(f.executor.binding);
                } else Assert.assertFalse(f.approved.operations().get(0).changedValues().containsKey("document"));
            }
        }
    }

    @Test public void finalPersistedIdAndRevisionAreResolvedBeforeBindingApply() {
        Fixture f = new Fixture(); f.executor.persistedId = 4200L;
        try (TeaQLRuntime runtime = f.runtime(true)) {
            Asset entity = asset(); entity.updateProperty("document", NEXT);
            runtime.saveGraph(new DefaultUserContext(runtime), entity);
            Assert.assertEquals(Long.valueOf(42), f.persisted.get(0).plannedEntity().id());
            Assert.assertEquals(Long.valueOf(4200), f.persisted.get(0).persistedEntity().id());
            Assert.assertEquals(Long.valueOf(8), f.persisted.get(0).version());
            Assert.assertEquals(Long.valueOf(4200), entity.getId());
        }
    }

    @Test public void participantInstallationIsPassiveUniqueAndDoesNotClaimFullBlobReadiness() {
        Fixture f = new Fixture(); TeaQLRuntime.Builder builder = f.builder(true);
        Assert.assertTrue(f.events.isEmpty());
        Assert.assertThrows(IllegalStateException.class, () -> builder.blobMutationParticipant(f));
        try (TeaQLRuntime runtime = builder.build()) {
            DefaultUserContext context = new DefaultUserContext(runtime);
            Assert.assertSame(f, context.capability(BlobMutationParticipant.class));
            runtime.install(RuntimeModule.of().withRequiredCapabilities("blob.v1"));
            Assert.assertThrows(TeaQLRuntimeException.class, runtime::validateCapabilities);
            Assert.assertTrue(f.events.isEmpty());
        }
    }

    @Test public void saveChecksManifestRequirementsEvenWhenTheHostSkippedStartupReadiness() {
        for (String capability : List.of("blob.v1", "secure_document.v1")) {
            Fixture f = new Fixture();
            try (TeaQLRuntime runtime = f.runtime(true)) {
                runtime.install(RuntimeModule.of().withRequiredCapabilities(capability));
                Asset entity = asset(); entity.updateProperty("title", "partial update");
                Assert.assertThrows(TeaQLRuntimeException.class, () -> runtime.saveGraph(new DefaultUserContext(runtime), entity));
                Assert.assertTrue(f.events.isEmpty());
                Assert.assertEquals(OLD, f.executor.business);
                Assert.assertEquals(OLD, f.executor.binding);
            }
        }
    }

    @Test public void invalidProviderResultsRollbackBeforeBindingApplyOrCommit() {
        for (String failure : List.of("missing-result", "missing-id", "missing-version", "zero-version")) {
            Fixture f = new Fixture(); f.failure = failure;
            try (TeaQLRuntime runtime = f.runtime(true)) {
                Asset entity = asset(); entity.updateProperty("document", NEXT);
                BlobException error = Assert.assertThrows(BlobException.class,
                        () -> runtime.saveGraph(new DefaultUserContext(runtime), entity));
                Assert.assertEquals(BlobException.Code.BLOB_TRANSACTION_REQUIRED, error.code());
                Assert.assertFalse(f.events.contains("apply"));
                Assert.assertFalse(f.events.contains("commit"));
                Assert.assertEquals(OLD, f.executor.business);
                Assert.assertEquals(OLD, f.executor.binding);
                assertPending(entity);
            }
        }
    }

    @Test public void createPlanningSeesTheFinalAllocatedIdAndStrongValue() {
        Fixture f = new Fixture();
        try (TeaQLRuntime runtime = f.builder(true).idGenerationService((context, entity) -> 601L).build()) {
            Asset entity = new Asset(); entity.updateProperty("document", NEXT); entity.setComment("create asset");
            runtime.saveGraph(new DefaultUserContext(runtime), entity);
            Assert.assertEquals(MutationOperationKind.CREATE, f.approved.operations().get(0).kind());
            Assert.assertEquals(new EntityKey("Asset", 601L), f.approved.operations().get(0).entity());
            Assert.assertEquals(new EntityKey("Asset", 601L), f.persisted.get(0).persistedEntity());
            Assert.assertEquals(NEXT, f.executor.binding);
        }
    }

    @Test public void auditFailureAfterCommitDoesNotRollbackOrAutomaticallyReplayEitherWrite() {
        Fixture f = new Fixture(); f.failure = "audit";
        try (TeaQLRuntime runtime = f.runtime(true)) {
            Asset entity = asset(); entity.updateProperty("document", NEXT);
            Assert.assertThrows(IllegalStateException.class, () -> runtime.saveGraph(new DefaultUserContext(runtime), entity));
            Assert.assertEquals(List.of("approve", "plan", "compatible", "begin", "prepare", "business", "apply", "commit", "audit"), f.events);
            Assert.assertEquals(NEXT, f.executor.business);
            Assert.assertEquals(NEXT, f.executor.binding);
            Assert.assertEquals(Long.valueOf(8), entity.getVersion());
        }
    }

    private static void assertPending(Asset entity) {
        Assert.assertTrue(entity.getEntityMutationLedger().changedFieldNames(new EntityKey("Asset", 42L)).contains("document"));
        Assert.assertEquals(NEXT, entity.document);
    }

    private static Asset asset() {
        Asset entity = new Asset();
        entity.__internalSet("id", 42L); entity.__internalSet("version", 7L);
        entity.__internalSet("document", OLD); entity.set$status(EntityStatus.PERSISTED);
        entity.setComment("replace asset file");
        return entity;
    }

    public static final class Asset extends BaseEntity {
        private BlobRef document;
        private String title;
        @Override public String typeName() { return "Asset"; }
        @Override public void __internalSet(String field, Object value) {
            switch (field) {
                case "document" -> { document = (BlobRef) value; markPropertyLoaded(field); }
                case "title" -> { title = (String) value; markPropertyLoaded(field); }
                default -> super.__internalSet(field, value);
            }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) {
                case "document" -> document;
                case "title" -> title;
                default -> super.__internalGet(field);
            };
        }
    }

    private static final class Fixture implements BlobMutationParticipant {
        final List<String> events = new ArrayList<>();
        final Executor executor = new Executor(this);
        String failure = "";
        boolean deny;
        boolean inherited;
        MutationPlan approved;
        UserContext planContext;
        UserContext prepareContext;
        UserContext applyContext;
        List<PersistedEntity> persisted;

        TeaQLRuntime runtime(boolean install) { return builder(install).build(); }
        TeaQLRuntime.Builder builder(boolean install) {
            SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
            EntityDescriptor descriptor = new EntityDescriptor(); descriptor.setType("Asset");
            descriptor.setEntitySupplier(Asset::new);
            descriptor.addSimpleProperty("id", Long.class);
            descriptor.addSimpleProperty("version", Long.class);
            descriptor.addSimpleProperty("title", String.class);
            EntityDescriptor blobOwner = descriptor;
            if (inherited) {
                blobOwner = new EntityDescriptor(); blobOwner.setType("AssetBase"); descriptor.setParent(blobOwner);
            }
            blobOwner.addSimpleProperty("document", BlobRef.class)
                    .withBlobFieldSpec(BlobFieldSpec.of(blobOwner.getType(), "document", List.of("pdf"), true, "default"));
            metadata.register(descriptor);
            MutationExecutor activeExecutor = executor;
            if (failure.equals("no-transaction")) activeExecutor = new MutationExecutor() {
                @Override public MutationResult mutate(UserContext context, PersistenceMutation mutation) { return executor.mutate(context, mutation); }
                @Override public String name() { return "nontransactional"; }
                @Override public DataServiceCapabilities capabilities() { return null; }
            };
            TeaQLRuntime.Builder builder = TeaQLRuntime.builder().metadata(metadata).dataService("default", activeExecutor)
                    .telemetry(RuntimeTelemetry.NOOP).executionLogging(false)
                    .mutationPolicyRegistry(key -> Optional.of(new MutationPolicy() {
                        @Override public MutationPolicyIdentity identity() { return new MutationPolicyIdentity("asset", "1", "sha256:test"); }
                        @Override public MutationDecision review(UserContext context, MutationPlan plan) {
                            events.add("approve");
                            return deny ? MutationDecision.deny("DENIED", "denied", List.of("document")) : MutationDecision.allow();
                        }
                    })).logSink(new RuntimeLogSink() {
                        @Override public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) {}
                        @Override public void writeAuditEvent(UserContext context, RawAuditEvent event) {
                            Assert.assertFalse(executor.active); events.add("audit");
                            fail("audit");
                        }
                    });
            if (install) builder.blobMutationParticipant(this);
            return builder;
        }

        @Override public Plan plan(UserContext context, MutationPlan plan) {
            Assert.assertFalse(executor.active); events.add("plan"); approved = plan; planContext = context;
            return new Plan() {
                @Override public void requireTransaction(Object resource) {
                    Assert.assertSame(executor, resource); Assert.assertFalse(executor.active); events.add("compatible");
                    if (failure.equals("incompatible")) throw new BlobException(BlobException.Code.BLOB_TRANSACTION_REQUIRED);
                }
                @Override public Prepared prepare(UserContext current, Object resource) {
                    Assert.assertSame(executor, resource); Assert.assertTrue(executor.active); events.add("prepare"); prepareContext = current;
                    fail("prepare");
                    return (actor, transaction, entities) -> {
                        Assert.assertSame(executor, transaction); Assert.assertTrue(executor.active); events.add("apply");
                        applyContext = actor; persisted = entities;
                        MutationOperation operation = plan.operations().get(0);
                        if (operation.kind() == MutationOperationKind.DELETE) executor.binding = null;
                        else if (operation.changedValues().containsKey("document")) executor.binding = (BlobRef) operation.changedValues().get("document");
                        fail("apply");
                    };
                }
            };
        }

        void fail(String stage) { if (failure.equals(stage)) throw new IllegalStateException(stage); }
    }

    private static final class Executor implements TransactionExecutor, MutationExecutor {
        final Fixture fixture;
        BlobRef business = OLD;
        BlobRef binding = OLD;
        boolean active;
        Object physicalOwner;
        final List<TransactionCompletion> completions=new ArrayList<>();
        Long persistedId;
        Executor(Fixture fixture) { this.fixture = fixture; }
        @Override public Object transactionResource() { return fixture.failure.equals("no-resource") ? null : this; }
        @Override public Object activeTransactionIdentity() { return active?physicalOwner:null; }
        @Override public boolean supportsTransactionCompletion() { return !fixture.failure.equals("no-completion"); }
        @Override public void afterTransaction(TransactionCompletion completion) { Assert.assertTrue(active); completions.add(completion); }
        @Override public <T> T executeInTransaction(UserContext context, TransactionCallback<T> action) {
            Assert.assertFalse(active); BlobRef oldBusiness = business; BlobRef oldBinding = binding;
            fixture.events.add("begin"); active = true; physicalOwner=new Object(); completions.clear();
            T result;
            try {
                result = action.doInTransaction(); fixture.fail("commit"); fixture.events.add("commit");
            } catch (RuntimeException | Error failure) {
                business = oldBusiness; binding = oldBinding; fixture.events.add("rollback"); active=false;
                for (int i=completions.size()-1;i>=0;i--) completions.get(i).beforeNotifications(TransactionCompletion.Outcome.ROLLED_BACK);
                for (int i=completions.size()-1;i>=0;i--) completions.get(i).afterCompletion(TransactionCompletion.Outcome.ROLLED_BACK);
                throw failure;
            } finally { active = false; }
            for (TransactionCompletion completion:completions) completion.beforeNotifications(TransactionCompletion.Outcome.COMMITTED);
            for (TransactionCompletion completion:completions) completion.afterCompletion(TransactionCompletion.Outcome.COMMITTED);
            return result;
        }
        @Override public MutationResult mutate(UserContext context, PersistenceMutation mutation) {
            Assert.assertTrue(active); fixture.events.add("business");
            EntityPersistenceMutation request = (EntityPersistenceMutation) mutation;
            Asset changes = (Asset) request.getEntity();
            if (changes.isPropertyLoaded("document")) business = changes.document;
            fixture.fail("business");
            Asset persisted = new Asset();
            persisted.__internalSet("id", persistedId == null ? changes.getId() : persistedId);
            persisted.__internalSet("version", request.getAction() == EntityPersistenceMutation.Action.DELETE ? -8L : 8L);
            if (changes.isPropertyLoaded("document")) persisted.__internalSet("document", changes.document);
            if (changes.isPropertyLoaded("title")) persisted.__internalSet("title", changes.title);
            if (fixture.failure.equals("missing-result")) return new DefaultMutationResult(null);
            if (fixture.failure.equals("missing-id")) persisted.__internalSet("id", null);
            if (fixture.failure.equals("missing-version")) persisted.__internalSet("version", null);
            if (fixture.failure.equals("zero-version")) persisted.__internalSet("version", 0L);
            persisted.set$status(request.getAction() == EntityPersistenceMutation.Action.DELETE ? EntityStatus.PERSISTED_DELETED : EntityStatus.PERSISTED);
            return new DefaultMutationResult(persisted);
        }
        @Override public String name() { return "transaction-test"; }
        @Override public DataServiceCapabilities capabilities() { return null; }
    }
}
