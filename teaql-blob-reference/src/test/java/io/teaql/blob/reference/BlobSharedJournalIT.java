package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.TransactionCompletion.Outcome.*;
import static org.junit.Assert.*;

/** Native graph planning plus actual PostgreSQL row/binding and completion oracles. */
public class BlobSharedJournalIT {
    @Test public void sharedChildMovesBetweenPendingRootsWithoutSavingUnapprovedSourceFields() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(),old=f.upload(),next=f.upload();
            Asset first=f.newAsset(42,a),second=f.newAsset(43,b),child=f.newAsset(44,old); first.children=List.of(child);
            f.sql.executeInTransaction(() -> {
                f.save(first); first.updateProperty("title","unapproved source title");
                child.updateProperty("document",next); child.updateProperty("title","second child"); second.children=List.of(child); f.save(second);
                assertSame(second.getEntityMutationLedger(),child.getEntityMutationLedger());
                assertFalse(f.approvedPlans.get(f.approvedPlans.size()-1).operations().stream().anyMatch(operation -> operation.entity().id().equals(42L)));
                assertEquals(Set.of("title"),first.getEntityMutationLedger().changedFieldNames(new EntityKey("Asset",42L)));
                assertTrue(first.getEntityMutationLedger().changedFieldNames(new EntityKey("Asset",44L)).isEmpty()); assertTrue(f.audits.isEmpty());
            });
            f.assertOracle(42,a); f.assertOracle(43,b); f.assertOracle(44,next);
            assertEquals("asset",f.row(42).get("title")); assertEquals("second child",f.row(44).get("title"));
            assertEquals(Long.valueOf(2),child.getVersion()); assertFalse(first.getEntityMutationLedger().hasUncommittedChanges());
            f.save(first); assertEquals("unapproved source title",f.row(42).get("title")); assertEquals(2L,((Number)f.row(44).get("version")).longValue());
        }
    }

    @Test public void rollbackRoutesBothHistoricalBatchesToTheCurrentChildOwnerAndRetriesOnce() throws Exception {
        try (Fixture f=fixture()) {
            BlobRef a=f.upload(),b=f.upload(),old=f.upload(),middle=f.upload(),latest=f.upload();
            Asset first=f.create(42,a),second=f.create(43,b),child=f.create(44,old); first.children=List.of(child);
            int audits=f.audits.size();
            assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> {
                first.updateProperty("title","first submitted"); child.updateProperty("document",middle); f.save(first);
                first.updateProperty("title","unapproved source"); child.updateProperty("document",latest); second.updateProperty("title","second submitted"); second.children=List.of(child); f.save(second);
                child.updateProperty("title","latest child edit"); second.updateProperty("title","latest second edit"); throw new IllegalArgumentException("rollback shared graphs");
            }));
            f.assertOracle(42,a); f.assertOracle(43,b); f.assertOracle(44,old); assertEquals(audits,f.audits.size());
            assertEquals(Long.valueOf(1),first.getVersion()); assertEquals(Long.valueOf(1),second.getVersion()); assertEquals(Long.valueOf(1),child.getVersion());
            assertEquals(Long.valueOf(1),second.getEntityMutationLedger().getOriginalVersion(new EntityKey("Asset",44L)));
            f.save(second); f.assertOracle(44,latest); assertEquals("latest child edit",f.row(44).get("title")); assertEquals("latest second edit",f.row(43).get("title"));
            f.save(first); assertEquals("unapproved source",f.row(42).get("title")); assertEquals(2L,((Number)f.row(44).get("version")).longValue());
            assertEquals(BlobState.UPLOADED_UNBOUND,f.find(middle).state());
        }
    }

    @Test public void twoPreviouslyPendingCohortsJoinAndAllGraphsSettleBeforeAnyAuditReentry() throws Exception {
        try (Fixture f=fixture()) {
            Asset first=f.newAsset(42,f.upload()),second=f.newAsset(43,f.upload()),child=f.newAsset(44,f.upload()),other=f.newAsset(45,f.upload()),independent=f.newAsset(46,f.upload());
            BlobRef next=f.upload(); first.children=List.of(child); second.children=List.of(other); AtomicBoolean once=new AtomicBoolean();
            f.auditHook=event -> {
                for (Asset asset:List.of(first,second,child,other,independent)) assertFalse(asset.getEntityMutationLedger().hasUncommittedChanges());
                if (once.compareAndSet(false,true)) { independent.updateProperty("title","audit followup"); f.save(independent); }
            };
            f.sql.executeInTransaction(() -> {
                f.save(first); f.save(second); child.updateProperty("document",next); second.children=List.of(other,child); f.save(second); f.save(independent);
                assertTrue(f.audits.isEmpty());
            });
            f.assertOracle(44,next); assertEquals(Long.valueOf(2),child.getVersion()); assertEquals(Long.valueOf(2),independent.getVersion());
            assertEquals("audit followup",f.row(46).get("title")); assertEquals(2L,((Number)f.row(46).get("version")).longValue());
        }
    }

    @Test public void joinedRollbackKeepsOriginalOrderEvenWhenBothRootsAlreadySubmittedSeparateBatches() throws Exception {
        try (Fixture f=fixture()) {
            Asset first=f.newAsset(42,f.upload()),second=f.newAsset(43,f.upload()),child=f.newAsset(44,f.upload()),other=f.newAsset(45,f.upload());
            BlobRef latest=f.upload(); first.children=List.of(child); second.children=List.of(other);
            assertThrows(IllegalArgumentException.class,() -> f.sql.executeInTransaction(() -> {
                f.save(first); f.save(second); child.updateProperty("document",latest); second.children=List.of(other,child); f.save(second); throw new IllegalArgumentException("rollback joined cohorts");
            }));
            assertEquals(0,count(f)); assertTrue(f.audits.isEmpty());
            for (Asset asset:List.of(first,second,child,other)) { assertNull(asset.getVersion()); assertEquals(EntityStatus.NEW,asset.get$status()); assertFalse(asset.getEntityMutationLedger().hasUncommittedChanges()); }
            f.save(second); f.save(first); assertEquals(4,count(f)); f.assertOracle(44,latest); assertEquals(1L,((Number)f.row(44).get("version")).longValue());
        }
    }

    @Test public void partialFailureInNewRootRollsBackBothGraphsAndRetainsRetryIntent() throws Exception {
        try (Fixture f=fixture()) {
            Asset first=f.newAsset(42,f.upload()),second=f.newAsset(43,f.upload()),child=f.newAsset(44,f.upload()); BlobRef latest=f.upload(); first.children=List.of(child);
            assertThrows(IllegalStateException.class,() -> f.sql.executeInTransaction(() -> {
                f.save(first); child.updateProperty("document",latest); second.children=List.of(child); f.failure="before-apply"; f.save(second);
            }));
            assertEquals(0,count(f)); assertTrue(f.audits.isEmpty()); assertNull(child.getVersion());
            f.failure=""; f.save(second); f.save(first); assertEquals(3,count(f)); f.assertOracle(44,latest); assertEquals(1L,((Number)f.row(44).get("version")).longValue());
        }
    }

    @Test public void unknownCommitMarksEveryJoinedGraphAndRejectsDiscardOrBlindRetry() throws Exception {
        try (Fixture f=fixture()) {
            Asset first=f.newAsset(42,f.upload()),second=f.newAsset(43,f.upload()),child=f.newAsset(44,f.upload()); BlobRef latest=f.upload(); first.children=List.of(child);
            f.sql.execute("CREATE TABLE shared_parent(id BIGINT PRIMARY KEY)");
            f.sql.execute("CREATE TABLE shared_child(id BIGINT PRIMARY KEY,parent_id BIGINT REFERENCES shared_parent(id) DEFERRABLE INITIALLY DEFERRED)");
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.save(first); child.updateProperty("document",latest); second.children=List.of(child); f.save(second); f.sql.update("INSERT INTO shared_child VALUES(1,999)",new Object[0]);
            }));
            assertEquals(UNKNOWN,failure.outcome()); assertEquals(0,count(f)); assertTrue(f.audits.isEmpty());
            for (Asset asset:List.of(first,second,child)) {
                assertTrue(asset.getEntityMutationLedger().hasUnknownTransactionOutcome());
                assertEquals(UNKNOWN,assertThrows(TransactionCompletionException.class,() -> f.save(asset)).outcome());
                assertThrows(TeaQLRuntimeException.class,() -> asset.setEntityMutationLedger(new EntityMutationLedger()));
            }
        }
    }

    @Test public void anotherPhysicalOwnerCannotBorrowSharedPendingChildAndLeavesFirstTransactionUsable() throws Exception {
        try (Fixture f=fixture()) {
            Asset first=f.newAsset(42,f.upload()),second=f.newAsset(43,f.upload()),child=f.newAsset(44,f.upload()); BlobRef latest=f.upload(); first.children=List.of(child);
            ExecutorService worker=Executors.newSingleThreadExecutor();
            try {
                f.sql.executeInTransaction(() -> {
                    f.save(first); child.updateProperty("document",latest); second.children=List.of(child);
                    Future<?> attempt=worker.submit(() -> assertThrows(TeaQLRuntimeException.class,() -> f.sql.executeInTransaction(() -> f.save(second))));
                    try { attempt.get(10,TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                    catch (ExecutionException | TimeoutException failure) { throw new IllegalStateException(failure); }
                    assertSame(first.getEntityMutationLedger(),child.getEntityMutationLedger()); f.save(first);
                });
                assertEquals(2,count(f)); f.assertOracle(44,latest); assertEquals(0,f.otherSql.queryForList("SELECT id FROM asset_data WHERE id=43",new Object[0]).size());
            } finally { worker.shutdownNow(); assertTrue(worker.awaitTermination(10,TimeUnit.SECONDS)); }
        }
    }

    @Test public void currentAuthorizationIsRecheckedWhenSharedChildEntersTheSecondGraph() throws Exception {
        for (String denial:List.of("field","policy")) try (Fixture f=fixture()) {
            Asset first=f.newAsset(42,f.upload()),second=f.newAsset(43,f.upload()),child=f.newAsset(44,f.upload()); BlobRef latest=f.upload(); first.children=List.of(child);
            var failure=assertThrows(TransactionCompletionException.class,() -> f.sql.executeInTransaction(() -> {
                f.save(first); child.updateProperty("document",latest); second.children=List.of(child); var denied=f.context(); denied.putAttribute(denial,Boolean.FALSE);
                assertEquals(BlobException.Code.BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> f.runtime.saveGraph(denied,second)).code());
            }));
            assertEquals(ROLLED_BACK,failure.outcome()); assertEquals(0,count(f)); assertTrue(f.audits.isEmpty());
            f.save(second); f.save(first); f.assertOracle(44,latest); assertEquals(3,count(f));
        }
    }

    @Test public void firstAttemptWithLostRollbackAcknowledgementKeepsUnknownJournalBeforeStagingSucceeded() throws Exception {
        try (Fixture f=fixture()) {
            Asset first=f.newAsset(42,f.upload()),child=f.newAsset(44,f.upload()); first.children=List.of(child); f.failure="terminate-before-apply";
            var failure=assertThrows(TransactionCompletionException.class,() -> f.save(first));
            assertEquals(UNKNOWN,failure.outcome()); assertEquals(0,count(f)); assertTrue(f.audits.isEmpty());
            assertTrue(first.getEntityMutationLedger().hasUnknownTransactionOutcome()); assertTrue(child.getEntityMutationLedger().hasUncommittedChanges());
            f.failure=""; assertEquals(UNKNOWN,assertThrows(TransactionCompletionException.class,() -> f.save(first)).outcome()); assertEquals(0,count(f));
        }
    }

    @Test public void childCanMoveBackToEarlierRootWithCorrectCommitAndRollbackVersions() throws Exception {
        for (boolean rollback:List.of(false,true)) try (Fixture f=fixture()) {
            Asset first=f.create(42,f.upload()),second=f.create(43,f.upload()),child=f.create(44,f.upload());
            BlobRef old=child.document,middle=f.upload(),latest=f.upload(); first.children=List.of(child);
            org.junit.function.ThrowingRunnable transaction=() -> f.sql.executeInTransaction(() -> {
                child.updateProperty("document",middle); f.save(first);
                child.updateProperty("document",latest); second.children=List.of(child); f.save(second);
                child.updateProperty("title","latest child"); f.save(first); assertEquals(Long.valueOf(4),child.getVersion());
                assertSame(first.getEntityMutationLedger(),child.getEntityMutationLedger());
                if (rollback) throw new IllegalArgumentException("rollback roundtrip");
            });
            if (rollback) {
                assertThrows(IllegalArgumentException.class,transaction); f.assertOracle(44,old); assertEquals(Long.valueOf(1),child.getVersion());
                assertEquals(3,f.audits.size()); f.save(first); f.assertOracle(44,latest); assertEquals(Long.valueOf(2),child.getVersion());
            } else {
                try { transaction.run(); } catch (Throwable failure) { throw new AssertionError(failure); }
                f.assertOracle(44,latest); assertEquals(6,f.audits.size());
            }
            f.save(second); assertEquals(rollback?2L:4L,((Number)f.row(44).get("version")).longValue());
            assertEquals("latest child",f.row(44).get("title")); assertEquals(Long.valueOf(1),first.getVersion()); assertEquals(Long.valueOf(1),second.getVersion());
        }
    }

    private static Fixture fixture() throws Exception { return new Fixture(true,BlobPolicies.defaults(),List.of("pdf"),true); }
    private static int count(Fixture f) { return ((Number)f.otherSql.queryForList("SELECT count(*) FROM asset_data",new Object[0]).get(0).values().iterator().next()).intValue(); }
}
