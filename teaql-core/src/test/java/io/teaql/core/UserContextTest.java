package io.teaql.core;

import io.teaql.data.dynamic.DynamicFieldsFacade;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class UserContextTest {

    abstract static class DummyUserContext implements UserContext {
        @Override public void pushTrace(String comment) {}
        @Override public List<TraceNode> getTraceChain() { return null; }
        @Override public void popTrace() {}
        @Override public void recordExecutionMetadata(ExecutionMetadata metadata) {}
        @Override public <T extends Entity> T executeForOne(ExecutableRequest<T> request) { return null; }
        @Override public <T extends Entity> SmartList<T> executeForList(ExecutableRequest<T> request) { return null; }
        @Override public <T extends Entity> Stream<T> executeForStream(ExecutableRequest<T> request) { return null; }
        @Override public <T extends Entity> Stream<T> executeForStream(ExecutableRequest<T> request, int enhanceBatchSize) { return null; }
        @Override public <T extends Entity> AggregationResult aggregation(ExecutableRequest<T> request) { return null; }
        @Override public <T extends Entity> SmartList<T> internalExecuteForList(SearchRequest searchRequest) { return null; }
        @Override public <T extends Entity> T internalExecuteForOne(SearchRequest searchRequest) { return null; }
        @Override public <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest) { return null; }
        @Override public <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest, int enhanceBatchSize) { return null; }
        @Override public <T extends Entity> AggregationResult internalAggregation(SearchRequest request) { return null; }
        @Override public void saveGraph(Object items) {}
        @Override public void saveGraph(Entity entity) {}
        @Override public void delete(Entity pEntity) {}
        @Override public <T> T evaluate(String expression, Object... args) { return null; }
        @Override public Object getObj(String key, Object defaultValue) { return null; }
    }

    @Test
    public void testDefaultMethods() {
        DummyUserContext context = new DummyUserContext() {};
        assertNull(context.extension("any"));
        assertNull(context.capability(String.class));
    }

    @Test
    public void localCacheIsSharedAndHonorsTtl() throws Exception {
        DummyUserContext first = new DummyUserContext() {};
        DummyUserContext second = new DummyUserContext() {};
        String key = "local-cache-" + System.nanoTime();

        first.putToLocalCache(key, "value");
        assertEquals("value", second.getFromLocalCache(key, String.class));
        second.removeFromLocalCache(key);
        assertNull(first.getFromLocalCache(key, String.class));

        first.putToLocalCache(key, "temporary", 1);
        Thread.sleep(1100);
        assertNull(second.getFromLocalCache(key, String.class));
    }

    @Test
    public void localLockEnforcesOwnershipTimeoutAndLeaseExpiry() throws Exception {
        DummyUserContext first = new DummyUserContext() {};
        DummyUserContext second = new DummyUserContext() {};
        String key = "local-lock-" + System.nanoTime();

        assertTrue(first.tryLocalLock(key, 0, 50));
        assertFalse(second.tryLocalLock(key, 0, 50));
        second.unlockLocal(key);
        assertFalse(second.tryLocalLock(key, 0, 50));
        Thread.sleep(60);
        assertTrue(second.tryLocalLock(key, 0, 50));
        second.unlockLocal(key);
        assertTrue(first.tryLocalLock(key, 0, 50));
        first.unlockLocal(key);
    }

    @Test(expected = TeaQLRuntimeException.class)
    public void testDynamicFieldsWithoutCapability() {
        DummyUserContext context = new DummyUserContext() {};
        context.dynamicFields();
    }

    @Test(expected = TeaQLRuntimeException.class)
    public void testGenerateBusinessIdWithoutCapability() {
        DummyUserContext context = new DummyUserContext() {};
        context.generateBusinessId(null, null, null);
    }
    
    @Test
    public void testGenerateBusinessIdWithCapability() {
        BusinessIdGenerator generator = new BusinessIdGenerator() {
            @Override
            public String generateBusinessId(UserContext context, Entity entity, EntityDescriptor entityDesc, PropertyDescriptor propertyDesc) {
                return "BID-123";
            }
        };
        
        DummyUserContext context = new DummyUserContext() {
            @Override
            public <T> T capability(Class<T> capabilityType) {
                if (capabilityType == BusinessIdGenerator.class) {
                    return (T) generator;
                }
                return null;
            }
        };
        
        String bid = context.generateBusinessId(null, null, null);
        assertEquals("BID-123", bid);
    }
}
