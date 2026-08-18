package io.teaql.core;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import io.teaql.core.criteria.Operator;

import static org.junit.Assert.*;

public class FunctionApplyTest {

    static class DummyExpression implements Expression {
        private String prop;
        public DummyExpression(String prop) { this.prop = prop; }
        @Override
        public List<String> properties(UserContext context) {
            return prop == null ? null : Arrays.asList(prop);
        }
    }

    @Test
    public void testConstructorAndGetters() {
        Expression e1 = new DummyExpression("p1");
        Expression e2 = new DummyExpression("p2");
        Expression e3 = new DummyExpression("p3");
        
        FunctionApply func = new FunctionApply(Operator.EQUAL, e1, e2, e3);
        assertEquals(Operator.EQUAL, func.getOperator());
        assertEquals(3, func.getExpressions().size());
        assertEquals(e1, func.first());
        assertEquals(e2, func.second());
        assertEquals(e3, func.third());
        assertEquals(e3, func.last());
    }
    
    @Test
    public void testLastAndEmptyCases() {
        Expression e1 = new DummyExpression("p1");
        FunctionApply func = new FunctionApply(Operator.IS_NULL, e1);
        assertEquals(e1, func.first());
        assertNull(func.second());
        assertNull(func.third());
        assertEquals(e1, func.last());
    }
    
    @Test(expected = TeaQLRuntimeException.class)
    public void testEmptyExpressionsThrowsException() {
        new FunctionApply(Operator.EQUAL);
    }

    @Test(expected = TeaQLRuntimeException.class)
    public void testNullExpressionsThrowsException() {
        new FunctionApply(Operator.EQUAL, (Expression[]) null);
    }
    
    @Test
    public void testProperties() {
        Expression e1 = new DummyExpression("p1");
        Expression e2 = new DummyExpression(null);
        Expression e3 = new DummyExpression("p3");
        
        FunctionApply func = new FunctionApply(Operator.CONTAIN, e1, e2, e3);
        List<String> props = func.properties(null);
        assertEquals(2, props.size());
        assertEquals("p1", props.get(0));
        assertEquals("p3", props.get(1));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        Expression e1 = new DummyExpression("p1");
        Expression e2 = new DummyExpression("p2");
        
        FunctionApply f1 = new FunctionApply(Operator.EQUAL, e1, e2);
        FunctionApply f2 = new FunctionApply(Operator.EQUAL, e1, e2);
        FunctionApply f3 = new FunctionApply(Operator.NOT_EQUAL, e1, e2);
        FunctionApply f4 = new FunctionApply(Operator.EQUAL, e1);
        
        assertTrue(f1.equals(f1));
        assertTrue(f1.equals(f2));
        assertEquals(f1.hashCode(), f2.hashCode());
        
        assertFalse(f1.equals(null));
        assertFalse(f1.equals(new Object()));
        assertFalse(f1.equals(f3));
        assertFalse(f1.equals(f4));
    }
}
