package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.data.dynamic.*;
import io.teaql.data.dynamic.jdbc.JdbcDynamicFieldsProvider;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.TransactionCompletion.Outcome.*;
import static org.junit.Assert.*;

/** Real caller-owned PostgreSQL transactions, native graph saves and independent SQL oracles. */
public class BlobOuterTransactionIT {
    @Test public void commitWaitsForOwnerAndKeepsLaterUnsavedEditsForTheNextSave() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); Asset asset=f.newAsset(42,key);
            f.sql.executeInTransaction(() -> {
                f.save(asset); assertEquals(Long.valueOf(1),asset.getVersion()); assertTrue(asset.getEntityMutationLedger().hasUncommittedChanges());
                assertTrue(f.audits.isEmpty()); assertEquals(0,count(f,"asset_data")); asset.updateProperty("title","later edit");
            });
            f.assertOracle(42,key); assertEquals("asset",f.row(42).get("title")); assertEquals("later edit",asset.getProperty("title"));
            assertFalse(asset.getEntityMutationLedger().hasUncommittedChanges()); assertEquals(Set.of("title"),asset.getEntityMutationLedger().changedFieldNames(new EntityKey("Asset",42L)));
            assertEquals(List.of(1L),versions(f)); f.save(asset); assertEquals("later edit",f.row(42).get("title")); assertEquals(List.of(1L,2L),versions(f));
        }
    }

    @Test public void rollbackOfNewSaveRestoresLatestIntentAndRetryCreatesExactlyOnce() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); Asset asset=f.newAsset(42,key);
            assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> { f.save(asset); asset.updateProperty("title","latest"); throw new IllegalArgumentException("test rollback"); }));
            assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_binding")); assertTrue(f.audits.isEmpty());
            assertNull(asset.getVersion()); assertEquals(EntityStatus.NEW,asset.get$status()); assertFalse(asset.getEntityMutationLedger().hasUncommittedChanges());
            assertEquals(BlobState.UPLOADED_UNBOUND,f.find(key).state()); f.save(asset); f.assertOracle(42,key);
            assertEquals("latest",f.row(42).get("title")); assertEquals(List.of(1L),versions(f));
        }
    }

    @Test public void repeatedCreateReplaceAndClearUseProvisionalVersionsAndFrozenAuditVersions() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef first=f.upload(),second=f.upload(); Asset asset=f.newAsset(42,first);
            f.sql.executeInTransaction(() -> {
                f.save(asset); asset.updateProperty("document",second); f.save(asset); asset.updateProperty("document",null); f.save(asset);
                assertEquals(Long.valueOf(3),asset.getVersion()); assertTrue(f.audits.isEmpty()); assertEquals(0,count(f,"asset_data"));
            });
            f.assertOracle(42,null); assertEquals(3L,((Number)f.row(42).get("version")).longValue());
            assertEquals(List.of(1L,2L,3L),versions(f)); assertFalse(asset.getEntityMutationLedger().hasUncommittedChanges()); assertTrue(asset.getEntityMutationLedger().currentChangeSet().isEmpty());
            assertNotNull(f.find(first).unreferencedAt()); assertNotNull(f.find(second).unreferencedAt());
        }
    }

    @Test public void repeatedUpdatesRollbackToOriginalVersionAndRetainLatestClearAndTitle() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(); Asset asset=f.create(42,old);
            assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> {
                asset.updateProperty("document",next); f.save(asset); asset.updateProperty("document",null); f.save(asset);
                asset.updateProperty("title","late"); throw new IllegalArgumentException("rollback both batches");
            }));
            f.assertOracle(42,old); assertEquals(Long.valueOf(1),asset.getVersion()); assertEquals(List.of(1L),versions(f));
            assertEquals(BlobState.UPLOADED_UNBOUND,f.find(next).state()); f.save(asset); f.assertOracle(42,null);
            assertEquals("late",f.row(42).get("title")); assertEquals(List.of(1L,2L),versions(f));
        }
    }

    @Test public void caughtInnerSaveFailureCannotCommitEarlierBindingOrClearRetryIntent() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef old=f.upload(),next=f.upload(); Asset asset=f.create(42,old);
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                asset.updateProperty("document",next); f.save(asset); asset.updateProperty("title","fail");
                assertThrows(RuntimeException.class,() -> f.save(asset));
            }));
            assertEquals(ROLLED_BACK,failure.outcome()); f.assertOracle(42,old); assertEquals(Long.valueOf(1),asset.getVersion()); assertEquals(List.of(1L),versions(f));
            asset.updateProperty("title","retry"); f.save(asset); f.assertOracle(42,next); assertEquals(List.of(1L,2L),versions(f));
        }
    }

    @Test public void failedAuditAfterCommitDoesNotRestoreGraphOrReplayAnyCommittedBatch() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef first=f.upload(),next=f.upload(); Asset asset=f.newAsset(42,first);
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.save(asset); asset.updateProperty("document",next); f.save(asset); f.failure="audit";
            }));
            assertEquals(COMMITTED,failure.outcome()); f.assertOracle(42,next); assertEquals(Long.valueOf(2),asset.getVersion());
            assertEquals(List.of(1L,2L),versions(f)); assertFalse(asset.getEntityMutationLedger().hasUncommittedChanges()); assertTrue(asset.getEntityMutationLedger().currentChangeSet().isEmpty());
            f.failure=""; f.save(asset); assertEquals(Long.valueOf(2),asset.getVersion()); assertEquals(List.of(1L,2L),versions(f));
        }
    }

    @Test public void deleteThenRecoverRollbackRebasesOntoOriginalLiveRow() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); Asset asset=f.create(42,key);
            assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> {
                asset.markForDeletion(); f.save(asset); assertEquals(Long.valueOf(-2),asset.getVersion());
                asset.markAsRecover(); f.save(asset); assertEquals(Long.valueOf(3),asset.getVersion()); throw new IllegalArgumentException("rollback lifecycle");
            }));
            f.assertOracle(42,key); assertEquals(Long.valueOf(1),asset.getVersion()); assertEquals(EntityStatus.PERSISTED,asset.get$status());
            f.save(asset); assertEquals(List.of(1L),versions(f)); assertEquals(Long.valueOf(1),asset.getVersion());
        }
    }

    @Test public void realCommitFailurePoisonsJournalAndBlocksBlindRetryUntilReloadAndReconciliation() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); Asset asset=f.newAsset(42,key);
            f.sql.execute("CREATE TABLE outer_parent(id BIGINT PRIMARY KEY)");
            f.sql.execute("CREATE TABLE outer_child(id BIGINT PRIMARY KEY,parent_id BIGINT REFERENCES outer_parent(id) DEFERRABLE INITIALLY DEFERRED)");
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> { f.save(asset); f.sql.update("INSERT INTO outer_child VALUES(1,999)",new Object[0]); }));
            assertEquals(UNKNOWN,failure.outcome()); assertTrue(asset.getEntityMutationLedger().hasUnknownTransactionOutcome()); assertTrue(f.audits.isEmpty());
            assertEquals(0,count(f,"asset_data")); assertEquals(0,count(f,"teaql_blob_binding"));
            assertEquals(UNKNOWN,assertThrows(TransactionCompletionException.class,() -> f.save(asset)).outcome()); assertEquals(0,count(f,"asset_data"));
            // Explicit test reconciliation established absence through an independent connection.
            Asset reloaded=f.newAsset(42,key); f.save(reloaded); f.assertOracle(42,key); assertEquals(List.of(1L),versions(f));
        }
    }

    @Test public void rolledBackCreateThenDeleteCancelsCreationAndLaterEditCanCreateAgain() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); Asset asset=f.newAsset(42,key);
            assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> { f.save(asset); asset.markForDeletion(); f.save(asset); throw new IllegalArgumentException("cancel creation"); }));
            assertEquals(0,count(f,"asset_data")); assertTrue(f.audits.isEmpty()); assertTrue(asset.getEntityMutationLedger().currentChangeSet().isEmpty());
            f.save(asset); assertEquals(0,count(f,"asset_data")); asset.updateProperty("title","create again"); f.save(asset);
            f.assertOracle(42,key); assertEquals("create again",f.row(42).get("title")); assertEquals(List.of(1L),versions(f));
        }
    }

    @Test public void committedAuditCanStartNewSaveWithoutLaterOldCallbackRestoringAnOlderVersion() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); Asset asset=f.newAsset(42,key); AtomicBoolean once=new AtomicBoolean();
            f.auditHook=event -> { if (once.compareAndSet(false,true)) { assertFalse(asset.getEntityMutationLedger().hasUncommittedChanges()); asset.updateProperty("title","audit followup"); f.save(asset); } };
            f.sql.executeInTransaction(() -> { f.save(asset); asset.updateProperty("title","second batch"); f.save(asset); });
            assertEquals(Long.valueOf(3),asset.getVersion()); assertEquals("audit followup",f.row(42).get("title"));
            assertEquals(List.of(1L,3L,2L),versions(f)); f.assertOracle(42,key);
        }
    }

    @Test public void laterContextChangesCannotRewriteTheActorOfEarlierSubmittedBatches() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); Asset asset=f.newAsset(42,key); var context=f.context();
            String actor=GeneratedSchemaBootstrap.AUDIT_ACTOR_ATTRIBUTE;
            f.sql.executeInTransaction(() -> {
                context.putAttribute(actor,"context-alpha"); f.runtime.saveGraph(context,asset);
                context.putAttribute(actor,"context-beta"); asset.updateProperty("title","changed-title-value"); f.runtime.saveGraph(context,asset);
                context.putAttribute(actor,"later-context"); assertTrue(f.audits.isEmpty());
            });
            assertEquals(List.of("context-alpha","context-beta"),f.audits.stream().map(event -> event.actor()).toList());
        }
    }

    @Test public void blobAndDynamicBatchesCommitTogetherAndPreserveLaterDynamicEdit() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(); Asset asset=f.newAsset(42,key); var context=dynamicContext(f,asset);
            f.sql.executeInTransaction(() -> {
                asset.updateDynamicField("note","first"); f.runtime.saveGraph(context,asset);
                asset.updateDynamicField("note","second"); f.runtime.saveGraph(context,asset);
                asset.updateDynamicField("note","later"); assertTrue(f.audits.isEmpty()); assertEquals(0,count(f,"teaql_dynamic_field_value"));
            });
            f.assertOracle(42,key); assertEquals("second",dynamicValue(f)); assertEquals("later",asset.dynamicFields().getString("note"));
            assertEquals(Set.of("#note"),asset.getEntityMutationLedger().changedFieldNames(new EntityKey("Asset",42L)));
            f.runtime.saveGraph(context,asset); assertEquals("later",dynamicValue(f)); assertEquals(List.of(1L,2L,3L),versions(f));
        }
    }

    @Test public void rollbackRestoresDynamicIntentAndOriginalBlobAndBusinessVersion() throws Exception {
        try (Fixture f=new Fixture()) {
            BlobRef key=f.upload(),next=f.upload(); Asset asset=f.create(42,key); var context=dynamicContext(f,asset);
            assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> {
                asset.updateProperty("document",next); asset.updateDynamicField("note","first"); f.runtime.saveGraph(context,asset);
                asset.updateDynamicField("note","latest"); f.runtime.saveGraph(context,asset); throw new IllegalArgumentException("rollback dynamic graph");
            }));
            f.assertOracle(42,key); assertEquals(0,count(f,"teaql_dynamic_field_value")); assertEquals(Long.valueOf(1),asset.getVersion());
            assertEquals("latest",asset.dynamicFields().getString("note")); f.runtime.saveGraph(context,asset);
            f.assertOracle(42,next); assertEquals("latest",dynamicValue(f)); assertEquals(List.of(1L,2L),versions(f));
        }
    }

    private static io.teaql.runtime.DefaultUserContext dynamicContext(Fixture f,Asset asset) {
        var provider=new JdbcDynamicFieldsProvider(f.sql); provider.ensureSchema();
        java.util.concurrent.atomic.AtomicLong ids=new java.util.concurrent.atomic.AtomicLong(5000);
        DynamicFieldContext definitions=new DynamicFieldContext() {
            @Override public String scopeType() { return "GLOBAL"; }
            @Override public String scopeId() { return "default"; }
            @Override public String userId() { return "transaction-test"; }
            @Override public String purpose() { return "test definitions"; }
            @Override public String comment() { return "test definitions"; }
            @Override public boolean strictIntent() { return false; }
            @Override public long nextId(String type) { return ids.incrementAndGet(); }
        };
        var definition=new DynamicFieldDef(); definition.setOwnerType("Asset"); definition.setScope(DynamicFieldScope.global());
        definition.setCode("note"); definition.setName("Note"); definition.setDataType(DynamicDataType.STRING); definition.setStatus(DynamicFieldStatus.ACTIVE);
        provider.registerFieldDef(definitions,definition);
        var facade=new DefaultDynamicFieldsFacade(provider).withIdGenerator(type -> ids.incrementAndGet());
        var context=f.context(); context.putAttribute(DynamicFieldsFacade.class.getName(),facade);
        var metadata=context.dynamicFields().metadata("Asset");
        asset.__internalHydrateDynamicFields(new DynamicFieldValues(metadata,Map.of()),asset.__internalLoadState().withDynamicSelection(Set.of()));
        return context;
    }
    private static String dynamicValue(Fixture f) { return (String)f.otherSql.queryForList("SELECT string_value FROM teaql_dynamic_field_value WHERE owner_type='Asset' AND owner_id=42",new Object[0]).get(0).get("string_value"); }

    private static int count(Fixture f,String table) { return ((Number)f.otherSql.queryForList("SELECT count(*) FROM "+table,new Object[0]).get(0).values().iterator().next()).intValue(); }
    private static List<Long> versions(Fixture f) { return f.audits.stream().map(event -> event.resultingVersion()).toList(); }
}
