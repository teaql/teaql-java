package io.teaql.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class AuditedTest {

    static class DummyEntity extends BaseEntity {
        public DummyEntity() {
            updateId(1L);
            set$status(EntityStatus.PERSISTED);
        }
        @Override public String typeName() { return "dummy"; }
    }
    
    static class DummyContext implements UserContext {
        public Object savedGraph;
        
        @Override public void saveGraph(Object items) { this.savedGraph = items; }
        @Override public void saveGraph(Entity entity) { this.savedGraph = entity; }
        // remaining methods omitted for brevity as they just return null or do nothing
        @Override public void pushTrace(String comment) {}
        @Override public java.util.List<TraceNode> getTraceChain() { return null; }
        @Override public void popTrace() {}
        @Override public void recordExecutionMetadata(ExecutionMetadata metadata) {}
        @Override public <T extends Entity> T executeForOne(ExecutableRequest<T> request) { return null; }
        @Override public <T extends Entity> SmartList<T> executeForList(ExecutableRequest<T> request) { return null; }
        @Override public <T extends Entity> java.util.stream.Stream<T> executeForStream(ExecutableRequest<T> request) { return null; }
        @Override public <T extends Entity> java.util.stream.Stream<T> executeForStream(ExecutableRequest<T> request, int enhanceBatchSize) { return null; }
        @Override public <T extends Entity> AggregationResult aggregation(ExecutableRequest<T> request) { return null; }
        @Override public <T extends Entity> SmartList<T> internalExecuteForList(SearchRequest searchRequest) { return null; }
        @Override public <T extends Entity> T internalExecuteForOne(SearchRequest searchRequest) { return null; }
        @Override public <T extends Entity> java.util.stream.Stream<T> internalExecuteForStream(SearchRequest searchRequest) { return null; }
        @Override public <T extends Entity> java.util.stream.Stream<T> internalExecuteForStream(SearchRequest searchRequest, int enhanceBatchSize) { return null; }
        @Override public <T extends Entity> AggregationResult internalAggregation(SearchRequest request) { return null; }
        @Override public <T> T evaluate(String expression, Object... args) { return null; }
        @Override public Object getObj(String key, Object defaultValue) { return null; }
    }

    @Test
    public void testValidAudited() {
        DummyEntity entity = new DummyEntity();
        Audited<DummyEntity> audited = new Audited<>(entity, "test audit");
        
        assertEquals(entity, audited.entity());
        assertEquals("test audit", entity.getComment());
        
        DummyContext context = new DummyContext();
        
        // save
        DummyEntity saved = audited.save(context);
        assertEquals(entity, saved);
        assertEquals(entity, context.savedGraph);
        
        // delete
        context.savedGraph = null;
        entity.markForDeletion().auditAs("delete test").save(context);
        assertEquals(entity, context.savedGraph);
        // Deletion is persisted through the same audited save boundary.
        
        // recover
        context.savedGraph = null;
        entity.set$status(EntityStatus.PERSISTED_DELETED);
        DummyEntity recovered = audited.recover(context);
        assertEquals(entity, recovered);
        assertEquals(entity, context.savedGraph);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullComment() {
        new Audited<>(new DummyEntity(), null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyComment() {
        new Audited<>(new DummyEntity(), "   ");
    }
}
