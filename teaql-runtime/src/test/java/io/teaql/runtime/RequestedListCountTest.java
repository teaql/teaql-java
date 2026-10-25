package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestedListCountTest {
    private BaseRequest<TeaQLRuntimeTest.DummyEntity> request() {
        var request = new BaseRequest<TeaQLRuntimeTest.DummyEntity>(TeaQLRuntimeTest.DummyEntity.class) {
            { internalComment("load matching records"); internalPurpose("render bounded list"); }
            @Override public String getTypeName() { return "Dummy"; }
        };
        request.offset(2, 2); request.count();
        return request;
    }

    @Test public void ordinaryListOwnsRowsAndRequestedCountUnderOneBalancedQuery() {
        var executor = new TeaQLRuntimeTest.PageQueryExecutor();
        List<String> events = new ArrayList<>();
        RuntimeTelemetry telemetry = operation -> {
            events.add("start:" + operation.family());
            return new RuntimeTelemetry.Scope() {
                public void success(Map<String, Object> attributes) { events.add("success:" + operation.family()); }
                public void failure(Throwable error) { events.add("failure:" + operation.family()); }
            };
        };
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", executor).telemetry(telemetry)
                .queryPolicy(new QueryPolicy() {
                  @Override public void enforceSelect(UserContext context, SearchRequest<?> query) {
                    var current = (BaseRequest<?>) query;
                    current.appendSearchCriteria(current.createBasicSearchCriteria("status", Operator.EQUAL, "ACTIVE"));
                  }
                }).build();
        var source = request();
        var rows = runtime.executeForList(new DefaultUserContext(runtime), source);
        assertEquals(List.of(3L, 4L), rows.toList(Entity::getId));
        assertEquals(5, rows.getTotalCount()); assertEquals(2, executor.requests.size());
        assertFalse(executor.requests.get(0).hasSimpleAgg()); assertTrue(executor.requests.get(1).hasSimpleAgg());
        assertSame(executor.requests.get(0).getSearchCriteria(), executor.requests.get(1).getSearchCriteria());
        var chain = new DefaultQueryRequest(executor.requests.get(1)).businessTraceChain();
        assertEquals("count()", chain.get(chain.size() - 1).getComment());
        assertEquals(List.of("start:query", "start:provider", "success:provider", "success:query"), events);
        assertEquals("caller count configuration is not removed", 1, source.getAggregations().getAggregates().size());
        assertEquals(2, source.getSlice().getOffset()); assertEquals(2, source.getSlice().getSize());
    }

    @Test public void groupedCountRetainsAggregateSemanticsInsteadOfCreatingAnotherRowsQuery() {
        var executor = new TeaQLRuntimeTest.PageQueryExecutor();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).dataService("dummy", executor).build();
        var source = request();
        source.getAggregations().getSimpleDimensions().add(new SimpleNamedExpression("name", new PropertyReference("name")));
        runtime.aggregation(new DefaultUserContext(runtime), source);
        assertEquals(1, executor.requests.size()); assertTrue(executor.requests.get(0).hasSimpleAgg());
        assertEquals(1, executor.requests.get(0).getAggregations().getSimpleDimensions().size());
    }

    @Test public void scalarAliasesAndNullRemainMetadataAlongsideOptionalExactCount() {
        for (boolean counted : new boolean[]{false, true}) for (Object value : new Object[]{600L, null}) {
            var executor = new TeaQLRuntimeTest.PageQueryExecutor() {
                @Override public QueryResult query(UserContext context, QueryRequest query) {
                    SearchRequest<?> source = ((DefaultQueryRequest) query).getSearchRequest();
                    requests.add(source);
                    var values = new java.util.HashMap<SimpleNamedExpression, Object>();
                    for (var aggregate : source.getAggregations().getAggregates())
                        values.put(new SimpleNamedExpression(aggregate.name()),
                            ((AggrExpression) aggregate.getExpression()).getOperator() == AggrFunction.COUNT ? 3L : value);
                    var item = new AggregationItem(); item.setValues(values);
                    var result = new AggregationResult(); result.setData(List.of(item));
                    return new DefaultQueryResult(new SmartList<>(), result);
                }
            };
            var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).dataService("dummy", executor).build();
            var source = request();
            if (!counted) source.getAggregations().getAggregates().clear();
            source.sum("sum", "amount");
            var rows = runtime.executeForList(new DefaultUserContext(runtime), source);
            assertTrue(rows.isEmpty()); assertTrue(rows.aggregationProperties().containsKey("sum"));
            assertEquals(value, rows.aggregationProperties().get("sum"));
            assertEquals(counted ? Long.valueOf(3) : null, rows.getExactTotalCount());
            assertEquals(counted ? 2 : 1, executor.requests.size());
        }
    }

    @Test public void groupedProjectionIsRejectedAtTheEntityListBoundary() {
        var executor = new TeaQLRuntimeTest.PageQueryExecutor();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).dataService("dummy", executor).build();
        var source = request();
        source.getAggregations().getSimpleDimensions().add(new SimpleNamedExpression("name", new PropertyReference("name")));
        assertThrows(TeaQLRuntimeException.class, () -> runtime.executeForList(new DefaultUserContext(runtime), source));
        assertTrue(executor.requests.isEmpty());
    }

    @Test public void noSeparatePageExecutionEntryRemainsInCoreOrRuntime() {
        for (Class<?> type : List.of(ExecutableRequest.class, UserContext.class, DefaultUserContext.class, TeaQLRuntime.class)) {
            assertFalse(type.getName(), java.util.Arrays.stream(type.getMethods())
                    .anyMatch(method -> method.getName().equals("executeForPage")));
        }
    }

    @Test public void staleContextIdSetDiagnosticsCannotBecomeAnotherRequestsTotal() {
        var executor = new TeaQLRuntimeTest.PageQueryExecutor();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).dataService("dummy", executor).build();
        var context = new DefaultUserContext(runtime);
        context.putAttribute("teaql.idSet.countAccuracy", "EXACT");
        context.putAttribute("teaql.idSet.count", 999L);
        var source = request();
        source.optimizePaginationWithIdSet("another-query", 60, 100);
        assertEquals(5, runtime.executeForList(context, source).getTotalCount());
        assertEquals(2, executor.requests.size());
    }

    @Test public void listWithoutExplicitCountDoesNotAddAnAggregateQuery() {
        var executor = new TeaQLRuntimeTest.PageQueryExecutor();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).dataService("dummy", executor).build();
        var source = request();
        source.getAggregations().getAggregates().clear();
        runtime.executeForList(new DefaultUserContext(runtime), source);
        assertEquals(1, executor.requests.size());
        assertFalse(executor.requests.get(0).hasSimpleAgg());
    }

    @Test public void negativeOffsetIsRejectedBeforePolicyOrProvider() {
        var executor = new TeaQLRuntimeTest.PageQueryExecutor();
        int[] policies = {0};
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).dataService("dummy", executor)
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> request) { policies[0]++; }
                }).build();
        var source = request(); source.offset(-1, 2);
        assertThrows(IllegalArgumentException.class, () -> runtime.executeForList(new DefaultUserContext(runtime), source));
        assertEquals(0, policies[0]); assertTrue(executor.requests.isEmpty());
    }
}
