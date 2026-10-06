package io.teaql.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EntityMutationLedgerTest {

    private static final EntityKey ORDER = new EntityKey("Order", 1L);
    private static final EntityKey OTHER_ORDER = new EntityKey("Order", 2L);

    @Test
    public void markAsNewRecordsOnlyRequestedKeyAndIsIdempotent() {
        EntityMutationLedger root = new EntityMutationLedger();

        root.markAsNew(ORDER);
        root.markAsNew(ORDER);

        assertTrue(root.isNew(ORDER));
        assertFalse(root.isNew(OTHER_ORDER));
        assertEquals(1, root.newKeys().size());
    }

    @Test
    public void newKeysViewIsReadOnly() {
        EntityMutationLedger root = new EntityMutationLedger();
        root.markAsNew(ORDER);

        assertThrows(UnsupportedOperationException.class, () -> root.newKeys().add(OTHER_ORDER));
    }

    @Test
    public void markAsDeleteRecordsOnlyRequestedKeyAndIsIdempotent() {
        EntityMutationLedger root = new EntityMutationLedger();

        root.markAsDelete(ORDER);
        root.markAsDelete(ORDER);

        assertTrue(root.isMarkedAsDelete(ORDER));
        assertFalse(root.isMarkedAsDelete(OTHER_ORDER));
        assertEquals(1, root.deletedKeys().size());
    }

    @Test
    public void deletedKeysViewIsReadOnly() {
        EntityMutationLedger root = new EntityMutationLedger();
        root.markAsDelete(ORDER);

        assertThrows(
                UnsupportedOperationException.class,
                () -> root.deletedKeys().add(OTHER_ORDER));
    }

    @Test
    public void markingEntityDeletedClearsItsChangesFromEveryScopeOnly() {
        EntityMutationLedger root = new EntityMutationLedger();
        root.set(ORDER, "status", "CREATED");
        root.set(OTHER_ORDER, "status", "CREATED");
        root.pushChangeSet();
        root.set(ORDER, "total", 100L);
        root.set(OTHER_ORDER, "total", 200L);

        root.markAsDelete(ORDER);

        assertTrue(root.changedFieldNames(ORDER).isEmpty());
        assertNull(root.get(ORDER, "status"));
        assertNull(root.get(ORDER, "total"));
        assertEquals("CREATED", root.get(OTHER_ORDER, "status"));
        assertEquals(200L, root.get(OTHER_ORDER, "total"));
    }

    @Test
    public void mergeFromNullIsNoOp() {
        EntityMutationLedger target = new EntityMutationLedger();
        target.set(ORDER, "status", "CREATED");

        target.mergeFrom(null);

        assertEquals("CREATED", target.get(ORDER, "status"));
        assertEquals(1, target.currentChangeSet().changes().size());
    }

    @Test
    public void mergeFromCopiesChangesAndPreservesExistingTargetChanges() {
        EntityMutationLedger target = new EntityMutationLedger();
        target.set(ORDER, "status", "CREATED");
        EntityMutationLedger source = new EntityMutationLedger();
        source.set(OTHER_ORDER, "status", "PAID");
        source.set(OTHER_ORDER, "total", 200L);

        target.mergeFrom(source);

        assertEquals("CREATED", target.get(ORDER, "status"));
        assertEquals("PAID", target.get(OTHER_ORDER, "status"));
        assertEquals(200L, target.get(OTHER_ORDER, "total"));
    }

    @Test
    public void mergeFromCopiesNewAndDeletedKeys() {
        EntityMutationLedger target = new EntityMutationLedger();
        EntityMutationLedger source = new EntityMutationLedger();
        source.markAsNew(ORDER);
        source.markAsDelete(OTHER_ORDER);

        target.mergeFrom(source);

        assertTrue(target.isNew(ORDER));
        assertTrue(target.isMarkedAsDelete(OTHER_ORDER));
    }

    @Test
    public void mergeFromCopiesTraceChainsAndOriginalVersions() {
        EntityMutationLedger target = new EntityMutationLedger();
        EntityMutationLedger source = new EntityMutationLedger();
        var trace = java.util.List.of(new TraceNode(TraceKind.AUDIT_REASON, "Order", 1L, "checkout > submit"));
        source.setTraceChain(ORDER, trace);
        source.setOriginalVersion(ORDER, 7L);

        target.mergeFrom(source);

        assertEquals(trace, target.getTraceChain(ORDER));
        assertEquals(Long.valueOf(7L), target.getOriginalVersion(ORDER));
    }

    @Test
    public void mergeLeavesSourceAndTargetIndependent() {
        EntityMutationLedger target = new EntityMutationLedger();
        EntityMutationLedger source = new EntityMutationLedger();
        source.set(ORDER, "status", "CREATED");
        source.markAsNew(ORDER);

        target.mergeFrom(source);
        target.set(ORDER, "status", "PAID");
        target.markAsNew(OTHER_ORDER);

        assertEquals("CREATED", source.get(ORDER, "status"));
        assertTrue(source.isNew(ORDER));
        assertFalse(source.isNew(OTHER_ORDER));
        assertEquals(1, source.newKeys().size());
    }

    @Test
    public void successfulSaveClearsThePreviousOptimisticLockBaseline() {
        EntityMutationLedger ledger = new EntityMutationLedger();
        ledger.setOriginalVersion(ORDER, 7L);
        ledger.set(ORDER, "status", "PAID");

        ledger.clearCurrentChangeSet();

        assertNull(ledger.getOriginalVersion(ORDER));
        assertTrue(ledger.currentChangeSet().changes().isEmpty());
    }

    @Test
    public void recoveryWithoutFieldsSurvivesMergeAndClearsAfterSave() {
        EntityMutationLedger source = new EntityMutationLedger();
        source.markAsRecover(ORDER);
        source.markAsRecover(ORDER);
        source.setOriginalVersion(ORDER, -2L);
        EntityMutationLedger target = new EntityMutationLedger();
        target.mergeFrom(source);

        assertEquals(java.util.Set.of(ORDER), target.recoveredKeys());
        assertEquals(Long.valueOf(-2), target.getOriginalVersion(ORDER));
        assertTrue(target.currentChangeSet().changes().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> target.recoveredKeys().add(OTHER_ORDER));
        target.clearCurrentChangeSet();
        assertTrue(target.recoveredKeys().isEmpty());
        assertEquals(java.util.Set.of(ORDER), source.recoveredKeys());
    }

    @Test
    public void deleteAndRecoverKeysAreMutuallyExclusiveAndTypeQualified() {
        EntityMutationLedger ledger = new EntityMutationLedger();
        EntityKey payment = new EntityKey("Payment", ORDER.id());
        ledger.markAsDelete(ORDER);
        ledger.markAsDelete(payment);
        ledger.markAsRecover(ORDER);
        assertEquals(java.util.Set.of(payment), ledger.deletedKeys());
        assertEquals(java.util.Set.of(ORDER), ledger.recoveredKeys());
        ledger.markAsDelete(ORDER);
        assertTrue(ledger.recoveredKeys().isEmpty());
        assertEquals(java.util.Set.of(ORDER, payment), ledger.deletedKeys());
    }

    @Test public void entityMergeDoesNotImportUnrelatedKeysWithTheSameId() {
        EntityKey payment = new EntityKey("Payment", ORDER.id());
        var source = new EntityMutationLedger();
        source.set(ORDER, "status", "PAID");
        source.markAsNew(ORDER);
        source.setOriginalVersion(ORDER, 7L);
        var trace = java.util.List.of(new TraceNode(TraceKind.AUDIT_REASON, "Order", 1L, "submit order"));
        source.setTraceChain(ORDER, trace);
        source.markAsDelete(payment);
        source.setOriginalVersion(payment, 90L);
        var target = new EntityMutationLedger();
        assertTrue(target.mergeEntityFrom(source, ORDER));
        assertEquals("PAID", target.get(ORDER, "status"));
        assertEquals(Long.valueOf(7), target.getOriginalVersion(ORDER));
        assertEquals(trace, target.getTraceChain(ORDER));
        assertTrue(target.isNew(ORDER));
        assertFalse(target.isMarkedAsDelete(payment));
        assertNull(target.getOriginalVersion(payment));
        assertTrue(source.isMarkedAsDelete(payment));
        target.set(ORDER, "status", "APPROVED");
        assertEquals("PAID", source.get(ORDER, "status"));
    }

    @Test public void readOnlyEntityMergeDoesNotImportAnotherGraphsPendingChanges() {
        var source = new EntityMutationLedger();
        source.setOriginalVersion(ORDER, 1L); // Loaded snapshot, no mutation.
        source.set(OTHER_ORDER, "status", "SUBMITTED");
        source.markAsNew(OTHER_ORDER);
        var target = new EntityMutationLedger();
        assertFalse(target.mergeEntityFrom(source, ORDER));
        assertTrue(target.currentChangeSet().changes().isEmpty());
        assertTrue(target.newKeys().isEmpty());
        assertNull(target.getOriginalVersion(ORDER));
        assertFalse(target.mergeEntityFrom(null, ORDER));
    }

    @Test public void entityMergePreservesFieldlessDeletionAndRecovery() {
        var source = new EntityMutationLedger();
        source.markAsDelete(ORDER);
        source.setOriginalVersion(ORDER, 4L);
        source.markAsRecover(OTHER_ORDER);
        source.setOriginalVersion(OTHER_ORDER, -5L);
        var target = new EntityMutationLedger();
        assertTrue(target.mergeEntityFrom(source, ORDER));
        assertTrue(target.isMarkedAsDelete(ORDER));
        assertEquals(Long.valueOf(4), target.getOriginalVersion(ORDER));
        assertTrue(target.mergeEntityFrom(source, OTHER_ORDER));
        assertTrue(target.recoveredKeys().contains(OTHER_ORDER));
        assertEquals(Long.valueOf(-5), target.getOriginalVersion(OTHER_ORDER));
        assertTrue(target.currentChangeSet().changes().isEmpty());
    }
}
