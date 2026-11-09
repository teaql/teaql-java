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

        Sample amount(Number value) { handleUpdate("amount", amount, value); amount = value; return this; }
        Sample bucket(String value) { handleUpdate("bucket", bucket, value); bucket = value; return this; }
        @Override public String typeName() { return "MemoryAmount"; }
        @Override public Object __internalGet(String property) {
            return switch (property) {
                case "amount" -> amount;
                case "bucket" -> bucket;
                default -> super.__internalGet(property);
            };
        }
    }

    private static final class Request extends BaseRequest<Sample> {
        Request() { super(Sample.class); internalComment("inspect memory aggregates"); purpose("verify empty aggregate semantics"); }
        @Override public String getTypeName() { return "MemoryAmount"; }
        Request only(String bucket) { appendSearchCriteria(createBasicSearchCriteria("bucket", Operator.EQUAL, bucket)); return this; }
        Request comment(String comment) { internalComment(comment); return this; }
        AggregationResult run(UserContext context) { return purpose("verify empty aggregate semantics").aggregation(context); }
    }

    private DefaultUserContext context;
    private MemoryDataService service;

    @Before public void setup() {
        var metadata = new SimpleEntityMetaFactory();
        var descriptor = new EntityDescriptor();
        descriptor.setType("MemoryAmount"); descriptor.setTargetType(Sample.class); descriptor.setDataService("memory");
        var properties = new ArrayList<PropertyDescriptor>();
        for (String name : List.of("id", "version", "amount", "bucket")) {
            var property = new PropertyDescriptor(); property.setName(name); property.setOwner(descriptor); properties.add(property);
        }
        descriptor.setProperties(properties); metadata.register(descriptor);
        service = new MemoryDataService("memory");
        var ids = new AtomicLong(100);
        context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).dataService("memory", service)
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
    @Test public void nonFiniteSumIsRejectedRatherThanSilentlyConvertedToNullOrZero() {
        seed("invalid", Double.POSITIVE_INFINITY);
        var error = assertThrows(TeaQLRuntimeException.class, () -> aggregate().run(context));
        assertTrue(error.getMessage().contains("finite"));
    }
    @Test public void missingIntentStillRejectsBeforeProviderAccess() {
        var request = aggregate().comment(" ");
        assertThrows(RuntimeException.class, () -> request.run(context));
    }
}
