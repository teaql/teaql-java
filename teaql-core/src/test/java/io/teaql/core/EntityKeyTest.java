package io.teaql.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.Test;

public class EntityKeyTest {

    @Test
    public void equalKeysHaveEqualHashCodes() {
        EntityKey first = new EntityKey("Order", 42L);
        EntityKey second = new EntityKey("Order", 42L);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void entityTypeAndIdBothParticipateInEquality() {
        EntityKey key = new EntityKey("Order", 42L);

        assertNotEquals(key, new EntityKey("OrderItem", 42L));
        assertNotEquals(key, new EntityKey("Order", 43L));
        assertNotEquals(key, null);
        assertNotEquals(key, "Order:42");
    }

    @Test
    public void naturalOrderingSortsByEntityTypeThenId() {
        TreeSet<EntityKey> keys = new TreeSet<>();
        keys.add(new EntityKey("OrderItem", 1L));
        keys.add(new EntityKey("Order", 2L));
        keys.add(new EntityKey("Order", 1L));

        assertEquals(
                List.of(
                        new EntityKey("Order", 1L),
                        new EntityKey("Order", 2L),
                        new EntityKey("OrderItem", 1L)),
                new ArrayList<>(keys));
    }

    @Test
    public void nullIdSortsBeforeNonNullIdOfSameEntityType() {
        TreeSet<EntityKey> keys = new TreeSet<>();
        keys.add(new EntityKey("Order", 1L));
        keys.add(new EntityKey("Order", null));

        assertEquals(
                List.of(new EntityKey("Order", null), new EntityKey("Order", 1L)),
                new ArrayList<>(keys));
    }

    @Test
    public void nullEntityTypeIsRejected() {
        NullPointerException error =
                assertThrows(NullPointerException.class, () -> new EntityKey(null, 1L));

        assertEquals("entity type must not be null", error.getMessage());
    }

    @Test
    public void stringRepresentationUsesEntityAndId() {
        assertEquals("Order:42", new EntityKey("Order", 42L).toString());
        assertEquals("Order:null", new EntityKey("Order", null).toString());
    }

    @Test
    public void testCoverage() {
        EntityKey key = new EntityKey("Order", 1L);
        // Cover this == o
        assertEquals(key, key);
        
        // Cover other.id == null
        EntityKey keyNull = new EntityKey("Order", null);
        assertEquals(1, key.compareTo(keyNull));
    }
}
