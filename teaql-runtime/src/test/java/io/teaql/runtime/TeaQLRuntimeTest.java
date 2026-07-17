package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TeaQLRuntimeTest {

    public static class DummyEntity extends BaseEntity {
        private String name;

        @Override
        public String typeName() {
            return "Dummy";
        }

        @Override
        public void __internalSet(String property, Object value) {
            if ("name".equals(property)) {
                this.name = (String) value;
            } else {
                super.__internalSet(property, value);
            }
        }
        
        @Override
        public Object __internalGet(String property) {
            if ("name".equals(property)) {
                return this.name;
            } else {
                return super.__internalGet(property);
            }
        }
    }

    public static class DummyQueryExecutor implements QueryExecutor {
        @Override
        public QueryResult query(UserContext ctx, QueryRequest request) {
            SmartList<DummyEntity> list = new SmartList<>();
            list.add(new DummyEntity());
            return new DefaultQueryResult(list);
        }

        @Override
        public String name() {
            return "dummy";
        }

        @Override
        public DataServiceCapabilities capabilities() {
            return null;
        }
    }

    public static class RecordingMutationExecutor implements MutationExecutor {
        public final List<DefaultMutationRequest> requests = new ArrayList<>();

        @Override
        public MutationResult mutate(UserContext ctx, MutationRequest request) {
            if (request instanceof DefaultMutationRequest) {
                requests.add((DefaultMutationRequest) request);
            }
            return null;
        }

        @Override
        public String name() {
            return "dummy";
        }

        @Override
        public DataServiceCapabilities capabilities() {
            return null;
        }
    }

    public static class DummyMetaFactory implements EntityMetaFactory {
        @Override
        public EntityDescriptor resolveEntityDescriptor(String type) {
            EntityDescriptor desc = new EntityDescriptor();
            desc.setType(type);
            desc.setDataService("dummy");
            return desc;
        }

        @Override
        public void register(EntityDescriptor type) {}

        @Override
        public List<EntityDescriptor> allEntityDescriptors() { return null; }
    }

    @Test
    public void testBuilder() {
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .build();
        Assert.assertNotNull(runtime);
    }

    @Test
    public void testExecuteForList() {
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", new DummyQueryExecutor())
                .build();

        SearchRequest<DummyEntity> request = new BaseRequest<DummyEntity>(DummyEntity.class) {
            {
                internalComment("test");
                internalPurpose("test request");
            }
            @Override
            public String getTypeName() {
                return "Dummy";
            }
        };

        SmartList<DummyEntity> result = runtime.executeForList(new DefaultUserContext(runtime), request);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
    }

    public static class ContainerEntity extends BaseEntity {
        private DummyEntity rel1;
        private DummyEntity rel2;
        private DummyEntity rel3;
        private DummyEntity rel4;

        @Override
        public String typeName() {
            return "Container";
        }

        @Override
        public void __internalSet(String property, Object value) {
            switch (property) {
                case "rel1": this.rel1 = (DummyEntity) value; break;
                case "rel2": this.rel2 = (DummyEntity) value; break;
                case "rel3": this.rel3 = (DummyEntity) value; break;
                case "rel4": this.rel4 = (DummyEntity) value; break;
                default: super.__internalSet(property, value);
            }
        }

        @Override
        public Object __internalGet(String property) {
            switch (property) {
                case "rel1": return this.rel1;
                case "rel2": return this.rel2;
                case "rel3": return this.rel3;
                case "rel4": return this.rel4;
                default: return super.__internalGet(property);
            }
        }
    }

    public static class AdvancedMetaFactory implements EntityMetaFactory {
        @Override
        public EntityDescriptor resolveEntityDescriptor(String type) {
            EntityDescriptor desc = new EntityDescriptor();
            desc.setType(type);
            desc.setDataService("dummy");
            if ("Container".equals(type)) {
                desc.setEntitySupplier(() -> new ContainerEntity());
            } else {
                desc.setEntitySupplier(() -> new DummyEntity());
            }
            return desc;
        }

        @Override
        public void register(EntityDescriptor type) {}

        @Override
        public List<EntityDescriptor> allEntityDescriptors() { return null; }
    }

    @Test
    public void testSaveGraphLedgerClassificationAndExecutionOrder() throws Exception {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new AdvancedMetaFactory())
                .dataService("dummy", executor)
                .build();

        // Simulate related entities
        EntityRoot root = new EntityRoot();
        root.pushChangeSet();
        root.setComment("root comment");

        DummyEntity e1 = new DummyEntity(); // to delete
        e1.updateId(101L);
        e1.set$status(EntityStatus.PERSISTED);
        e1.setEntityRoot(root);
        e1.markToRemove();

        DummyEntity e2 = new DummyEntity(); // to update
        e2.updateId(102L);
        e2.set$status(EntityStatus.PERSISTED);
        e2.setEntityRoot(root);
        e2.updateProperty("name", "updated");

        DummyEntity e3 = new DummyEntity(); // to insert
        e3.updateId(103L);
        e3.set$status(EntityStatus.NEW);
        e3.setEntityRoot(root);
        e3.updateProperty("name", "inserted");
        root.markAsNew(new EntityKey(e3.typeName(), e3.getId()));

        DummyEntity e4 = new DummyEntity(); // to delete only
        e4.updateId(104L);
        e4.set$status(EntityStatus.PERSISTED);
        e4.setEntityRoot(root);
        e4.markToRemove();
        
        java.util.Map<EntityKey, BaseEntity> realEntities = new java.util.HashMap<>();
        realEntities.put(new EntityKey("Dummy", 101L), e1);
        realEntities.put(new EntityKey("Dummy", 102L), e2);
        realEntities.put(new EntityKey("Dummy", 103L), e3);
        realEntities.put(new EntityKey("Dummy", 104L), e4);

        java.lang.reflect.Method method = TeaQLRuntime.class.getDeclaredMethod(
            "executeLedgerPlan", UserContext.class, EntityRoot.class, MutationExecutor.class, java.util.Map.class);
        method.setAccessible(true);
        method.invoke(runtime, new DefaultUserContext(runtime), root, executor, realEntities);

        List<DefaultMutationRequest> requests = executor.requests;

        List<DefaultMutationRequest> deletes = new ArrayList<>();
        List<DefaultMutationRequest> saves = new ArrayList<>();

        for (DefaultMutationRequest req : requests) {
            if (req.getAction() == DefaultMutationRequest.Action.DELETE) {
                deletes.add(req);
            } else {
                saves.add(req);
            }
        }

        boolean seenSave = false;
        for (DefaultMutationRequest req : requests) {
            if (req.getAction() == DefaultMutationRequest.Action.SAVE) {
                seenSave = true;
            } else if (req.getAction() == DefaultMutationRequest.Action.DELETE) {
                Assert.assertFalse("DELETE should execute before SAVE", seenSave);
            }
        }

        Assert.assertTrue(deletes.stream().anyMatch(r -> r.getEntity().getId().equals(101L)));
        Assert.assertTrue(saves.stream().anyMatch(r -> r.getEntity().getId().equals(102L)));
        Assert.assertTrue(saves.stream().anyMatch(r -> r.getEntity().getId().equals(103L)));
        Assert.assertTrue(deletes.stream().anyMatch(r -> r.getEntity().getId().equals(104L))); // 104 was marked to remove, so it should be deleted.
        Assert.assertFalse(saves.stream().anyMatch(r -> r.getEntity().getId().equals(104L))); // 104 shouldn't be saved
        
        Assert.assertTrue(requests.stream().allMatch(r -> "root comment".equals(r.getEntity().getComment())));
        // The current change set is not cleared by executeLedgerPlan, it's cleared by saveGraph, so we can't test that here unless we do it.
        // I will omit that check or just check saveGraph does it.
    }

    @Test
    public void testDelete() {
        RecordingMutationExecutor executor = new RecordingMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .build();

        DummyEntity entity = new DummyEntity();
        entity.setComment("test delete");
        entity.set$status(EntityStatus.PERSISTED);
        runtime.delete(new DefaultUserContext(runtime), entity);

        Assert.assertFalse(executor.requests.isEmpty());
        Assert.assertEquals(DefaultMutationRequest.Action.DELETE, executor.requests.get(0).getAction());
    }
}
