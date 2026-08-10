package io.teaql.core.meta;

import org.junit.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PropertyDescriptorTest {

    static PropertyType STRING = () -> String.class;
    static PropertyType LONG = () -> Long.class;

    @Test
    public void testConstructorsAndGettersSetters() {
        PropertyDescriptor pd = new PropertyDescriptor("prop1", STRING);
        assertEquals("prop1", pd.getName());
        assertEquals(STRING, pd.getType());
        
        EntityDescriptor owner = new EntityDescriptor();
        owner.setType("owner");
        pd.setOwner(owner);
        assertEquals(owner, pd.getOwner());
        
        pd.setName("id");
        pd.setType(LONG);
        assertEquals("id", pd.getName());
        assertEquals(LONG, pd.getType());
        assertTrue(pd.isId());
        assertFalse(pd.isVersion());
        
        pd.setName("version");
        assertTrue(pd.isVersion());
        assertFalse(pd.isId());
    }

    @Test
    public void testAdditionalInfo() {
        PropertyDescriptor pd = new PropertyDescriptor("prop1", STRING);
        
        pd.with("key1", "value1").with("identifier", "true");
        
        assertEquals("value1", pd.getAdditionalInfo().get("key1"));
        assertEquals("value1", pd.getSelfAdditionalInfo().get("key1"));
        
        assertTrue(pd.isIdentifier());
        
        pd.with("identifier", "false");
        assertFalse(pd.isIdentifier());
        
        pd.with("candidates", "a, b ,c");
        List<String> candidates = pd.getCandidates();
        assertEquals(3, candidates.size());
        assertEquals("a", candidates.get(0));
        assertEquals("b", candidates.get(1));
        assertEquals("c", candidates.get(2));
        
        pd.with("candidates", null); // this might trigger the null logic or not depending on map implementation
        Map<String, String> map = new HashMap<>();
        map.put("key2", "value2");
        pd.setAdditionalInfo(map);
        assertEquals("value2", pd.getAdditionalInfo().get("key2"));
        
        // test candidates when null
        assertTrue(pd.getCandidates().isEmpty());
    }
    
    @Test
    public void testGetStrAndGetBoolean() {
        PropertyDescriptor pd = new PropertyDescriptor("prop1", STRING);
        pd.with("boolKey", "true");
        pd.with("strKey", "strValue");
        
        assertEquals("strValue", pd.getStr("strKey", "default"));
        assertEquals("default", pd.getStr("missingKey", "default"));
        
        assertTrue(pd.getBoolean("boolKey", false));
        assertFalse(pd.getBoolean("missingKey", false));
        assertTrue(pd.getBoolean("missingKey", true));
        
        pd.setAdditionalInfo(null);
        assertEquals("default", pd.getStr("any", "default"));
        assertFalse(pd.getBoolean("any", false));
    }
}
