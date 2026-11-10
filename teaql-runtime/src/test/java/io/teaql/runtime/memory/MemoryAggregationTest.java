package io.teaql.runtime.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Public runtime aggregation boundary; no SQL, provider result stub or generated-code edits. */
public class MemoryAggregationTest {
    public static final class Sample extends BaseEntity {
        private Number amount;
        private String bucket;
        private Sample parent;

        Sample amount(Number value) { handleUpdate("amount", amount, value); amount = value; return this; }
        Sample bucket(String value) { handleUpdate("bucket", bucket, value); bucket = value; return this; }
        Sample parent(Sample value) { handleUpdate("parent", parent, value); parent = value; return this; }
        @Override public String typeName() { return "MemoryAmount"; }
        @Override public Object __internalGet(String property) {
            return switch (property) {
                case "amount" -> amount;
                case "bucket" -> bucket;
                case "parent" -> parent;
                default -> super.__internalGet(property);
            };
        }
        @Override public void __internalSet(String property, Object value) {
            switch (property) {
                case "amount" -> amount = (Number) value;
                case "bucket" -> bucket = (String) value;
                case "parent" -> parent = (Sample) value;
                default -> super.__internalSet(property, value);
            }
        }
    }

    private static final class Request extends BaseRequest<Sample> {
        Request() { super(Sample.class, Sample::new); internalComment("inspect memory aggregates"); purpose("verify empty aggregate semantics"); }
        @Override public String getTypeName() { return "MemoryAmount"; }
        Request only(String bucket) { appendSearchCriteria(createBasicSearchCriteria("bucket", Operator.EQUAL, bucket)); return this; }
        Request comment(String comment) { internalComment(comment); return this; }
        AggregationResult run(UserContext context) { return purpose("verify empty aggregate semantics").aggregation(context); }
    }

    private DefaultUserContext context;
    private MemoryDataService service;
    private boolean denyNestedQuery;

    @Before public void setup() {
        var metadata = new SimpleEntityMetaFactory();
        var descriptor = new EntityDescriptor();
        descriptor.setType("MemoryAmount"); descriptor.setTargetType(Sample.class); descriptor.setDataService("memory");
        descriptor.setEntitySupplier(Sample::new);
        var properties = new ArrayList<PropertyDescriptor>();
        for (String name : List.of("id", "version", "amount", "bucket", "parent")) {
            var property = new PropertyDescriptor(); property.setName(name); property.setOwner(descriptor); properties.add(property);
        }
        descriptor.setProperties(properties); metadata.register(descriptor);
        service = new MemoryDataService("memory");
        var ids = new AtomicLong(100);
        context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("memory", service)
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext caller, SearchRequest<?> request) {
                        if (denyNestedQuery && QueryRequestScope.capture(request).parentRequest() != null)
                            throw new TeaQLRuntimeException("memory nested query denied by policy");
                    }
                })
                .idGenerationService((c, e) -> ids.getAndIncrement()).build());
    }

    private void seed(String bucket, Number... values) {
        for (Number value : values) new Sample().bucket(bucket).amount(value).auditAs("seed memory aggregate probe").save(context);
    }
    private Request aggregate() {
        var request = new Request();
        request.addAggregate("rows", "id", AggrFunction.COUNT);
        request.addAggregate("values", "amount", AggrFunction.COUNT);
        request.addAggregate("total", "amount", AggrFunction.SUM);
        return request;
    }
    private Map<String, Object> scalar(Request request) {
        var result = request.run(context);
        assertNotNull("memory aggregation must produce an AggregationResult", result);
        assertEquals(1, result.toList().size()); return result.toList().get(0);
    }
    private static void number(String expected, Object actual) {
        assertTrue("aggregate remains numeric: " + actual, actual instanceof Number);
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())));
    }

    @Test public void noRowsProducePresentNullSumAndZeroCounts() throws Exception {
        var values = scalar(aggregate()); number("0", values.get("rows")); number("0", values.get("values"));
        assertTrue(values.containsKey("total")); assertNull(values.get("total"));
        var json = new ObjectMapper().valueToTree(values); assertTrue(json.has("total")); assertTrue(json.get("total").isNull());
    }
    @Test public void allNullRowsKeepCountStarDistinctFromCountValue() {
        seed("nulls", (Number) null, null); var values = scalar(aggregate());
        number("2", values.get("rows")); number("0", values.get("values")); assertNull(values.get("total"));
    }
    @Test public void mixedNullsDoNotContributeToSumOrValueCount() {
        seed("mixed", null, 2, 3); var values = scalar(aggregate());
        number("3", values.get("rows")); number("2", values.get("values")); number("5", values.get("total"));
    }
    @Test public void cancellationProducesRealZeroRatherThanNull() {
        seed("zero", new BigDecimal("2.50"), new BigDecimal("-2.50")); number("0", scalar(aggregate()).get("total"));
    }
    @Test public void groupedInputWithoutGroupsProducesNoSyntheticRow() {
        var request = aggregate(); request.groupBy("bucket"); var result = request.run(context);
        assertNotNull(result); assertTrue(result.toList().isEmpty());
    }
    @Test public void groupedNullBucketAndIndependentTotalsAreRetained() {
        seed(null, 4); seed("a", null, 2); seed("b", (Number) null); var request = aggregate(); request.groupBy("bucket");
        var rows = request.run(context).toList(); assertEquals(3, rows.size());
        for (var row : rows) {
            if (row.get("bucket") == null) { number("4", row.get("total")); number("1", row.get("rows")); }
            else if ("a".equals(row.get("bucket"))) { number("2", row.get("total")); number("2", row.get("rows")); }
            else { assertEquals("b", row.get("bucket")); assertTrue(row.containsKey("total")); assertNull(row.get("total")); }
        }
    }
    @Test public void filteringHappensBeforeAggregationAndDoesNotMutateRequest() {
        seed("a", 2); seed("b", 100); var request = aggregate().only("a");
        var criteria = request.getSearchCriteria(); var expressions = new ArrayList<>(request.getAggregations().getAggregates());
        number("2", scalar(request).get("total")); number("2", scalar(request).get("total"));
        assertSame(criteria, request.getSearchCriteria()); assertEquals(expressions, request.getAggregations().getAggregates());
    }
    @Test public void explicitFallbackDoesNotChangeRawNullForAnotherConsumer() {
        var request = new Request(); request.addAggregate("total", "amount", AggrFunction.SUM);
        var result = request.run(context); assertNotNull(result); number("0", result.toNumber(0));
        assertNull(result.toNumber(null)); assertNull(scalar(request).get("total"));
    }
    @Test public void decimalAndLargeIntegerSumsDoNotPassThroughBinaryFloat() {
        seed("decimal", new BigDecimal("0.1"), new BigDecimal("0.2"));
        var decimal = scalar(aggregate().only("decimal")).get("total"); assertTrue(decimal instanceof BigDecimal); number("0.3", decimal);
        seed("integer", 9007199254740993L, 1L); number("9007199254740994", scalar(aggregate().only("integer")).get("total"));
    }
    @Test public void unsupportedAggregateIsRejectedEvenOnEmptyInput() {
        var request = new Request(); request.addAggregate("stddev", "amount", AggrFunction.STDDEV);
        var error = assertThrows(TeaQLRuntimeException.class, () -> request.run(context));
        assertTrue(error.getMessage().contains("STDDEV"));
    }
    @Test public void nonFiniteSeedIsRejectedBeforeItEntersMemoryStorage() {
        var error = assertThrows(io.teaql.core.checker.CheckException.class, () -> seed("invalid", Double.POSITIVE_INFINITY));
        assertTrue(error.getMessage().contains("finite"));
        assertTrue(new Request().purpose("verify rejected seed was not stored").executeForList(context).isEmpty());
    }
    @Test public void missingIntentStillRejectsBeforeProviderAccess() {
        var request = aggregate().comment(" ");
        assertThrows(RuntimeException.class, () -> request.run(context));
    }

    private Sample saved(Sample row) {
        row.auditAs("seed a memory graph bucket").save(context); return row;
    }
    private Request related(Request targets) {
        targets.addRelationCount("rows", new Request().only("child"), "parent", null);
        targets.addRelationScalarAggregate("total", new Request().only("child"), "parent", "amount", AggrFunction.SUM, null);
        return targets;
    }
    private SmartList<Sample> list(Request request) {
        return request.purpose("verify memory graph aggregate boundaries").executeForList(context);
    }
    @Test public void emptyRelatedValuesArePresentWithoutContaminatingStoredOrEarlierRows() {
        var parent = saved(new Sample().bucket("parent").amount(null).parent(null));
        var request = related(new Request().only("parent")); request.selectProperty("bucket");
        var childTemplate = request.getDynamicAggregateAttributes().get(0).getAggregateRequest();
        var originalCriteria = childTemplate.getSearchCriteria();
        var first = list(request).get(0);
        number("0", first.getDynamicProperty("rows")); assertTrue(first.hasDynamicProperty("total"));
        assertNull(first.getDynamicProperty("total")); assertFalse(first.isPropertyLoaded("amount"));
        assertNotSame(parent, first); assertFalse(parent.hasDynamicProperty("rows"));
        saved(new Sample().bucket("child").amount(7).parent(parent));
        var second = list(request).get(0);
        number("1", second.getDynamicProperty("rows")); number("7", second.getDynamicProperty("total"));
        number("0", first.getDynamicProperty("rows")); assertNull(first.getDynamicProperty("total"));
        assertSame(originalCriteria, childTemplate.getSearchCriteria());
        assertEquals(0, childTemplate.getAggregations().getDimensions().size());
        assertFalse(parent.hasDynamicProperty("total")); assertFalse(first.needPersist());
    }
    @Test public void facetsUseFullFilteredMembershipEvenWhenVisiblePageIsEmpty() {
        var a = saved(new Sample().bucket("parent").amount(null).parent(null));
        var b = saved(new Sample().bucket("parent").amount(null).parent(null));
        saved(new Sample().bucket("child").amount(null).parent(a));
        saved(new Sample().bucket("child").amount(2).parent(a));
        saved(new Sample().bucket("excluded").amount(100).parent(a));
        var targets = related(new Request().only("parent")); targets.selectProperty("bucket");
        var source = new Request().only("child"); source.offset(100, 1);
        source.addFacet("buckets", "parent", targets, true);
        var result = list(source); assertTrue(result.isEmpty());
        var facets = result.getFacet("buckets"); assertEquals(2, facets.size());
        for (Object value : facets) {
            var row = (Sample) value;
            number(row.getId().equals(a.getId()) ? "2" : "0", row.getDynamicProperty("count"));
            number(row.getId().equals(a.getId()) ? "2" : "0", row.getDynamicProperty("rows"));
            if (row.getId().equals(b.getId())) assertNull(row.getDynamicProperty("total"));
            else number("2", row.getDynamicProperty("total"));
        }
        source.getFacetRequests().get(0).setMergeCriteria(true);
        assertEquals(1, list(source).getFacet("buckets").size());
        assertFalse(a.hasDynamicProperty("count")); assertFalse(b.hasDynamicProperty("rows"));
    }
    @Test public void nestedMembershipRemainsRequestImmutableAndDoesNotExposeStoredDetails() {
        var a = saved(new Sample().bucket("parent").amount(null).parent(null));
        saved(new Sample().bucket("child").amount(9).parent(a));
        var allowed = new Request().only("parent"); allowed.offset(100, 1);
        var request = new Request().only("child");
        request.appendSearchCriteria(new SubQuerySearchCriteria("parent", allowed, "id"));
        var first = list(request); assertEquals(1, first.size()); assertEquals(1, list(request).size());
        assertTrue(allowed.getAggregations().getSelectedExpressions().isEmpty());
        assertEquals(100, allowed.getSlice().getOffset());
        var reference = (Sample) first.get(0).getProperty("parent");
        assertEquals(a.getId(), reference.getId()); assertFalse(reference.isPropertyLoaded("bucket"));
        assertNotSame(a, reference);
    }
    @Test public void relatedAggregatesCannotBypassTheNestedQueryPolicy() {
        var parent = saved(new Sample().bucket("parent").amount(null).parent(null));
        var request = related(new Request().only("parent"));
        denyNestedQuery = true;
        var error = assertThrows(TeaQLRuntimeException.class, () -> list(request));
        assertTrue(error.getMessage().contains("denied by policy"));
        assertFalse(parent.hasDynamicProperty("rows"));
        denyNestedQuery = false;
        number("0", list(request).get(0).getDynamicProperty("rows"));
    }
    @Test public void malformedRelatedAggregatesFailEvenWithoutAnyVisibleParents() {
        var request = new Request().only("absent");
        request.addSingleAggregateDynamicProperty("invalid", new Request());
        assertThrows(TeaQLRuntimeException.class, () -> list(request));
        assertTrue(request.getDynamicAggregateAttributes().get(0).getAggregateRequest().getAggregations().getSelectedExpressions().isEmpty());
    }
}
