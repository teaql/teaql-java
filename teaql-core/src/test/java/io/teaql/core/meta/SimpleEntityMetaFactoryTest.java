package io.teaql.core.meta;

import io.teaql.core.TeaQLRuntimeException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SimpleEntityMetaFactoryTest {

    @Test
    public void testRegisterAndResolve() {
        SimpleEntityMetaFactory factory = new SimpleEntityMetaFactory();
        
        EntityDescriptor d1 = new EntityDescriptor(); d1.setType("type1");
        factory.register(d1);
        
        EntityDescriptor resolved = factory.resolveEntityDescriptor("type1");
        assertEquals(d1, resolved);
        
        // register null should not throw
        factory.register(null);
    }

    @Test(expected = TeaQLRuntimeException.class)
    public void testResolveNotFound() {
        SimpleEntityMetaFactory factory = new SimpleEntityMetaFactory();
        factory.resolveEntityDescriptor("unknown");
    }
    
    @Test
    public void testAllEntityDescriptors() {
        SimpleEntityMetaFactory factory = new SimpleEntityMetaFactory();
        EntityDescriptor d1 = new EntityDescriptor(); d1.setType("type1");
        EntityDescriptor d2 = new EntityDescriptor(); d2.setType("type2");
        
        factory.register(d1);
        factory.register(d2);
        
        List<EntityDescriptor> all = factory.allEntityDescriptors();
        assertEquals(2, all.size());
        assertTrue(all.contains(d1));
        assertTrue(all.contains(d2));
    }
}
