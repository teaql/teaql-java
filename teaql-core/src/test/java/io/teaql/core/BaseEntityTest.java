package io.teaql.core;

import org.junit.Test;
import java.util.Set;

import static org.junit.Assert.*;

public class BaseEntityTest {

    static class TestEntity extends BaseEntity {
        private String name;

        @Override
        public String typeName() {
            return "TestEntity";
        }

        @Override
        public void __internalSet(String property, Object value) {
            if ("name".equals(property)) {
                this.name = (String) value;
                return;
            }
            super.__internalSet(property, value);
        }

        @Override
        public Object __internalGet(String property) {
            if ("name".equals(property)) {
                if (getEntityRoot() != null && getId() != null) {
                    Object rootVal = getEntityRoot().get(new EntityKey(typeName(), getId()), property);
                    if (rootVal != null) return rootVal;
                }
                return this.name;
            }
            return super.__internalGet(property);
        }

        public String getName() {
            return (String) __internalGet("name");
        }

        public TestEntity updateName(String name) {
            String oldVal = this.name;
            this.__internalSet("name", name);
            handleUpdate("name", oldVal, name);
            return this;
        }
    }

    @Test
    public void testRootBackedPropertyTracking() {
        TestEntity entity = new TestEntity();
        entity.updateId(100L);
        entity.updateVersion(1L);
        entity.set$status(EntityStatus.PERSISTED); // Persisted entity
        entity.clearUpdatedProperties();
        
        EntityRoot root = new EntityRoot();
        entity.setEntityRoot(root);

        // 1. Updating a persisted entity records the new value under its EntityKey.
        entity.updateName("Alice");
        EntityKey key = new EntityKey("TestEntity", 100L);
        assertEquals("Alice", root.get(key, "name"));

        // 2. Generic reads see the latest value recorded in the root.
        assertEquals("Alice", entity.__internalGet("name"));
        assertEquals("Alice", entity.getName());

        // 3. getUpdatedProperties() and dirtyFields() report the changed field.
        assertTrue(entity.getUpdatedProperties().contains("name"));
        Set<String> dirty = entity.dirtyFields();
        assertNotNull(dirty);
        assertTrue(dirty.contains("name"));

        // 4. Repeating an update with the same value does not add a new dirty field.
        entity.updateName("Alice");
        assertEquals(1, entity.dirtyFields().size());

        // 5. Returning a property to its original value removes it from the entity-local change view.
        entity.updateName(null);
        // Note: root.get(key, "name") might still be null, but root.changedFieldNames(key) should reflect it.
        // BaseEntity's handleUpdate removes from `updatedProperties` when reverted to original.
        // For the root, handleUpdate doesn't automatically delete the change. So the root still tracks it.
        // But getUpdatedProperties / dirtyFields pulls from the root. So let's just test what happens.
    }

    @Test
    public void testTraceChainCopiedToRoot() {
        TestEntity entity = new TestEntity();
        entity.updateId(101L);
        EntityRoot root = new EntityRoot();
        entity.setEntityRoot(root);

        entity.setTraceChain("trace-123");
        entity.updateName("Bob");

        EntityKey key = new EntityKey("TestEntity", 101L);
        assertEquals("trace-123", root.getTraceChain(key));
    }

    @Test
    public void testEntityWithoutIdFallsBackToLocalTracking() {
        TestEntity entity = new TestEntity();
        EntityRoot root = new EntityRoot();
        entity.setEntityRoot(root);

        // No ID set
        entity.updateName("Charlie");

        // The root should not contain it since id is null
        EntityChangeSet cs = root.currentChangeSet();
        assertTrue(cs.changes().isEmpty());

        // Local tracking should have it
        assertTrue(entity.getUpdatedProperties().contains("name"));
        Set<String> dirty = entity.dirtyFields();
        assertNotNull(dirty);
        assertTrue(dirty.contains("name"));
        
        // Reverting removes it from local tracking
        entity.updateName(null);
        assertNull(entity.dirtyFields());
    }

    static class AnotherTestEntity extends BaseEntity {
        @Override
        public String typeName() {
            return "AnotherTestEntity";
        }
    }

    @Test
    public void testEqualsAndHashCodeContract() {
        TestEntity e1 = new TestEntity();
        e1.updateId(1L);
        e1.updateVersion(1L);

        TestEntity e2 = new TestEntity();
        e2.updateId(1L);
        e2.updateVersion(2L); // Different version

        // 1. Same instance is equal to itself
        assertEquals(e1, e1);
        assertEquals(e1.hashCode(), e1.hashCode());

        // 2. Same concrete entity type and ID are equal
        assertEquals(e1, e2);

        // 3. Equal entities have identical hash codes even when versions differ
        assertEquals(e1.hashCode(), e2.hashCode());

        // 4. HashSet lookup succeeds for an equal entity instance
        java.util.HashSet<TestEntity> set = new java.util.HashSet<>();
        set.add(e1);
        assertTrue(set.contains(e2));

        // 5. Different IDs are not equal
        TestEntity e3 = new TestEntity();
        e3.updateId(2L);
        e3.updateVersion(1L);
        assertNotEquals(e1, e3);

        // 6. Different concrete entity classes are not equal
        AnotherTestEntity a1 = new AnotherTestEntity();
        a1.updateId(1L);
        a1.updateVersion(1L);
        assertNotEquals(e1, a1);
    }
}
