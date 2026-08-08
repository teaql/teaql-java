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
        @Override public void put(String key, Object value) {}
        @Override public <T> T evaluate(String expression, Object... args) { return null; }
        @Override public Object getObj(String key, Object defaultValue) { return null; }
    }

    @Test
    public void testDefaultMethods() {
        DummyUserContext ctx = new DummyUserContext() {};
        assertNull(ctx.extension("any"));
        assertNull(ctx.capability(String.class));
    }

    @Test(expected = TeaQLRuntimeException.class)
    public void testDynamicFieldsWithoutCapability() {
        DummyUserContext ctx = new DummyUserContext() {};
        ctx.dynamicFields();
    }

    @Test(expected = TeaQLRuntimeException.class)
    public void testGenerateBusinessIdWithoutCapability() {
        DummyUserContext ctx = new DummyUserContext() {};
        ctx.generateBusinessId(null, null, null);
    }
    
    @Test
    public void testGenerateBusinessIdWithCapability() {
        BusinessIdGenerator generator = new BusinessIdGenerator() {
            @Override
            public String generateBusinessId(UserContext ctx, Entity entity, EntityDescriptor entityDesc, PropertyDescriptor propertyDesc) {
                return "BID-123";
            }
        };
        
        DummyUserContext ctx = new DummyUserContext() {
            @Override
            public <T> T capability(Class<T> capabilityType) {
                if (capabilityType == BusinessIdGenerator.class) {
                    return (T) generator;
                }
                return null;
            }
        };
        
        String bid = ctx.generateBusinessId(null, null, null);
        assertEquals("BID-123", bid);
    }
}
