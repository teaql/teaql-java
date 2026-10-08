package io.teaql.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class EntityChangeSetTest {

    private static final EntityKey ORDER = new EntityKey("Order", 1L);
    private static final EntityKey OTHER_ORDER = new EntityKey("Order", 2L);

    @Test
    public void newChangeSetIsEmpty() {
        EntityChangeSet changeSet = new EntityChangeSet();

        assertTrue(changeSet.isEmpty());
        assertTrue(changeSet.changes().isEmpty());
    }

    @Test
    public void dynamicIntentsHaveDistinctNamesAndStayTypeQualified() {
        var changes = new EntityChangeSet();
        var otherType = new EntityKey("Invoice", 1L);
        changes.set(ORDER, "status", "PAID");
        changes.setDynamic(ORDER, io.teaql.data.dynamic.DynamicFieldMutation.set("status", io.teaql.data.dynamic.DynamicDataType.STRING, null));
        changes.setDynamic(otherType, io.teaql.data.dynamic.DynamicFieldMutation.delete("status", io.teaql.data.dynamic.DynamicDataType.STRING));
        assertEquals(Set.of("status", "#status"), changes.fieldNames(ORDER));
        assertEquals(Set.of("#status"), changes.fieldNames(otherType));
        assertThrows(UnsupportedOperationException.class, () -> changes.dynamicChanges().get(ORDER).clear());
        assertEquals(io.teaql.data.dynamic.DynamicFieldMutation.Kind.SET, changes.dynamicChanges().get(ORDER).get("status").kind());
        assertNull(changes.dynamicChanges().get(ORDER).get("status").value());
        changes.clearEntity(ORDER);
        assertFalse(changes.isEmpty());
        assertTrue(changes.fieldNames(ORDER).isEmpty());
        assertEquals(io.teaql.data.dynamic.DynamicFieldMutation.Kind.DELETE, changes.dynamicChanges().get(otherType).get("status").kind());
    }

    @Test
    public void setMakesFieldValueReadable() {
        EntityChangeSet changeSet = new EntityChangeSet();

        changeSet.set(ORDER, "status", "PAID");

        assertFalse(changeSet.isEmpty());
        assertEquals("PAID", changeSet.get(ORDER, "status"));
        assertNull(changeSet.get(ORDER, "missing"));
    }

    @Test
    public void settingSameFieldKeepsLatestValue() {
        EntityChangeSet changeSet = new EntityChangeSet();
        changeSet.set(ORDER, "status", "CREATED");

        changeSet.set(ORDER, "status", "PAID");

        assertEquals("PAID", changeSet.get(ORDER, "status"));
        assertEquals(1, changeSet.fieldNames(ORDER).size());
    }

    @Test
    public void changesForDifferentEntitiesRemainIsolated() {
        EntityChangeSet changeSet = new EntityChangeSet();

        changeSet.set(ORDER, "status", "PAID");
        changeSet.set(OTHER_ORDER, "status", "CANCELLED");

        assertEquals("PAID", changeSet.get(ORDER, "status"));
        assertEquals("CANCELLED", changeSet.get(OTHER_ORDER, "status"));
    }

    @Test
    public void fieldNamesContainOnlyFieldsForRequestedEntity() {
        EntityChangeSet changeSet = new EntityChangeSet();
        changeSet.set(ORDER, "status", "PAID");
        changeSet.set(ORDER, "total", 100L);
        changeSet.set(OTHER_ORDER, "comment", "other");

        assertEquals(Set.of("status", "total"), changeSet.fieldNames(ORDER));
        assertEquals(Set.of("comment"), changeSet.fieldNames(OTHER_ORDER));
        assertTrue(changeSet.fieldNames(new EntityKey("Order", 3L)).isEmpty());
    }

    @Test
    public void clearEntityRemovesOnlyRequestedEntity() {
        EntityChangeSet changeSet = new EntityChangeSet();
        changeSet.set(ORDER, "status", "PAID");
        changeSet.set(OTHER_ORDER, "status", "CANCELLED");

        changeSet.clearEntity(ORDER);

        assertNull(changeSet.get(ORDER, "status"));
        assertEquals("CANCELLED", changeSet.get(OTHER_ORDER, "status"));
        assertFalse(changeSet.isEmpty());
    }

    @Test
    public void exposedCollectionsAreReadOnly() {
        EntityChangeSet changeSet = new EntityChangeSet();
        changeSet.set(ORDER, "status", "PAID");

        Map<EntityKey, Map<String, Object>> changes = changeSet.changes();
        Set<String> fieldNames = changeSet.fieldNames(ORDER);

        assertThrows(
                UnsupportedOperationException.class,
                () -> changes.put(OTHER_ORDER, Map.of("status", "CANCELLED")));
        assertThrows(UnsupportedOperationException.class, () -> fieldNames.add("total"));
    }
}
