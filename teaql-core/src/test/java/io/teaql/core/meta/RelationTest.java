package io.teaql.core.meta;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RelationTest {

    @Test
    public void testGetAdditionalInfo() {
        EntityDescriptor owner1 = new EntityDescriptor();
        owner1.setType("Owner1");
        owner1.with("k1", "v1");

        EntityDescriptor owner2 = new EntityDescriptor();
        owner2.setType("Owner2");
        owner2.with("k2", "v2");

        Relation rel = new Relation();
        rel.setOwner(owner1);
        rel.with("relK", "relV");

        // Case 1: relationKeeper != owner
        rel.setRelationKeeper(owner2);
        Map<String, String> info1 = rel.getAdditionalInfo();
        assertEquals("relV", info1.get("relK"));
        assertEquals(null, info1.get("k2"));

        // Case 2: relationKeeper == owner
        rel.setRelationKeeper(owner1);
        PropertyDescriptor reverseProp = new PropertyDescriptor();
        reverseProp.setOwner(owner2);
        rel.setReverseProperty(reverseProp);
        
        Map<String, String> info2 = rel.getAdditionalInfo();
        assertEquals("relV", info2.get("relK"));
        assertEquals("v2", info2.get("k2"));
        assertNotNull(info2);
    }
}
