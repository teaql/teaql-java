package io.teaql.core;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.*;

public class SubQuerySearchCriteriaTest {

    @Test
    public void testConstructorsAndGettersSetters() {
        SearchRequest req1 = new BaseRequest(Entity.class) {};
        SearchRequest req2 = new BaseRequest(Entity.class) {};
        
        SubQuerySearchCriteria criteria = new SubQuerySearchCriteria("prop1", req1, "depProp1");
        
        assertEquals("prop1", criteria.getPropertyName());
        assertEquals(req1, criteria.getDependsOn());
        assertEquals("depProp1", criteria.getDependsOnPropertyName());
        
        criteria.setPropertyName("prop2");
        criteria.setDependsOn(req2);
        criteria.setDependsOnPropertyName("depProp2");
        
        assertEquals("prop2", criteria.getPropertyName());
        assertEquals(req2, criteria.getDependsOn());
        assertEquals("depProp2", criteria.getDependsOnPropertyName());
    }
    
    @Test
    public void testProperties() {
        SearchRequest req1 = new BaseRequest(Entity.class) {};
        SubQuerySearchCriteria criteria = new SubQuerySearchCriteria("prop1", req1, "depProp1");
        List<String> props = criteria.properties(null);
        assertEquals(1, props.size());
        assertEquals("prop1", props.get(0));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        SearchRequest req1 = new BaseRequest(Entity.class) {};
        SearchRequest req2 = new BaseRequest(Entity.class) {};
        
        SubQuerySearchCriteria c1 = new SubQuerySearchCriteria("prop1", req1, "depProp1");
        SubQuerySearchCriteria c2 = new SubQuerySearchCriteria("prop1", req1, "depProp1");
        SubQuerySearchCriteria c3 = new SubQuerySearchCriteria("prop2", req1, "depProp1");
        SubQuerySearchCriteria c4 = new SubQuerySearchCriteria("prop1", req2, "depProp1");
        SubQuerySearchCriteria c5 = new SubQuerySearchCriteria("prop1", req1, "depProp2");
        
        assertTrue(c1.equals(c1));
        assertTrue(c1.equals(c2));
        assertEquals(c1.hashCode(), c2.hashCode());
        
        assertFalse(c1.equals(null));
        assertFalse(c1.equals(new Object()));
        assertFalse(c1.equals(c3));
        assertFalse(c1.equals(c4));
        assertFalse(c1.equals(c5));
    }
}
