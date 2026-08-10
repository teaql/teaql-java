package io.teaql.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConstantTest {

    @Test
    public void testGettersAndSetters() {
        Constant c = new Constant();
        assertNull(c.getValue());
        
        c.setValue("hello");
        assertEquals("hello", c.getValue());
        
        c.setValue(123);
        assertEquals(123, c.getValue());
    }

    @Test
    public void testEqualsAndHashCode() {
        Constant c1 = new Constant();
        c1.setValue("test");
        
        Constant c2 = new Constant();
        c2.setValue("test");
        
        Constant c3 = new Constant();
        c3.setValue("other");
        
        assertTrue(c1.equals(c1));
        assertTrue(c1.equals(c2));
        assertEquals(c1.hashCode(), c2.hashCode());
        
        assertFalse(c1.equals(null));
        assertFalse(c1.equals(new Object()));
        assertFalse(c1.equals(c3));
    }
}
