package io.teaql.core;

import io.teaql.core.criteria.Operator;
import org.junit.Test;

import static org.junit.Assert.*;

public class TypeCriteriaTest {

    @Test
    public void testConstructorsAndGettersSetters() {
        TypeCriteria tc1 = new TypeCriteria();
        assertNull(tc1.getTypeParameter());
        
        Parameter param1 = new Parameter("type", "dummy", Operator.EQUAL);
        TypeCriteria tc2 = new TypeCriteria(param1);
        assertEquals(param1, tc2.getTypeParameter());
        
        Parameter param2 = new Parameter("type", "dummy2", Operator.EQUAL);
        tc2.setTypeParameter(param2);
        assertEquals(param2, tc2.getTypeParameter());
    }

    @Test
    public void testEqualsAndHashCode() {
        Parameter param1 = new Parameter("type", "dummy1", Operator.EQUAL);
        Parameter param2 = new Parameter("type", "dummy2", Operator.EQUAL);
        
        TypeCriteria tc1 = new TypeCriteria(param1);
        TypeCriteria tc2 = new TypeCriteria(param1);
        TypeCriteria tc3 = new TypeCriteria(param2);
        
        assertTrue(tc1.equals(tc1));
        assertTrue(tc1.equals(tc2));
        assertEquals(tc1.hashCode(), tc2.hashCode());
        
        assertFalse(tc1.equals(null));
        assertFalse(tc1.equals(new Object()));
        assertFalse(tc1.equals(tc3));
    }
}
