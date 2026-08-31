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
        source.setTraceChain(ORDER, "checkout > submit");
        source.setOriginalVersion(ORDER, 7L);

        target.mergeFrom(source);

        assertEquals("checkout > submit", target.getTraceChain(ORDER));
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
}
