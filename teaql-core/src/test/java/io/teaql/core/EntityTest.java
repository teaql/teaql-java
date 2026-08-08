package io.teaql.core;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class EntityTest {

    @Test
    public void testNonBaseEntityDefaultMethods() {
        Entity nonBaseEntity = new Entity() {
            @Override public Long getId() { return null; }
            @Override public Long getVersion() { return null; }
            @Override public boolean newItem() { return false; }
            @Override public boolean updateItem() { return false; }
            @Override public boolean deleteItem() { return false; }
            @Override public boolean needPersist() { return false; }
            @Override public List<String> getUpdatedProperties() { return null; }
            @Override public void addRelation(String relationName, Entity value) {}
            @Override public void addDynamicProperty(String propertyName, Object value) {}
            @Override public void appendDynamicProperty(String propertyName, Object value) {}
            @Override public <T> T getDynamicProperty(String propertyName) { return null; }
            @Override public void markAsDeleted() {}
            @Override public void markAsRecover() {}
        };

        boolean getThrows = false;
        try {
            nonBaseEntity.getProperty("prop");
        } catch (UnsupportedOperationException e) {
            getThrows = true;
        }
        assertEquals(true, getThrows);

        boolean setThrows = false;
        try {
            nonBaseEntity.setProperty("prop", "val");
        } catch (UnsupportedOperationException e) {
            setThrows = true;
        }
        assertEquals(true, setThrows);
    }

    @Test
    public void testBaseEntityDefaultMethods() {
        Entity baseEntity = new BaseEntity() {
            @Override public String typeName() { return "dummy"; }
            @Override public Object __internalGet(String propertyName) { return "val"; }
            @Override public void __internalSet(String propertyName, Object value) {}
        };
        
        // This will cover the false branch of 'instanceof BaseEntity' in Entity.java
        assertEquals("val", baseEntity.getProperty("prop"));
        baseEntity.setProperty("prop", "newVal");
    }
}
