package io.teaql.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class SimpleAggregationTest {

    @Test
    public void testConstructorsAndGettersSetters() {
        SearchRequest req1 = new BaseRequest(Entity.class) {};
        SearchRequest req2 = new BaseRequest(Entity.class) {};
        
        SimpleAggregation agg1 = new SimpleAggregation("agg1", req1);
        assertEquals("agg1", agg1.getName());
        assertEquals(req1, agg1.getAggregateRequest());
        assertFalse(agg1.isSingleNumber()); // default boolean is false
        
        SimpleAggregation agg2 = new SimpleAggregation("agg2", req2, true);
        assertEquals("agg2", agg2.getName());
        assertEquals(req2, agg2.getAggregateRequest());
        assertTrue(agg2.isSingleNumber());
        
        agg1.setName("agg3");
        agg1.setAggregateRequest(req2);
        agg1.setSingleNumber(true);
        
        assertEquals("agg3", agg1.getName());
        assertEquals(req2, agg1.getAggregateRequest());
        assertTrue(agg1.isSingleNumber());
    }
    
    @Test
    public void testEqualsAndHashCode() {
        SearchRequest req = new BaseRequest(Entity.class) {};
        SimpleAggregation agg1 = new SimpleAggregation("agg1", req, true);
        SimpleAggregation agg2 = new SimpleAggregation("agg1", req, true);
        SimpleAggregation agg3 = new SimpleAggregation("agg3", req, true);
        SimpleAggregation agg4 = new SimpleAggregation("agg1", req, false);
        
        assertTrue(agg1.equals(agg1));
        assertTrue(agg1.equals(agg2));
        assertEquals(agg1.hashCode(), agg2.hashCode());
        
        assertFalse(agg1.equals(null));
        assertFalse(agg1.equals(new Object()));
        assertFalse(agg1.equals(agg3));
        assertFalse(agg1.equals(agg4));
    }
}
