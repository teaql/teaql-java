package io.teaql.core;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class OrderBysTest {

    @Test
    public void testGettersSettersAndAdd() {
        OrderBys orderBys = new OrderBys();
        assertTrue(orderBys.isEmpty());
        assertTrue(orderBys.getOrderBys().isEmpty());
        
        OrderBy o1 = new OrderBy("prop1");
        orderBys.addOrderBy(o1);
        assertFalse(orderBys.isEmpty());
        assertEquals(1, orderBys.getOrderBys().size());
        assertEquals(o1, orderBys.getOrderBys().get(0));
        
        // Add null should be ignored
        orderBys.addOrderBy(null);
        assertEquals(1, orderBys.getOrderBys().size());
        
        List<OrderBy> list = new ArrayList<>(Arrays.asList(o1, new OrderBy("prop2")));
        orderBys.setOrderBys(list);
        assertEquals(2, orderBys.getOrderBys().size());
    }
    
    @Test
    public void testProperties() {
        OrderBys orderBys = new OrderBys();
        orderBys.addOrderBy(new OrderBy("prop1"));
        orderBys.addOrderBy(new OrderBy("prop2"));
        
        List<String> props = orderBys.properties(null);
        assertEquals(2, props.size());
        assertEquals("prop1", props.get(0));
        assertEquals("prop2", props.get(1));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        OrderBys obs1 = new OrderBys();
        obs1.addOrderBy(new OrderBy("prop1"));
        
        OrderBys obs2 = new OrderBys();
        obs2.addOrderBy(new OrderBy("prop1"));
        
        OrderBys obs3 = new OrderBys();
        obs3.addOrderBy(new OrderBy("prop2"));
        
        assertTrue(obs1.equals(obs1));
        assertTrue(obs1.equals(obs2));
        assertEquals(obs1.hashCode(), obs2.hashCode());
        
        assertFalse(obs1.equals(null));
        assertFalse(obs1.equals(new Object()));
        assertFalse(obs1.equals(obs3));
    }
}
