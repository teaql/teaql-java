package io.teaql.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EntityRootTest {

    private static final EntityKey ORDER = new EntityKey("Order", 1L);
    private static final EntityKey OTHER_ORDER = new EntityKey("Order", 2L);

    @Test
    public void markAsNewRecordsOnlyRequestedKeyAndIsIdempotent() {
        EntityRoot root = new EntityRoot();

        root.markAsNew(ORDER);
        root.markAsNew(ORDER);

        assertTrue(root.isNew(ORDER));
        assertFalse(root.isNew(OTHER_ORDER));
        assertEquals(1, root.newKeys().size());
    }

    @Test
    public void newKeysViewIsReadOnly() {
        EntityRoot root = new EntityRoot();
        root.markAsNew(ORDER);

        assertThrows(UnsupportedOperationException.class, () -> root.newKeys().add(OTHER_ORDER));
    }

    @Test
    public void markAsDeleteRecordsOnlyRequestedKeyAndIsIdempotent() {
        EntityRoot root = new EntityRoot();

        root.markAsDelete(ORDER);
        root.markAsDelete(ORDER);

        assertTrue(root.isMarkedAsDelete(ORDER));
        assertFalse(root.isMarkedAsDelete(OTHER_ORDER));
        assertEquals(1, root.deletedKeys().size());
    }

    @Test
    public void deletedKeysViewIsReadOnly() {
        EntityRoot root = new EntityRoot();
        root.markAsDelete(ORDER);

        assertThrows(
                UnsupportedOperationException.class,
                () -> root.deletedKeys().add(OTHER_ORDER));
    }

    @Test
    public void markingEntityDeletedClearsItsChangesFromEveryScopeOnly() {
        EntityRoot root = new EntityRoot();
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
}
