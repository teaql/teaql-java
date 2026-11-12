package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.*;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.GenericSQLRelation;
import io.teaql.runtime.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Small, mapped three-level fixture shared in meaning with the Rust tests. */
public class LayeredAggregationTest {
    public static class StatSchool extends BaseEntity { @Override public String typeName() { return "StatSchool"; } }
    public static class StatKind extends BaseEntity { @Override public String typeName() { return "StatKind"; } }
    public static class StatPlatform extends BaseEntity { @Override public String typeName() { return "StatPlatform"; } }
    public static class Request<T extends BaseEntity> extends BaseRequest<T> {
        private final String type;
        Request(Class<T> entity, String type) { super(entity); this.type = type; }
        @Override public String getTypeName() { return type; }
        public Request<T> comment(String text) { super.internalComment(text); return this; }
        Request<T> where(String property, Operator operator, Object... values) {
            appendSearchCriteria(createBasicSearchCriteria(property, operator, values)); return this;
        }
    }

    private SimpleEntityMetaFactory metadata;
    private PortableSQLDatabaseTest.SQLiteTeaQLDatabase database;
    private UserContext context;

    @Before public void setup() throws Exception {
        metadata = new SimpleEntityMetaFactory();
        var platform = descriptor("StatPlatform", StatPlatform.class, "rollup_platform",
                new String[][]{{"name", "name"}});
        var kind = descriptor("StatKind", StatKind.class, "rollup_kind",
                new String[][]{{"label", "Legacy Kind Label"}, {"ordinal", "ordinal"}});
        var school = descriptor("StatSchool", StatSchool.class, "rollup_school",
                new String[][]{{"capacity", "Legacy Capacity"}});
        metadata.register(platform); metadata.register(kind); metadata.register(school);
        reference(kind, "platform", "StatPlatform", "rollup_kind", "platform");
        reference(school, "kind", "StatKind", "rollup_school", "Legacy Kind Key");
        database = new PortableSQLDatabaseTest.SQLiteTeaQLDatabase();
        database.execute("CREATE TABLE rollup_platform(id INTEGER PRIMARY KEY,version INTEGER,name TEXT)");
        database.execute("CREATE TABLE rollup_kind(id INTEGER PRIMARY KEY,version INTEGER,\"Legacy Kind Label\" TEXT,ordinal INTEGER,platform INTEGER)");
        database.execute("CREATE TABLE rollup_school(id INTEGER PRIMARY KEY,version INTEGER,\"Legacy Capacity\" INTEGER,\"Legacy Kind Key\" INTEGER)");
        database.execute("INSERT INTO rollup_platform VALUES(1,1,'Campus')");
        database.execute("INSERT INTO rollup_kind VALUES(1001,1,'shared',1,1),(1002,1,'shared',2,1)");
        database.execute("INSERT INTO rollup_school VALUES(1,1,800,1001),(2,1,800,1001),(3,1,800,1001),(4,1,800,1001),(5,1,100,1002)");
        context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("sql", new PortableSQLDataService("sql", database, metadata)).build());
    }
    @After public void close() throws Exception { if (database != null) database.close(); }

    private static EntityDescriptor descriptor(String name, Class<? extends BaseEntity> type, String table, String[][] fields) {
        var descriptor = new EntityDescriptor(); descriptor.setType(name); descriptor.setTargetType(type); descriptor.setDataService("sql");
        List<String[]> definitions = new ArrayList<>(List.of(new String[]{"id", "id"}, new String[]{"version", "version"}));
        definitions.addAll(Arrays.asList(fields));
        for (var field : definitions) {
            boolean text = Set.of("label", "name").contains(field[0]);
            var property = new GenericSQLProperty(table, field[1], text ? "TEXT" : "INTEGER");
            property.setName(field[0]); property.setOwner(descriptor); property.setType(new SimplePropertyType(text ? String.class : Long.class));
            descriptor.getProperties().add(property);
        }
        return descriptor;
    }
    private void reference(EntityDescriptor owner, String name, String target, String table, String column) {
        var relation = (GenericSQLRelation) owner.addObjectProperty(metadata, name, target,
                owner.getType() + "List", metadata.resolveEntityDescriptor(target).getTargetType(), GenericSQLRelation::new);
        relation.setTableName(table); relation.setColumnName(column); relation.setColumnType("INTEGER");
    }
    private Request<StatKind> kind(String dimension) {
        var request = new Request<>(StatKind.class, "StatKind"); request.groupBy(dimension); return request;
    }
    private Request<StatSchool> source(Request<StatKind> parent) {
        var request = new Request<>(StatSchool.class, "StatSchool");
        request.count("schoolCount"); request.sum("capacityTotal", "capacity");
        request.avg("capacityMean", "capacity"); request.min("capacityMin", "capacity"); request.max("capacityMax", "capacity");
        request.groupBy("kind", parent); return request;
    }
    private List<Map<String, Object>> execute(Request<StatSchool> request) {
        return request.comment("Count source observations at the final parent grain")
                .purpose("Compare native Java roll-up results with the shared small fixture").aggregation(context).toList();
    }
    private static long number(Map<String, Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    @SuppressWarnings("unchecked") private static Map<String, Object> object(Map<String, Object> row, String key) { return (Map<String, Object>) row.get(key); }

    @Test public void mergedParentDimensionKeepsWeightedSourceMeasuresAndMappedKeys() {
        var rows = execute(source(kind("label"))); assertEquals(1, rows.size()); var row = rows.get(0);
        assertEquals(5, number(row, "schoolCount")); assertEquals(3300, number(row, "capacityTotal"));
        assertEquals(660.0, ((Number) row.get("capacityMean")).doubleValue(), 0.000001);
        assertEquals(100, number(row, "capacityMin")); assertEquals(800, number(row, "capacityMax"));
        assertEquals("shared", object(row, "kind").get("label"));
        assertFalse(row.containsKey("Legacy Kind Key")); assertFalse(row.containsKey("__teaql_m0"));
    }
    @Test public void sourceAndParentFiltersConstrainTheSamePopulation() {
        var parent = kind("label").where("ordinal", Operator.EQUAL, 1L);
        assertEquals(4, number(execute(source(parent)).get(0), "schoolCount"));
        assertTrue(execute(source(parent).where("capacity", Operator.LESS_THAN, 500L)).isEmpty());
    }
    @Test public void multihopDeduplicatesParentMeasuresBeforeJoiningFinalCells() {
        var parent = new Request<>(StatKind.class, "StatKind"); parent.count("typeCount"); parent.sum("ordinalTotal", "ordinal");
        var platform = new Request<>(StatPlatform.class, "StatPlatform"); platform.groupBy("name"); parent.groupBy("platform", platform);
        var rows = execute(source(parent)); assertEquals(1, rows.size()); var row = rows.get(0);
        assertEquals(5, number(row, "schoolCount")); assertEquals(2, number(object(row, "kind"), "typeCount"));
        assertEquals(3, number(object(row, "kind"), "ordinalTotal"));
        assertEquals("Campus", object(object(row, "kind"), "platform").get("name"));
    }
    @Test public void parentLimitSelectsGroupedCellsInsteadOfFirstParentId() {
        var parent = kind("label"); parent.offset(0, 1);
        assertEquals(5, number(execute(source(parent)).get(0), "schoolCount"));
    }
    @Test public void parentOrderAndPageApplyAfterSourcePopulationFiltering() {
        var parent = kind("ordinal"); parent.addOrderByDescending("ordinal"); parent.offset(0, 1);
        assertEquals(1, number(execute(source(parent)).get(0), "schoolCount"));
        var filtered = source(parent).where("capacity", Operator.GREATER_THAN_OR_EQUAL, 800L);
        var rows = execute(filtered); assertEquals(4, number(rows.get(0), "schoolCount"));
        assertEquals(1, number(object(rows.get(0), "kind"), "ordinal"));
    }
    @Test public void sourceOrderAndPageApplyToFinalStatisticalRows() {
        var request = source(kind("ordinal")); request.addOrderByDescending("schoolCount"); request.offset(1, 1);
        var rows = execute(request); assertEquals(1, rows.size()); assertEquals(1, number(rows.get(0), "schoolCount"));
    }
    @Test public void groupOnlyRequestDoesNotHydrateArbitrarySourceEntities() {
        var request = new Request<>(StatSchool.class, "StatSchool"); request.groupBy("kind", kind("label"));
        var rows = execute(request); assertEquals(1, rows.size()); assertEquals(Set.of("kind"), rows.get(0).keySet());
    }
    @Test public void emptyScalarPopulationHasZeroCountsAndNullNumericAggregates() {
        var parent = new Request<>(StatKind.class, "StatKind"); parent.count("types");
        var rows = execute(source(parent).where("capacity", Operator.LESS_THAN, 0L));
        assertEquals(1, rows.size()); var row = rows.get(0); assertEquals(0, number(row, "schoolCount"));
        for (String alias : List.of("capacityTotal", "capacityMean", "capacityMin", "capacityMax")) assertNull(row.get(alias));
        assertEquals(0, number(object(row, "kind"), "types"));
    }
    @Test public void nullDimensionsMatchTheirOwnAggregateCells() {
        database.execute("UPDATE rollup_kind SET \"Legacy Kind Label\"=NULL");
        var row = execute(source(kind("label"))).get(0);
        assertEquals(5, number(row, "schoolCount")); assertTrue(object(row, "kind").containsKey("label")); assertNull(object(row, "kind").get("label"));
    }
    @Test public void nullMeasuresDoNotEnterAverageOrFieldCount() {
        database.execute("UPDATE rollup_school SET \"Legacy Capacity\"=NULL WHERE id=5");
        var request = source(kind("label")); request.countProperty("nonNullCapacity", "capacity");
        var row = execute(request).get(0); assertEquals(5, number(row, "schoolCount"));
        assertEquals(4, number(row, "nonNullCapacity")); assertEquals(800.0, ((Number) row.get("capacityMean")).doubleValue(), 0.000001);
    }
    @Test public void invalidAliasAndForeignServiceFailBeforeAnySql() {
        var request = source(kind("label")); request.count("kind"); database.clearQueryTrace();
        try { execute(request); fail("nested result collision must fail"); }
        catch (TeaQLRuntimeException expected) { assertTrue(expected.getMessage().contains("Conflicting statistical result path")); }
        assertTrue(database.queryTrace().isEmpty());
        metadata.resolveEntityDescriptor("StatKind").setDataService("external");
        try { execute(source(kind("label"))); fail("cross-service SQL must fail"); }
        catch (TeaQLRuntimeException expected) { assertTrue(expected.getMessage().contains("share a data service")); }
        assertTrue(database.queryTrace().isEmpty());
    }
    @Test public void negativeBoundsAndInvalidOrderCannotReachSql() {
        var parent = kind("label"); parent.offset(-1, 1); database.clearQueryTrace();
        try { execute(source(parent)); fail("negative parent offset must fail"); }
        catch (TeaQLRuntimeException expected) { assertTrue(expected.getMessage().contains("page boundary")); }
        var request = source(kind("label"));
        request.getOrderBy().addOrderBy(new OrderBy("schoolCount", "DESC; SELECT 1"));
        try { execute(request); fail("order direction must not be concatenated as arbitrary SQL"); }
        catch (TeaQLRuntimeException expected) { assertTrue(expected.getMessage().contains("order direction")); }
        assertTrue(database.queryTrace().isEmpty());
    }
    @Test public void defaultRouteSpellingsAreTheSameLogicalDataService() {
        metadata.resolveEntityDescriptor("StatSchool").setDataService("");
        metadata.resolveEntityDescriptor("StatKind").setDataService("default");
        metadata.resolveEntityDescriptor("StatPlatform").setDataService("default");
        context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("default", new PortableSQLDataService("default", database, metadata)).build());
        assertEquals(5, number(execute(source(kind("label"))).get(0), "schoolCount"));
    }

    @Test public void sourceHavingRunsAfterGroupingAndBeforePaging() {
        var request = source(kind("ordinal"));
        request.having("schoolCount", Operator.GREATER_THAN, 1L);
        request.addOrderByAscending("schoolCount"); request.offset(0, 1);
        var rows = execute(request);
        assertEquals(1, rows.size()); assertEquals(4, number(rows.get(0), "schoolCount"));
        assertEquals(1, number(object(rows.get(0), "kind"), "ordinal"));
    }

    @Test public void parentHavingUsesParentOwnerGrainInsteadOfChildCount() {
        var parent = kind("label"); parent.count("types");
        parent.having("types", Operator.GREATER_THAN_OR_EQUAL, 2L);
        assertEquals(5, number(execute(source(parent)).get(0), "schoolCount"));
        parent.having("types", Operator.LESS_THAN, 2L);
        assertTrue(execute(source(parent)).isEmpty());
    }

    @Test public void parentHavingWithoutPageAndAfterSourceFilterControlsEligibleCells() {
        var parent = kind("label"); parent.count("types"); parent.unlimited();
        parent.having("types", Operator.GREATER_THAN, 1L);
        assertEquals(5, number(execute(source(parent)).get(0), "schoolCount"));
        assertTrue(execute(source(parent).where("capacity", Operator.GREATER_THAN, 500L)).isEmpty());
    }

    @Test public void havingResolvesNestedResultAliasesWithoutUsingPhysicalColumns() {
        var parent = kind("label"); parent.count("types");
        var request = source(parent); request.having("kind.types", Operator.EQUAL, 2L);
        assertEquals(5, number(execute(request).get(0), "schoolCount"));
        request.having("kind.label", Operator.EQUAL, "not shared");
        assertTrue(execute(request).isEmpty());
    }

    @Test public void havingSupportsBooleanCompositionBetweenAndNull() {
        database.execute("UPDATE rollup_kind SET \"Legacy Kind Label\"=NULL WHERE id=1002");
        var request = source(kind("label"));
        request.having(SearchCriteria.or(
                request.createBasicSearchCriteria("kind.label", Operator.IS_NULL),
                request.createBasicSearchCriteria("schoolCount", Operator.BETWEEN, 2L, 3L)));
        var rows = execute(request); assertEquals(1, rows.size());
        assertEquals(1, number(rows.get(0), "schoolCount"));
        var empty = source(kind("label"));
        empty.having(SearchCriteria.not(empty.createBasicSearchCriteria("schoolCount", Operator.BETWEEN, 1L, 5L)));
        assertTrue(execute(empty).isEmpty());
    }

    @Test public void simpleStatisticsAlsoApplyHavingAndRetainEmptyScalarSemantics() {
        var request = new Request<>(StatSchool.class, "StatSchool"); request.count("schoolCount");
        request.having("schoolCount", Operator.GREATER_THAN, 5L);
        assertTrue(execute(request).isEmpty());
        var empty = new Request<>(StatSchool.class, "StatSchool"); empty.count("schoolCount");
        empty.sum("capacityTotal", "capacity"); empty.where("capacity", Operator.LESS_THAN, 0L);
        empty.having("schoolCount", Operator.EQUAL, 0L); empty.having("capacityTotal", Operator.IS_NULL);
        assertEquals(0, number(execute(empty).get(0), "schoolCount"));
    }

    @Test public void havingRejectsUnknownOrNonSelectedKeysBeforeSql() {
        for (String key : List.of("capacity", "Legacy Capacity", "missing", "schoolCount; DROP TABLE rollup_school")) {
            var request = source(kind("label")); request.having(key, Operator.GREATER_THAN, 0L);
            database.clearQueryTrace();
            assertThrows(TeaQLRuntimeException.class, () -> execute(request));
            assertTrue(database.queryTrace().isEmpty());
        }
    }

    @Test public void simpleHavingOrderingAndPagingUseStatisticalRows() {
        var request = new Request<>(StatSchool.class, "StatSchool");
        request.groupBy("kind"); request.count("schoolCount");
        request.having("schoolCount", Operator.GREATER_THAN, 0L);
        request.addOrderByAscending("schoolCount"); request.offset(1, 1);
        var rows = execute(request); assertEquals(1, rows.size());
        assertEquals(4, number(rows.get(0), "schoolCount"));
    }

    @Test public void havingThresholdsAreCapturedFromEveryLayerForIntentRedaction() {
        var repositories = new HashMap<String, PortableSQLRepository<?>>();
        for (String type : List.of("StatSchool", "StatKind", "StatPlatform"))
            repositories.put(type, new PortableSQLRepository<>(metadata.resolveEntityDescriptor(type), database, repositories::get, metadata));
        var parent = kind("label"); parent.having("label", Operator.EQUAL, "PARENT-HAVING-PRIVATE");
        var request = source(parent); request.having("schoolCount", Operator.EQUAL, 927381L);
        var redactions = new SqlIntentRedactions();
        SqlLikeIntent.capture(context, request, repositories.get("StatSchool"), redactions);
        var values = new ArrayList<Object>(); redactions.appendTo(values, false);
        assertTrue(values.contains("PARENT-HAVING-PRIVATE")); assertTrue(values.contains("927381"));
    }

    @Test public void generatedStyleRollupMembershipDoesNotMaterializeGroupedParentIds() {
        var parent = kind("ordinal"); parent.addOrderByDescending("ordinal"); parent.offset(0, 1);
        var request = source(parent).where("capacity", Operator.GREATER_THAN, 0L);
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", parent, "id"));
        var original = request.getSearchCriteria(); database.clearQueryTrace();
        var rows = execute(request); assertEquals(1, rows.size());
        assertEquals(1, number(rows.get(0), "schoolCount")); assertEquals(100, number(rows.get(0), "capacityTotal"));
        assertEquals("Only the statistical SQL should execute", 1, database.queryTrace().size());
        assertSame("The reusable source builder must not be rewritten", original, request.getSearchCriteria());
    }

    @Test public void independentParentMembershipRemainsABusinessFilter() {
        var request = source(kind("label"));
        var independent = new Request<>(StatKind.class, "StatKind").where("ordinal", Operator.EQUAL, 1L);
        independent.unlimited();
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", independent, "id"));
        var row = execute(request).get(0);
        assertEquals(4, number(row, "schoolCount")); assertEquals(3200, number(row, "capacityTotal"));
    }
}
