package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Provider FK readback must not replace an already materialized graph node. */
public class ReviewedMutationGraphIdentityTest {
    public static final class Node extends BaseEntity {
        private final String type;
        private final Map<String,Object> values = new HashMap<>();
        Node(String type) { this.type = type; }
        @Override public String typeName() { return type; }
        @Override public Object __internalGet(String name) {
            return name.equals("id") || name.equals("version") ? super.__internalGet(name) : values.get(name);
        }
        @Override public void __internalSet(String name, Object value) {
            if (name.equals("id") || name.equals("version")) super.__internalSet(name, value);
            else { values.put(name, value); markPropertyLoaded(name); }
        }
    }
    @Test public void referenceReadbackRetainsTheCompleteChildForConsecutiveMutations() {
        var metadata = new SimpleEntityMetaFactory();
        for (String name : List.of("Parent", "Child")) {
            var descriptor = new EntityDescriptor(); descriptor.setType(name); descriptor.setDataService("fixture");
            descriptor.setEntitySupplier(() -> new Node(name));
            var fields = new ArrayList<PropertyDescriptor>();
            for (String field : List.of("id", "version", "name")) {
                var property = new PropertyDescriptor(); property.setName(field); fields.add(property);
            }
            if (name.equals("Parent")) { var relation = new Relation(); relation.setName("child"); fields.add(relation); }
            descriptor.setProperties(fields); metadata.register(descriptor);
        }
        var requests = new ArrayList<EntityPersistenceMutation>();
        var executor = new MutationExecutor() {
            public MutationResult mutate(UserContext caller, PersistenceMutation request) {
                var mutation = (EntityPersistenceMutation) request; requests.add(mutation);
                var entity = (BaseEntity) mutation.getEntity();
                entity.__internalSet("version", entity.getVersion() == null ? 1L : entity.getVersion() + 1);
                entity.set$status(EntityStatus.PERSISTED); return new DefaultMutationResult(entity);
            }
            public String name() { return "fixture"; }
            public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        };
        var runtime = TeaQLRuntime.builder().metadata(metadata).dataService("fixture", executor).build();
        var context = new DefaultUserContext(runtime);
        var parent = new Node("Parent"); parent.__internalSet("id", 1L);
        var child = new Node("Child"); child.__internalSet("id", 1L);
        // This low-level fixture explicitly performs the NEW lifecycle entry
        // normally supplied by generated Q.newEntity(), not merely hydration.
        parent.getEntityMutationLedger().markAsNew(new EntityKey("Parent", 1L));
        child.getEntityMutationLedger().markAsNew(new EntityKey("Child", 1L));
        child.updateProperty("name", "first"); parent.updateProperty("child", child);
        parent.auditAs("Create the complete identity graph").save(context);
        assertSame(child, parent.__internalGet("child"));
        assertEquals(Long.valueOf(1), child.getVersion()); assertEquals(Long.valueOf(1), parent.getVersion());
        assertEquals(2, requests.size());
        for (String next : List.of("second", "third")) {
            child.updateProperty("name", next);
            parent.auditAs("Save the next child mutation through the unchanged parent").save(context);
            assertSame(child, parent.__internalGet("child"));
            assertEquals(next, requests.get(requests.size() - 1).getEntity().getProperty("name"));
            assertEquals(Long.valueOf(1), parent.getVersion());
            assertTrue(parent.getEntityMutationLedger().currentChangeSet().changes().isEmpty());
        }
        assertEquals(4, requests.size()); assertEquals(Long.valueOf(3), child.getVersion());
    }
}
