package io.teaql.core.criteria;

import io.teaql.core.SearchCriteria;
import io.teaql.core.UserContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VersionSearchCriteriaTest {

    static class DummyCriteria implements SearchCriteria {
        private String prop;
        public DummyCriteria(String prop) { this.prop = prop; }
        @Override
        public List<String> properties(UserContext ctx) {
            return prop == null ? null : Arrays.asList(prop);
        }
    }

    @Test
    public void testGettersSettersAndProperties() {
        SearchCriteria c1 = new DummyCriteria("p1");
        VersionSearchCriteria vc1 = new VersionSearchCriteria(c1);
        
        assertEquals(c1, vc1.getSearchCriteria());
        
        List<String> props = vc1.properties(null);
        assertEquals(1, props.size());
        assertEquals("p1", props.get(0));
        
        SearchCriteria c2 = new DummyCriteria("p2");
        vc1.setSearchCriteria(c2);
        assertEquals(c2, vc1.getSearchCriteria());
    }

    @Test
    public void testEqualsAndHashCode() {
        SearchCriteria c1 = new DummyCriteria("p1");
        SearchCriteria c2 = new DummyCriteria("p2");
        
        VersionSearchCriteria vc1 = new VersionSearchCriteria(c1);
        VersionSearchCriteria vc2 = new VersionSearchCriteria(c1);
        VersionSearchCriteria vc3 = new VersionSearchCriteria(c2);
        
        assertTrue(vc1.equals(vc1));
        assertTrue(vc1.equals(vc2));
        assertEquals(vc1.hashCode(), vc2.hashCode());
        
        assertFalse(vc1.equals(null));
        assertFalse(vc1.equals(new Object()));
        assertFalse(vc1.equals(vc3));
    }
}
