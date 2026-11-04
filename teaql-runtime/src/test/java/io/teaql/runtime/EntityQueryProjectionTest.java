package io.teaql.runtime;

import io.teaql.core.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityQueryProjectionTest {
    private static class RecordingExecutor implements QueryExecutor, StreamingQueryExecutor {
        final List<SearchRequest<?>> requests = new ArrayList<>();
        boolean fail;
        int closed;
        private void record(QueryRequest request) {
            requests.add(((DefaultQueryRequest) request).getSearchRequest());
            if (fail) throw new IllegalStateException("provider failed");
        }
        @Override public QueryResult query(UserContext context, QueryRequest request) {
            record(request);
            return new DefaultQueryResult(new SmartList<>());
        }
        @Override public <T extends Entity> Stream<T> queryForStream(UserContext context, QueryRequest request) {
            record(request);
            return Stream.<T>empty().onClose(() -> closed++);
        }
        @Override public String name() { return "dummy"; }
        @Override public DataServiceCapabilities capabilities() { return null; }
    }

    private BaseRequest<TeaQLRuntimeTest.DummyEntity> request() {
        var request = new BaseRequest<TeaQLRuntimeTest.DummyEntity>(TeaQLRuntimeTest.DummyEntity.class) {
            {
                internalComment("load a sparse entity");
                internalPurpose("verify mandatory identity and version");
            }
            @Override public String getTypeName() { return "Dummy"; }
            @Override public SearchRequest<?> diagnosticSource() { return this; }
        };
        request.selectProperty("name");
        request.offset(0, 3);
        return request;
    }
    private TeaQLRuntime runtime(RecordingExecutor executor) {
        return TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", executor).build();
    }
    private List<String> fields(SearchRequest<?> request) {
        return request.getProjections().stream().map(SimpleNamedExpression::name).toList();
    }
    private void mandatory(SearchRequest<?> request) {
        assertEquals(3, fields(request).size());
        assertTrue(fields(request).containsAll(List.of("id", "version", "name")));
        assertFalse(fields(request).contains("active"));
    }

    @Test public void listRestoresTechnicalFieldsWithoutWideningOrMutatingCaller() {
        var executor = new RecordingExecutor(); var runtime = runtime(executor); var caller = request();
        runtime.executeForList(new DefaultUserContext(runtime), caller);
        assertEquals(1, executor.requests.size()); mandatory(executor.requests.get(0));
        assertEquals(List.of("name"), fields(caller));
        assertEquals(caller.comment(), executor.requests.get(0).comment());
        assertEquals(caller.purpose(), executor.requests.get(0).purpose());
        assertSame(caller, executor.requests.get(0).diagnosticSource());
    }

    @Test public void restoresOnlyMissingFieldAndDoesNotDuplicateExistingProjections() {
        for (String existing : List.of("id", "version")) {
            var executor = new RecordingExecutor(); var runtime = runtime(executor); var caller = request();
            caller.selectProperty(existing);
            runtime.executeForList(new DefaultUserContext(runtime), caller);
            mandatory(executor.requests.get(0));
            assertEquals(List.of("name", existing), fields(caller));
        }
    }

    @Test public void emptyProjectionRetainsProviderDefaultFullEntityLoading() {
        var executor = new RecordingExecutor(); var runtime = runtime(executor); var caller = request();
        caller.getProjections().clear();
        runtime.executeForList(new DefaultUserContext(runtime), caller);
        assertTrue(executor.requests.get(0).getProjections().isEmpty());
        assertTrue(caller.getProjections().isEmpty());
    }

    @Test public void policyCannotRemoveMandatoryEntityFields() {
        var executor = new RecordingExecutor();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", executor).queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> query) {
                        ((BaseRequest<?>) query).unselectProperty("id");
                        ((BaseRequest<?>) query).unselectProperty("version");
                    }
                }).build();
        var caller = request(); caller.selectProperty("id"); caller.selectProperty("version");
        runtime.executeForList(new DefaultUserContext(runtime), caller);
        mandatory(executor.requests.get(0));
    }

    @Test public void internalListPreservesScopeAndCompleteProjections() {
        var executor = new RecordingExecutor(); var runtime = runtime(executor); var caller = request();
        var root = QueryRequestScope.capture(caller);
        var scope = root.deriveChild("children", "load selected children");
        var nested = new QueryRequestSnapshot<>(caller, scope);
        runtime.internalExecuteForList(new DefaultUserContext(runtime), nested);
        mandatory(executor.requests.get(0));
        assertSame(scope, executor.requests.get(0).requestScope());
        assertSame(caller, executor.requests.get(0).diagnosticSource());
        assertEquals(List.of("name"), fields(nested));
        nested.selectProperty("id"); nested.selectProperty("version");
        runtime.internalExecuteForList(new DefaultUserContext(runtime), nested);
        assertEquals(fields(nested), fields(executor.requests.get(1)));
        assertSame(scope, executor.requests.get(1).requestScope());
    }

    @Test public void publicAndInternalStreamsRestoreFieldsAndCloseProviderStreams() {
        var executor = new RecordingExecutor(); var runtime = runtime(executor); var caller = request();
        var context = new DefaultUserContext(runtime);
        try (var stream = runtime.executeForStream(context, caller)) { assertEquals(0L, stream.count()); }
        var scope = QueryRequestScope.capture(caller).deriveChild("children", "load child stream");
        var nested = new QueryRequestSnapshot<>(caller, scope);
        try (var stream = runtime.internalExecuteForStream(context, nested)) { assertEquals(0L, stream.count()); }
        assertEquals(2, executor.closed);
        executor.requests.forEach(this::mandatory);
        assertSame(scope, executor.requests.get(1).requestScope());
        assertEquals(List.of("name"), fields(caller));
        assertEquals(List.of("name"), fields(nested));
    }

    @Test public void aggregateAndBothDimensionShapesKeepTheirProjection() {
        for (int shape = 0; shape < 3; shape++) {
            var executor = new RecordingExecutor(); var runtime = runtime(executor); var caller = request();
            var expression = new SimpleNamedExpression("_value", new PropertyReference("name"));
            var aggregations = caller.getAggregations();
            switch (shape) {
                case 0 -> aggregations.getAggregates().add(expression);
                case 1 -> aggregations.getSimpleDimensions().add(expression);
                case 2 -> aggregations.getComplexDimensions().add(expression);
            }
            runtime.executeForList(new DefaultUserContext(runtime), caller);
            assertEquals(List.of("name"), fields(executor.requests.get(0)));
        }
    }

    @Test public void providerFailureDoesNotChangeCallerProjection() {
        var executor = new RecordingExecutor(); executor.fail = true;
        var runtime = runtime(executor); var caller = request();
        assertThrows(IllegalStateException.class, () -> runtime.executeForList(new DefaultUserContext(runtime), caller));
        assertEquals(List.of("name"), fields(caller));
        mandatory(executor.requests.get(0));
    }
}
