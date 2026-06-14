package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TeaQLRuntimeTest {

    public static class DummyEntity extends BaseEntity {
        @Override
        public String typeName() {
            return "Dummy";
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

    public static class DummyMutationExecutor implements MutationExecutor {
        public boolean called = false;

        @Override
        public MutationResult mutate(UserContext ctx, MutationRequest request) {
            called = true;
            return null; // Mock return
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

    @Test
    public void testSaveGraph() {
        DummyMutationExecutor executor = new DummyMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .build();

        DummyEntity entity = new DummyEntity();
        entity.setComment("test save");
        runtime.saveGraph(new DefaultUserContext(runtime), entity);
        
        Assert.assertTrue(executor.called);
    }

    @Test
    public void testDelete() {
        DummyMutationExecutor executor = new DummyMutationExecutor();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new DummyMetaFactory())
                .dataService("dummy", executor)
                .build();

        DummyEntity entity = new DummyEntity();
        entity.setComment("test delete");
        entity.set$status(EntityStatus.PERSISTED);
        runtime.delete(new DefaultUserContext(runtime), entity);
        
        Assert.assertTrue(executor.called);
    }
}
