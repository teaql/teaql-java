package io.teaql.core.meta;

import io.teaql.core.Entity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class EntityDescriptorTest {


    @Test
    public void testEntityDescriptorDefaultsAndLookups() {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("User");
        
        PropertyDescriptor idProp = new PropertyDescriptor();
        idProp.setName("id");
        idProp.setOwner(descriptor);
        
        PropertyDescriptor nameProp = new PropertyDescriptor();
        nameProp.setName("name");
        nameProp.setOwner(descriptor);

        PropertyDescriptor versionProp = new PropertyDescriptor();
        versionProp.setName("version");
        versionProp.setOwner(descriptor);

        Relation ordersRel = new Relation();
        ordersRel.setName("orders");
        ordersRel.setOwner(descriptor);
        ordersRel.setRelationKeeper(descriptor);

        Relation categoryRel = new Relation();
        categoryRel.setName("category");
        categoryRel.setOwner(descriptor);
        // simulating a foreign relation where this entity is not the keeper
        categoryRel.setRelationKeeper(new EntityDescriptor());

        descriptor.setProperties(List.of(idProp, nameProp, versionProp, ordersRel, categoryRel));

        // Test findProperty
        assertEquals(nameProp, descriptor.findProperty("name"));
        assertNull(descriptor.findProperty("missing"));

        // Test getOwnProperties
        List<PropertyDescriptor> ownProps = descriptor.getOwnProperties();
        assertEquals(4, ownProps.size());
        assertTrue(ownProps.contains(idProp));
        assertTrue(ownProps.contains(nameProp));
        assertTrue(ownProps.contains(versionProp));
        assertTrue(ownProps.contains(ordersRel));
        assertFalse(ownProps.contains(categoryRel));

        // Test relations
        List<Relation> ownRels = descriptor.getOwnRelations();
        assertEquals(1, ownRels.size());
        assertTrue(ownRels.contains(ordersRel));

        List<Relation> foreignRels = descriptor.getForeignRelations();
        assertEquals(1, foreignRels.size());
        assertTrue(foreignRels.contains(categoryRel));

        // Test id and version
        assertEquals(idProp, descriptor.findIdProperty());
        assertEquals(versionProp, descriptor.findVersionProperty());

        // Test isRoot
        assertFalse(descriptor.isRoot());

        // If we clear relations
        EntityDescriptor emptyDescriptor = new EntityDescriptor();
        assertTrue(emptyDescriptor.isRoot());

        // Test isView and hasRepository
        descriptor.with(MetaConstants.VIEW_OBJECT, "true");
        assertTrue(descriptor.isView());
        assertFalse(descriptor.hasRepository());

        descriptor.with(MetaConstants.VIEW_OBJECT, "false");
        assertFalse(descriptor.isView());
        assertTrue(descriptor.hasRepository());
    }
}
