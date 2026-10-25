package io.teaql.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import org.junit.Test;
import static org.junit.Assert.*;

public class SmartListSemanticsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static JsonNode vectors() throws Exception {
        try (var source = SmartListSemanticsTest.class.getResourceAsStream("/smart-list-semantics.json")) {
            return JSON.readTree(source);
        }
    }

    private static void aggregate(SmartList<?> list, String alias, Object value) {
        AggregationItem item = new AggregationItem();
        item.addValue(new SimpleNamedExpression(alias), value);
        AggregationResult result = new AggregationResult();
        result.setData(new ArrayList<>(List.of(item)));
        list.addAggregationResult(null, result);
    }

    @Test public void loadedUnknownZeroAndPageAreDistinct() throws Exception {
        for (JsonNode state : vectors().get("states")) {
            List<Integer> data = JSON.convertValue(state.get("rows"), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            SmartList<Integer> list = state.get("loaded").asBoolean() ? new SmartList<>(data) : SmartList.unloaded();
            if (!state.get("total").isNull()) aggregate(list, TeaQLConstants.ROOT_LIST_PARAMETER_NAME, state.get("total").longValue());
            assertEquals(state.get("loaded").asBoolean(), list.isLoaded());
            assertEquals(data, list.getData());
            Long expected = state.get("total").isNull() ? null : state.get("total").longValue();
            assertEquals(expected, list.getExactTotalCount());
            assertEquals(expected == null ? data.size() : expected.longValue(), list.getTotalCountOrSize());
            assertEquals(expected, list.getExactTotalCount());
        }
        assertTrue(SmartList.empty(SmartListTest.DummyEntity.class).isLoaded());
        assertFalse(SmartList.unloaded().map(Function.identity()).isLoaded());
        assertFalse(SmartList.unloaded().filter(value -> true).isLoaded());
        assertThrows(UnsupportedOperationException.class, () -> SmartList.unloaded().add(1));
    }

    @Test public void invalidAndWideCountsAreNeverTruncated() {
        for (Object value : Arrays.asList(-1, 1.5, Double.NaN, Double.POSITIVE_INFINITY, "3", null,
                new java.math.BigInteger("9223372036854775808"))) {
            SmartList<Object> list = new SmartList<>();
            aggregate(list, TeaQLConstants.ROOT_LIST_PARAMETER_NAME, value);
            assertThrows("invalid exact count: " + value, IllegalStateException.class, list::getExactTotalCount);
        }
        SmartList<Object> list = new SmartList<>();
        aggregate(list, TeaQLConstants.ROOT_LIST_PARAMETER_NAME, 4_294_967_296L);
        assertEquals(Long.valueOf(4_294_967_296L), list.getExactTotalCount());
        assertEquals(4_294_967_296L, list.getTotalCountOrSize());
    }

    @Test public void scalarAliasesPreserveNullAndUnrelatedAliasesDoNotBecomeTotals() {
        SmartList<Object> list = new SmartList<>();
        aggregate(list, "nullable", null);
        aggregate(list, "unrelated", 15);
        assertTrue(list.aggregationProperties().containsKey("nullable"));
        assertNull(list.aggregationProperties().get("nullable"));
        assertFalse(list.aggregationProperties().containsKey("missing"));
        assertEquals(Map.of("unrelated", 15), list.aggregationNumberProperties());
        assertNull(list.getExactTotalCount());
    }

    @Test @SuppressWarnings("unchecked") public void mapIsolatesNestedMetadataAndRecordFacets() throws Exception {
        SmartList<Integer> source = new SmartList<>(List.of(1, 2, 3));
        aggregate(source, TeaQLConstants.ROOT_LIST_PARAMETER_NAME, 9);
        aggregate(source, "nested", new LinkedHashMap<>(Map.of("values", new ArrayList<>(List.of(1, 2)))));
        BigDecimal decimal = new BigDecimal(vectors().path("metadata").path("decimal").path("value").asText());
        aggregate(source, "decimal", decimal);
        source.setSummary(Map.of("nested", Map.of("values", List.of(3))));
        source.addFacet("empty", new SmartList<>());
        source.addFacet("status", new SmartList<>(List.of(new LinkedHashMap<>(Map.of("status", "paid", "count", 3)))));
        SmartList<Integer> mapped = source.map(value -> value * 10);
        assertEquals(List.of(10, 20, 30), mapped.getData());
        assertEquals(Long.valueOf(9), mapped.getExactTotalCount());
        assertEquals(source.aggregationProperties(), mapped.aggregationProperties());
        assertEquals(source.getSummary(), mapped.getSummary());
        assertEquals(decimal, mapped.aggregationProperties().get("decimal"));
        ((List<Integer>) ((Map<?, ?>) mapped.aggregationProperties().get("nested")).get("values")).add(99);
        ((Map<String, Object>) mapped.getFacet("status").first()).put("count", 99);
        mapped.removeFacet("empty");
        mapped.setSummary(Map.of("changed", true));
        assertEquals(List.of(1, 2), ((Map<?, ?>) source.aggregationProperties().get("nested")).get("values"));
        assertEquals(3, ((Map<?, ?>) source.getFacet("status").first()).get("count"));
        assertNotNull(source.getFacet("empty"));
        assertFalse(source.getSummary().containsKey("changed"));
        for (SmartList<?> derived : List.of(source.filter(value -> value % 2 == 1),
                source.filter(value -> true), source.flatMap(value -> List.of(value, value)))) {
            assertTrue(derived.isLoaded()); assertNull(derived.getExactTotalCount());
            assertTrue(derived.getFacets().isEmpty()); assertTrue(derived.getSummary().isEmpty());
            assertTrue(derived.getAggregationResults().isEmpty());
        }
        assertEquals(List.of(1, 3), source.filter(value -> value % 2 == 1).getData());
        assertEquals(List.of(1, 1, 2, 2, 3, 3), source.flatMap(value -> List.of(value, value)).getData());
        List<Integer> nativeCopy = source.toList(Function.identity()); nativeCopy.set(0, 99);
        assertEquals(Integer.valueOf(1), source.first());
    }

    @Test public void duplicateMergeOrderIndexAndGroupsFollowVectors() throws Exception {
        JsonNode merge = vectors().path("merge");
        List<JsonNode> target = new ArrayList<>(), incoming = new ArrayList<>(), expected = new ArrayList<>();
        merge.path("target").forEach(target::add); merge.path("incoming").forEach(incoming::add);
        merge.path("expected").forEach(expected::add);
        SmartList<JsonNode> list = new SmartList<>(target);
        aggregate(list, TeaQLConstants.ROOT_LIST_PARAMETER_NAME, 9);
        list.mergeBy(incoming, row -> row.path("key").asText());
        assertEquals(expected, list.getData());
        assertEquals(4, list.identityMap(row -> row.path("key").asText()).get("a").path("value").asInt());
        assertEquals(List.of(expected.get(0), expected.get(1)), list.groupBy(row -> row.path("key").asText()).get("a"));
        assertEquals(Long.valueOf(9), list.getExactTotalCount());
        assertNull(list.getOrNull(-1)); assertNull(list.getOrNull(list.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, expected.get(0)));
        assertNull(new SmartList<>().first()); assertNull(new SmartList<>().last());
    }

    @Test public void missingEntityIdIsOmittedAndFacetsCanBeTaken() {
        var one = new SmartListTest.DummyEntity(1L);
        var lastOne = new SmartListTest.DummyEntity(1L);
        var noId = new SmartListTest.OtherEntity();
        SmartList<Entity> list = new SmartList<>(Arrays.asList(one, noId, lastOne));
        assertEquals(Arrays.asList(1L, null, 1L), list.ids());
        assertEquals(3, list.versions().size());
        assertEquals(Map.of(1L, lastOne), list.mapById());
        list.addFacet("empty", new SmartList<>());
        assertNull(list.getFacet("absent")); assertNotNull(list.getFacet("empty"));
        Map<String, SmartList<?>> taken = list.takeFacets();
        assertTrue(taken.containsKey("empty")); assertTrue(list.getFacets().isEmpty());
    }

    @Test public void projectionRecordIdentitySelectorsPreserveMixedIdKindsAndMissingValues() throws Exception {
        JsonNode fixture = vectors().get("identity");
        List<Map<String, Object>> data = JSON.convertValue(fixture.get("rows"), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        SmartList<Map<String, Object>> rows = new SmartList<>(data);
        assertEquals(JSON.convertValue(fixture.get("ids"), List.class), rows.toList(row -> row.get("id")));
        assertEquals(JSON.convertValue(fixture.get("versions"), List.class), rows.toList(row -> row.get("version")));
        var index = rows.filter(row -> row.get("id") != null).identityMap(row -> row.get("id"));
        assertEquals(4, index.size()); assertFalse(index.containsKey(null));
        var keys = List.of(0, 1, "1", "");
        for (int position = 0; position < keys.size(); position++)
            assertSame(rows.get(fixture.get("indexedPositions").get(position).intValue()), index.get(keys.get(position)));
    }

    @Test public void alignmentAddsNoPublicStateOrSummarySetter() throws Exception {
        assertTrue(Arrays.stream(SmartList.class.getMethods()).noneMatch(method ->
            method.getName().equals("setLoaded") || method.getName().equals("setSummary")));
        assertFalse(java.lang.reflect.Modifier.isPublic(SmartList.class.getDeclaredMethod("setSummary", Map.class).getModifiers()));
    }
}
