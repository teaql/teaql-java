package io.teaql.core;

import io.teaql.core.criteria.Operator;
import org.junit.Test;
import java.util.*;

import static org.junit.Assert.*;

public class ParameterTest {

    static class DummyEntity extends BaseEntity {
        private String idStr;
        public DummyEntity(Long id) { this.updateId(id); }
        public DummyEntity(String idStr) { this.idStr = idStr; }
        @Override public String typeName() { return "dummy"; }
    }

    @Test
    public void testFlatValues() {
        // null
        List<?> vals = Parameter.flatValues(null);
        assertTrue(vals.isEmpty());
        
        // Single value
        vals = Parameter.flatValues("single");
        assertEquals(1, vals.size());
        assertEquals("single", vals.get(0));
        
        // Array
        vals = Parameter.flatValues(new String[]{"a", "b"});
        assertEquals(2, vals.size());
        assertEquals("a", vals.get(0));
        assertEquals("b", vals.get(1));
        
        // Collection / Iterable
        vals = Parameter.flatValues(Arrays.asList("x", "y"));
        assertEquals(2, vals.size());
        assertEquals("x", vals.get(0));
        assertEquals("y", vals.get(1));
        
        // Iterator
        vals = Parameter.flatValues(Arrays.asList("1", "2").iterator());
        assertEquals(2, vals.size());
        assertEquals("1", vals.get(0));
        assertEquals("2", vals.get(1));
        
        // Entity
        vals = Parameter.flatValues(new DummyEntity(1L));
        assertEquals(1, vals.size());
        assertEquals(1L, vals.get(0));
        
        // Nested array/collection
        vals = Parameter.flatValues(Arrays.asList(new String[]{"nested1", "nested2"}, new DummyEntity(2L)));
        assertEquals(3, vals.size());
        assertEquals("nested1", vals.get(0));
        assertEquals("nested2", vals.get(1));
        assertEquals(2L, vals.get(2));
    }
    
    @Test
    public void testParameterConstructorAndMethods() {
        // Multi-value operator (e.g. IN might be true for hasMultiValue, depending on enum, assuming EQ is single)
        Parameter p1 = new Parameter("prop1", Arrays.asList("v1", "v2"), Operator.IN);
        assertEquals("prop1", p1.getName());
        assertEquals(Operator.IN, p1.getOperator());
        // For IN operator, value should be a list
        assertTrue(p1.getValue() instanceof List);
        assertEquals(2, ((List<?>) p1.getValue()).size());
        
        // Single value operator (e.g. EQ)
        Parameter p2 = new Parameter("prop2", Arrays.asList("v1", "v2"), Operator.EQUAL);
        assertEquals("prop2", p2.getName());
        assertEquals(Operator.EQUAL, p2.getOperator());
        // Only first value
        assertEquals("v1", p2.getValue());
        
        // Set operator
        p2.setOperator(Operator.CONTAIN);
        assertEquals(Operator.CONTAIN, p2.getOperator());
        
        // Equals and HashCode
        Parameter p3 = new Parameter("prop2", Arrays.asList("v1", "v2"), Operator.CONTAIN);
        assertTrue(p2.equals(p3));
        assertEquals(p2.hashCode(), p3.hashCode());
        
        Parameter p4 = new Parameter("prop3", "v1", Operator.EQUAL);
        assertFalse(p2.equals(p4));
        assertFalse(p2.equals(null));
        assertFalse(p2.equals(new Object()));
    }
    
    @Test
    public void testPrivateConstructorsViaReflection() throws Exception {
        // Using reflection to cover the private constructors if needed, or we might not need to if not used.
        // If not used, they might be dead code, but we can call them to be safe.
        java.lang.reflect.Constructor<Parameter> c1 = Parameter.class.getDeclaredConstructor(String.class, Object.class, boolean.class);
        c1.setAccessible(true);
        Parameter p1 = c1.newInstance("name1", Arrays.asList("v1", "v2"), true);
        assertEquals("name1", p1.getName());
        assertTrue(p1.getValue() instanceof Object[]);
        assertEquals(2, ((Object[]) p1.getValue()).length);
        
        Parameter p2 = c1.newInstance("name2", Arrays.asList("v1", "v2"), false);
        assertEquals("name2", p2.getName());
        assertEquals("v1", p2.getValue());
        
        java.lang.reflect.Constructor<Parameter> c2 = Parameter.class.getDeclaredConstructor(String.class, Object.class);
        c2.setAccessible(true);
        Parameter p3 = c2.newInstance("name3", "v3");
        assertEquals("name3", p3.getName());
        assertTrue(p3.getValue() instanceof Object[]);
        assertEquals(1, ((Object[]) p3.getValue()).length);
    }
}
