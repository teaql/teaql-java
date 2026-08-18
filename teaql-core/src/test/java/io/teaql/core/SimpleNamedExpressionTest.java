package io.teaql.core;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SimpleNamedExpressionTest {

    static class DummyExpression implements Expression {
        @Override
        public List<String> properties(UserContext context) {
            return Arrays.asList("prop1", "prop2");
        }
    }

    @Test
    public void testConstructorsAndGetters() {
        Expression expr = new DummyExpression();
        SimpleNamedExpression namedExpr = new SimpleNamedExpression("name1", expr);
        
        assertEquals("name1", namedExpr.name());
        assertEquals(expr, namedExpr.getExpression());
        
        SimpleNamedExpression namedExpr2 = new SimpleNamedExpression("name2");
        assertEquals("name2", namedExpr2.name());
        assertTrue(namedExpr2.getExpression() instanceof PropertyReference);
        assertEquals("name2", ((PropertyReference) namedExpr2.getExpression()).getPropertyName());
    }

    @Test(expected = TeaQLRuntimeException.class)
    public void testConstructorWithNullExpression() {
        new SimpleNamedExpression("name1", null);
    }
    
    @Test
    public void testProperties() {
        SimpleNamedExpression expr = new SimpleNamedExpression("name", new DummyExpression());
        List<String> props = expr.properties(null);
        assertEquals(2, props.size());
        assertEquals("prop1", props.get(0));
        assertEquals("prop2", props.get(1));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        Expression dummy1 = new DummyExpression();
        Expression dummy2 = new DummyExpression();
        
        SimpleNamedExpression e1 = new SimpleNamedExpression("name1", dummy1);
        SimpleNamedExpression e2 = new SimpleNamedExpression("name1", dummy1);
        SimpleNamedExpression e3 = new SimpleNamedExpression("name2", dummy1);
        SimpleNamedExpression e4 = new SimpleNamedExpression("name1", dummy2);
        
        assertTrue(e1.equals(e1));
        assertTrue(e1.equals(e2));
        assertEquals(e1.hashCode(), e2.hashCode());
        
        assertFalse(e1.equals(null));
        assertFalse(e1.equals(new Object()));
        assertFalse(e1.equals(e3));
        assertFalse(e1.equals(e4));
    }
}
