package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Entity-shaped execution must prepare statistical predicates just like aggregation. */
public class StatisticalMembershipPreparationTest {
    private static class Request extends BaseRequest<TeaQLRuntimeTest.DummyEntity> {
        Request(String comment, String purpose) {
            super(TeaQLRuntimeTest.DummyEntity.class); internalComment(comment); internalPurpose(purpose);
            selectProperty("id"); selectProperty("version"); offset(0, 5);
        }
        @Override public String getTypeName() { return "Dummy"; }
    }
    private static class Provider implements QueryExecutor, StreamingQueryExecutor {
        final List<SearchRequest<?>> requests = new ArrayList<>();
        @Override public QueryResult query(UserContext context, QueryRequest request) {
            requests.add(((DefaultQueryRequest)request).getSearchRequest()); return new DefaultQueryResult(new SmartList<>());
        }
        @Override public <T extends Entity> Stream<T> queryForStream(UserContext context, QueryRequest request) {
            requests.add(((DefaultQueryRequest)request).getSearchRequest()); return Stream.empty();
        }
        @Override public String name() { return "dummy"; }
        @Override public DataServiceCapabilities capabilities() { return null; }
    }
    private void verify(boolean stream, boolean internal) {
        var provider = new Provider(); var reviewed = new ArrayList<SearchRequest<?>>();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).dataService("dummy", provider)
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> request) {
                        assertTrue("Policies keep the native typed builder", request instanceof Request); reviewed.add(request);
                    }
                }).build();
        var rootIntent = QueryIntent.of("load root", "render bounded list");
        var root = internal ? new Request(null, null) {
            @Override public QueryIntent inheritedQueryIntent() { return rootIntent; }
        } : new Request(rootIntent.comment(), rootIntent.purpose());
        var child = statisticalChild();
        root.appendSearchCriteria(SearchCriteria.or(new SubQuerySearchCriteria("id", child, "id"),
                root.createBasicSearchCriteria("name", Operator.EQUAL, "fallback")));
        var context = new DefaultUserContext(runtime);
        if (stream) try (var values = internal ? runtime.internalExecuteForStream(context, root)
                : runtime.executeForStream(context, root)) { assertEquals(0, values.count()); }
        else if (internal) runtime.internalExecuteForList(context, root); else runtime.executeForList(context, root);
        assertEquals(List.of(root, child), reviewed); assertEquals(1, provider.requests.size());
        var prepared = provider.requests.get(0); var booleanGroup = (FunctionApply)prepared.getSearchCriteria();
        assertEquals(2, booleanGroup.getExpressions().size());
        var membership = (SubQuerySearchCriteria)booleanGroup.first(); var subquery = membership.getDependsOn();
        assertNotNull(subquery.requestScope()); assertSame(prepared.requestScope(), subquery.requestScope().rootRequest());
        assertEquals("eligible totals", subquery.requestScope().comment());
        assertEquals("render bounded list", subquery.requestScope().purpose());
        assertEquals(QueryOperation.Kind.AGGREGATE, subquery.requestScope().parentRequest().operation().kind());
        assertNotNull(subquery.getHaving()); assertEquals(1, subquery.getSlice().getSize());
        assertNull("Reusable native builders must not acquire execution headers", child.requestScope());
        assertNotSame(child, subquery);
    }
    private static Request statisticalChild() {
        var child = new Request("eligible totals", null);
        child.getAggregations().getSimpleDimensions().add(new SimpleNamedExpression("id", new PropertyReference("id")));
        child.count("n"); child.having("n", Operator.GREATER_THAN, 0L); child.offset(0, 1); return child;
    }
    @Test public void publicListPreparesStatisticalMembership() { verify(false, false); }
    @Test public void publicStreamPreparesStatisticalMembership() { verify(true, false); }
    @Test public void internalListPreparesStatisticalMembership() { verify(false, true); }
    @Test public void internalStreamPreparesStatisticalMembership() { verify(true, true); }
    @Test public void childPolicyRejectionStopsBeforeProviderExecution() {
        var provider = new Provider(); var child = statisticalChild();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).dataService("dummy", provider)
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> request) {
                        if (request == child) throw new TeaQLRuntimeException("child policy rejected");
                    }
                }).build();
        var root = new Request("load root", "verify rejection");
        root.appendSearchCriteria(new SubQuerySearchCriteria("id", child, "id"));
        var error = assertThrows(TeaQLRuntimeException.class, () -> runtime.executeForList(new DefaultUserContext(runtime), root));
        assertEquals("child policy rejected", error.getMessage()); assertTrue(provider.requests.isEmpty());
    }
}
