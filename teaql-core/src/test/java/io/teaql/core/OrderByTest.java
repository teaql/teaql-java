package io.teaql.core;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.*;

public class OrderByTest {

    @Test
    public void testConstructorsAndGettersSetters() {
        OrderBy o1 = new OrderBy("prop1");
        assertEquals("ASC", o1.getDirection());
        assertTrue(o1.getExpression() instanceof AggrExpression);
        AggrExpression exp1 = (AggrExpression) o1.getExpression();
        assertEquals(AggrFunction.SELF, exp1.getOperator());
        
        OrderBy o2 = new OrderBy("prop2", "DESC");
        assertEquals("DESC", o2.getDirection());
        
        OrderBy o3 = new OrderBy(AggrFunction.MAX, "prop3", "ASC");
        assertEquals("ASC", o3.getDirection());
        AggrExpression exp3 = (AggrExpression) o3.getExpression();
        assertEquals(AggrFunction.MAX, exp3.getOperator());
        
        o3.setDirection("DESC");
        assertEquals("DESC", o3.getDirection());
        
        Expression newExp = new PropertyReference("prop4");
        o3.setExpression(newExp);
        assertEquals(newExp, o3.getExpression());
    }
    
    @Test
    public void testProperties() {
        OrderBy o1 = new OrderBy("prop1");
        List<String> props = o1.properties(null);
        assertEquals(1, props.size());
        assertEquals("prop1", props.get(0));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        OrderBy o1 = new OrderBy("prop1");
        OrderBy o2 = new OrderBy("prop1");
        OrderBy o3 = new OrderBy("prop2");
        OrderBy o4 = new OrderBy("prop1", "DESC");
        
        assertTrue(o1.equals(o1));
        assertTrue(o1.equals(o2));
        assertEquals(o1.hashCode(), o2.hashCode());
        
        assertFalse(o1.equals(null));
        assertFalse(o1.equals(new Object()));
        assertFalse(o1.equals(o3));
        assertFalse(o1.equals(o4));
    }
}
