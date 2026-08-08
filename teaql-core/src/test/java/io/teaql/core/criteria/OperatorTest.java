package io.teaql.core.criteria;

import org.junit.Test;
import static org.junit.Assert.*;

public class OperatorTest {

    @Test
    public void testOperatorByValue() {
        assertEquals(Operator.IS_NULL, Operator.operatorByValue("__is_null__"));
        assertEquals(Operator.IS_NULL, Operator.operatorByValue("__IS_NULL__")); // Ignore case
        assertEquals(Operator.IS_NOT_NULL, Operator.operatorByValue("__is_not_null__"));
        assertEquals(Operator.IS_NOT_NULL, Operator.operatorByValue("__IS_NOT_NULL__")); // Ignore case
        assertNull(Operator.operatorByValue("other"));
        assertNull(Operator.operatorByValue(""));
        
        // Handle null
        assertNull(Operator.operatorByValue(null));
    }
    
    @Test
    public void testOperatorByValueNull() {
        assertNull(Operator.operatorByValue(null));
    }

    @Test
    public void testHasOneOperator() {
        assertTrue(Operator.IS_NULL.hasOneOperator());
        assertTrue(Operator.IS_NOT_NULL.hasOneOperator());
        assertFalse(Operator.EQUAL.hasOneOperator());
        assertFalse(Operator.BETWEEN.hasOneOperator());
    }

    @Test
    public void testHasTwoOperator() {
        assertFalse(Operator.IS_NULL.hasTwoOperator());
        assertFalse(Operator.IS_NOT_NULL.hasTwoOperator());
        assertFalse(Operator.BETWEEN.hasTwoOperator());
        
        assertTrue(Operator.EQUAL.hasTwoOperator());
        assertTrue(Operator.CONTAIN.hasTwoOperator());
        assertTrue(Operator.GREATER_THAN.hasTwoOperator());
    }

    @Test
    public void testHasMultiValue() {
        assertTrue(Operator.IN.hasMultiValue());
        assertTrue(Operator.NOT_IN.hasMultiValue());
        assertTrue(Operator.IN_LARGE.hasMultiValue());
        assertTrue(Operator.NOT_IN_LARGE.hasMultiValue());
        
        assertFalse(Operator.EQUAL.hasMultiValue());
        assertFalse(Operator.IS_NULL.hasMultiValue());
        assertFalse(Operator.BETWEEN.hasMultiValue());
    }

    @Test
    public void testIsBetween() {
        assertTrue(Operator.BETWEEN.isBetween());
        
        assertFalse(Operator.EQUAL.isBetween());
        assertFalse(Operator.IN.isBetween());
        assertFalse(Operator.IS_NULL.isBetween());
    }
}
