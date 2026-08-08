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

    @Test
    public void testEqualsAndHashCode() {
        EntityDescriptor d1 = new EntityDescriptor();
        d1.setType("type1");
        
        EntityDescriptor d2 = new EntityDescriptor();
        d2.setType("type1");
        
        EntityDescriptor d3 = new EntityDescriptor();
        d3.setType("type2");
        
        assertEquals(d1, d1);
        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
        assertNotEquals(d1, null);
        assertNotEquals(d1, new Object());
        
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    public void testCreateEntity() {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("User");
        
        assertThrows(IllegalStateException.class, descriptor::createEntity);
        
        Entity dummy = new io.teaql.core.BaseEntity() {
            @Override public String typeName() { return "User"; }
        };
        
        descriptor.withEntitySupplier(() -> dummy);
        assertEquals(dummy, descriptor.createEntity());
    }

    @Test
    public void testMiscMethods() {
        EntityDescriptor descriptor = new EntityDescriptor();
        assertNull(descriptor.findProperty("foo")); // empty properties
        
        descriptor.with("constant", "true");
        assertTrue(descriptor.isConstant());
        
        List<String> def = List.of("a");
        assertEquals(def, descriptor.getList("missing", def));
        
        descriptor.with("list", "a,b,c");
        assertEquals(List.of("a", "b", "c"), descriptor.getList("list", def));
    }

    @Test
    public void testAddSimpleProperty() {
        EntityDescriptor descriptor = new EntityDescriptor();
        
        PropertyDescriptor p1 = descriptor.addSimpleProperty("prop1", String.class);
        assertEquals("prop1", p1.getName());
        assertEquals(String.class, p1.getType().javaType());
        
        PropertyDescriptor p2 = descriptor.addSimpleProperty("prop2", Integer.class, PropertyDescriptor.class);
        assertEquals("prop2", p2.getName());
        
        assertThrows(UnsupportedOperationException.class, () -> {
            descriptor.addSimpleProperty("prop3", String.class, Relation.class);
        });
        
        PropertyDescriptor p3 = descriptor.addSimpleProperty("prop4", Boolean.class, PropertyDescriptor::new);
        assertEquals("prop4", p3.getName());
        
        // add one identifier to test getIdentifier
        p3.with("identifier", "true");
        assertEquals(p3, descriptor.getIdentifier());
    }

    @Test
    public void testAddObjectProperty() {
        SimpleEntityMetaFactory factory = new SimpleEntityMetaFactory();
        
        EntityDescriptor parent = new EntityDescriptor();
        parent.setType("Parent");
        factory.register(parent);
        
        EntityDescriptor child = new EntityDescriptor();
        child.setType("Child");
        factory.register(child);
        
        Relation r1 = child.addObjectProperty(factory, "parent", "Parent", "children", io.teaql.core.BaseEntity.class);
        assertEquals("parent", r1.getName());
        assertEquals(child, r1.getOwner());
        assertEquals(child, r1.getRelationKeeper());
        
        Relation reverse = (Relation) r1.getReverseProperty();
        assertNotNull(reverse);
        assertEquals("children", reverse.getName());
        assertEquals(parent, reverse.getOwner());
        assertEquals(child, reverse.getRelationKeeper());
        
        // test other variants
        Relation r2 = child.addObjectProperty(factory, "p2", "Parent", "c2", io.teaql.core.BaseEntity.class, Relation.class);
        assertEquals("p2", r2.getName());
        
        assertThrows(UnsupportedOperationException.class, () -> {
            child.addObjectProperty(factory, "p3", "Parent", "c3", io.teaql.core.BaseEntity.class, (Class<? extends Relation>) (Class) PropertyDescriptor.class);
        });
        
        Relation r3 = child.addObjectProperty(factory, "p4", "Parent", "c4", io.teaql.core.BaseEntity.class, Relation::new);
        assertEquals("p4", r3.getName());
    }

    @Test
    public void testMissingBranches() {
        EntityDescriptor d = new EntityDescriptor();
        
        // getStr when additionalInfo is null
        assertEquals("def", d.getStr("k", "def"));
        
        // isView when viewObject is null
        assertFalse(d.isView());
        
        // getIdentifier when no identifier exists
        assertNull(d.getIdentifier());
        
        // hasChildren when empty
        assertFalse(d.hasChildren());
        
        // setParent and hasChildren
        EntityDescriptor parent = new EntityDescriptor();
        d.setParent(parent);
        assertTrue(parent.hasChildren());
        assertEquals(1, parent.getChildren().size());
        
        d.setParent(null);
        
        // isRoot when getParent() == null but has own relations
        EntityDescriptor d2 = new EntityDescriptor();
        Relation r = new Relation();
        r.setRelationKeeper(d2);
        d2.setProperties(List.of(r));
        assertFalse(d2.isRoot());
    }
}
