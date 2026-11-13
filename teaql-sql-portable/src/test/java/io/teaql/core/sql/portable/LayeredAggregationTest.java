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
        @Override @SuppressWarnings("unchecked")
        protected BaseRequest<T> internalCopyTarget() { return new Request<>((Class<T>) returnType(), type); }
        @Override @SuppressWarnings("unchecked")
        public Request<T> copy() { return (Request<T>) super.copy(); }
        Request<T> where(String property, Operator operator, Object... values) {
            appendSearchCriteria(createBasicSearchCriteria(property, operator, values)); return this;
        }
    }

    protected SimpleEntityMetaFactory metadata;
    protected FixtureDatabase database;
    protected UserContext context;

    protected interface FixtureDatabase extends TeaQLDatabase, AutoCloseable {
        void clearQueryTrace();
        List<String> queryTrace();
    }
    private static final class SQLiteFixture extends PortableSQLDatabaseTest.SQLiteTeaQLDatabase implements FixtureDatabase {
        SQLiteFixture() throws Exception {
            // This fixture uses the portable TeaQLDatabase, not the SQLite provider
            // which registers its own functions on every physical connection.
            super(new org.sqlite.Function() {
                @Override protected void xFunc() throws java.sql.SQLException {
                    String value = value_text(0);
                    String letters = value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
                    if (letters.isEmpty()) { result("?000"); return; }
                    String codes = "01230120022455012623010202";
                    StringBuilder encoded = new StringBuilder().append(letters.charAt(0));
                    char previous = codes.charAt(letters.charAt(0) - 'A');
                    for (int index = 1; index < letters.length() && encoded.length() < 4; index++) {
                        char code = codes.charAt(letters.charAt(index) - 'A');
                        if (code != '0' && code != previous) encoded.append(code);
                        previous = code;
                    }
                    while (encoded.length() < 4) encoded.append('0');
                    result(encoded.toString());
                }
            });
        }
    }
    protected FixtureDatabase createDatabase() throws Exception { return new SQLiteFixture(); }

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
        database = createDatabase();
        database.execute("CREATE TABLE rollup_platform(id INTEGER PRIMARY KEY,version INTEGER,name TEXT)");
        database.execute("CREATE TABLE rollup_kind(id INTEGER PRIMARY KEY,version INTEGER,\"Legacy Kind Label\" TEXT,ordinal INTEGER,platform INTEGER)");
        database.execute("CREATE TABLE rollup_school(id INTEGER PRIMARY KEY,version INTEGER,\"Legacy Capacity\" BIGINT,\"Legacy Kind Key\" INTEGER)");
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
    protected Request<StatKind> kind(String dimension) {
        var request = new Request<>(StatKind.class, "StatKind"); request.groupBy(dimension); return request;
    }
    protected Request<StatSchool> source(Request<StatKind> parent) {
        var request = new Request<>(StatSchool.class, "StatSchool");
        request.count("schoolCount"); request.sum("capacityTotal", "capacity");
        request.avg("capacityMean", "capacity"); request.min("capacityMin", "capacity"); request.max("capacityMax", "capacity");
        request.groupBy("kind", parent); return request;
    }
    protected List<Map<String, Object>> execute(Request<StatSchool> request) {
        return request.comment("Count source observations at the final parent grain")
                .purpose("Compare native Java roll-up results with the shared small fixture").aggregation(context).toList();
    }
    protected static long number(Map<String, Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    @SuppressWarnings("unchecked") protected static Map<String, Object> object(Map<String, Object> row, String key) { return (Map<String, Object>) row.get(key); }

    @Test public void mergedParentDimensionKeepsWeightedSourceMeasuresAndMappedKeys() {
        var rows = execute(source(kind("label"))); assertEquals(1, rows.size()); var row = rows.get(0);
        assertEquals(5, number(row, "schoolCount")); assertEquals(3300, number(row, "capacityTotal"));
        assertEquals(660.0, ((Number) row.get("capacityMean")).doubleValue(), 0.000001);
        assertEquals(100, number(row, "capacityMin")); assertEquals(800, number(row, "capacityMax"));
        assertEquals("shared", object(row, "kind").get("label"));
        assertFalse(row.containsKey("Legacy Kind Key")); assertFalse(row.containsKey("__teaql_m0"));
    }
    @Test public void distinctCountsUseFinalOwnerPopulationNotSumOfIntermediateCounts() {
        database.execute("INSERT INTO rollup_school VALUES(6,1,800,1002),(7,1,NULL,1002)");
        var parent = kind("label"); parent.countDistinctProperty("labels", "label");
        parent.having("labels", Operator.EQUAL, 1L);
        var request = source(parent); request.countDistinctProperty("capacities", "capacity");
        request.having("capacities", Operator.EQUAL, 2L);
        request.addOrderByDescending("capacities");
        var rows = execute(request); assertEquals(1, rows.size());
        assertEquals(7, number(rows.get(0), "schoolCount"));
        assertEquals(2, number(rows.get(0), "capacities"));
        assertEquals(1, number(object(rows.get(0), "kind"), "labels"));
        // Per-kind distinct capacity counts would be 1 + 2 = 3, not 2.
        assertEquals(2, number(execute(request).get(0), "capacities"));
    }
    @Test public void distinctCountEmptyAndNullPopulationsReturnZero() {
        database.execute("INSERT INTO rollup_school VALUES(7,1,NULL,1002)");
        for (long id : List.of(7L, 999L)) {
            var parent = new Request<>(StatKind.class, "StatKind");
            parent.countDistinctProperty("labels", "label");
            var request = source(parent).where("id", Operator.EQUAL, id);
            request.countDistinctProperty("capacities", "capacity");
            var rows = execute(request); assertEquals(1, rows.size());
            assertEquals(0, number(rows.get(0), "capacities"));
            assertEquals(id == 7 ? 1 : 0, number(object(rows.get(0), "kind"), "labels"));
        }
    }
    @Test public void ordinaryDistinctCountSupportsHavingOverMappedField() {
        var request = new Request<>(StatSchool.class, "StatSchool");
        request.countDistinctProperty("capacities", "capacity");
        request.having("capacities", Operator.EQUAL, 2L);
        assertEquals(2, number(execute(request).get(0), "capacities"));
        request.having("capacities", Operator.GREATER_THAN, 2L);
        assertTrue(execute(request).isEmpty());
    }

    @Test public void selfGroupingKeepsMappedValuesAliasesAndParentCellPaging() {
        var ordinary=new Request<>(StatSchool.class,"StatSchool");
        ordinary.groupBy("bucket","capacity",AggrFunction.SELF);ordinary.count("schoolCount");
        ordinary.having("bucket",Operator.GREATER_THAN_OR_EQUAL,100L);
        ordinary.addOrderByDescending("bucket");ordinary.offset(1,1);
        var parent=new Request<>(StatKind.class,"StatKind");
        parent.groupBy("rank","ordinal",AggrFunction.SELF);parent.count("kinds");parent.sum("ordinalTotal","ordinal");
        parent.having("rank",Operator.GREATER_THAN_OR_EQUAL,1L);parent.addOrderByDescending("ordinal");parent.top(1);
        var platform=new Request<>(StatPlatform.class,"StatPlatform");
        platform.groupBy("caption","name",AggrFunction.SELF);platform.count("platforms");platform.addOrderByAscending("name");
        var multi=new Request<>(StatKind.class,"StatKind");multi.count("kinds");multi.sum("ordinalTotal","ordinal");multi.groupBy("platform",platform);
        for(int round=0;round<2;round++) {
            var rows=execute(ordinary);assertEquals(1,rows.size());
            assertEquals(100,number(rows.get(0),"bucket"));assertEquals(1,number(rows.get(0),"schoolCount"));
            assertFalse(rows.get(0).containsKey("capacity"));
            rows=execute(source(parent));assertEquals(1,rows.size());
            assertEquals(1,number(rows.get(0),"schoolCount"));
            var kind=object(rows.get(0),"kind");assertEquals(2,number(kind,"rank"));assertFalse(kind.containsKey("ordinal"));
            rows=execute(source(multi));assertEquals(1,rows.size());assertEquals(5,number(rows.get(0),"schoolCount"));
            kind=object(rows.get(0),"kind");assertEquals(2,number(kind,"kinds"));assertEquals(3,number(kind,"ordinalTotal"));
            assertEquals("Campus",object(kind,"platform").get("caption"));assertEquals(1,number(object(kind,"platform"),"platforms"));
        }
    }

    @Test public void selfGroupingRetainsNullAndExactLargeIntegerKeys() {
        database.execute("UPDATE rollup_school SET \"Legacy Capacity\"=9007199254740993 WHERE id<=4");
        database.execute("UPDATE rollup_school SET \"Legacy Capacity\"=NULL WHERE id=5");
        for(boolean layered:List.of(false,true)) {
            var request=new Request<>(StatSchool.class,"StatSchool");
            request.groupBy("bucket","capacity",AggrFunction.SELF);request.count("schoolCount");
            request.addOrderByDescending("capacity");
            if(layered) {var parent=new Request<>(StatKind.class,"StatKind");parent.count("kinds");request.groupBy("kind",parent);}
            var rows=execute(request);assertEquals(2,rows.size());
            var nonnull=rows.stream().filter(row->row.get("bucket")!=null).findFirst().orElseThrow();
            var nil=rows.stream().filter(row->row.get("bucket")==null).findFirst().orElseThrow();
            assertEquals(9007199254740993L,number(nonnull,"bucket"));assertEquals(4,number(nonnull,"schoolCount"));
            assertEquals(1,number(nil,"schoolCount"));
        }
    }

    private static void phoneticDimension(Request<?> request,String field) {
        request.getAggregations().getSimpleDimensions().add(new SimpleNamedExpression("phonetic",
                new FunctionApply(Operator.SOUNDS_LIKE,new PropertyReference(field))));
    }
    @Test public void functionDimensionMergesDistinctParentValuesBeforePaging() {
        database.execute("UPDATE rollup_kind SET \"Legacy Kind Label\"=CASE id WHEN 1001 THEN 'Robert' ELSE 'Rupert' END");
        var parent=new Request<>(StatKind.class,"StatKind");phoneticDimension(parent,"label");
        parent.count("kinds");parent.sum("ordinalTotal","ordinal");
        parent.having("phonetic",Operator.EQUAL,"R163");parent.addOrderByAscending("phonetic");parent.top(1);
        var request=source(parent);
        for(int round=0;round<2;round++){
            var rows=execute(request);assertEquals(1,rows.size());var row=rows.get(0);
            assertEquals(5,number(row,"schoolCount"));assertEquals(3300,number(row,"capacityTotal"));
            var kind=object(row,"kind");assertEquals("R163",kind.get("phonetic"));
            assertEquals(2,number(kind,"kinds"));assertEquals(3,number(kind,"ordinalTotal"));
            assertFalse(kind.containsKey("label"));
        }
    }
    @Test public void multihopFunctionDimensionPreservesEveryMeasureOwner() {
        database.execute("UPDATE rollup_platform SET name='Robert' WHERE id=1");
        database.execute("INSERT INTO rollup_platform VALUES(2,1,'Rupert')");
        database.execute("UPDATE rollup_kind SET platform=2 WHERE id=1002");
        var platform=new Request<>(StatPlatform.class,"StatPlatform");phoneticDimension(platform,"name");platform.count("platforms");
        var parent=new Request<>(StatKind.class,"StatKind");parent.count("kinds");parent.sum("ordinalTotal","ordinal");parent.groupBy("platform",platform);
        var rows=execute(source(parent));assertEquals(1,rows.size());var row=rows.get(0);
        assertEquals(5,number(row,"schoolCount"));var kind=object(row,"kind");
        assertEquals(2,number(kind,"kinds"));assertEquals(3,number(kind,"ordinalTotal"));
        assertEquals("R163",object(kind,"platform").get("phonetic"));assertEquals(2,number(object(kind,"platform"),"platforms"));
    }

    @Test public void ordinaryFunctionGroupingHasTheSameKeyAndHavingSemantics() {
        database.execute("UPDATE rollup_kind SET \"Legacy Kind Label\"=CASE id WHEN 1001 THEN 'Robert' ELSE 'Rupert' END");
        var request=new Request<>(StatKind.class,"StatKind");phoneticDimension(request,"label");request.count("kinds");
        request.having("phonetic",Operator.EQUAL,"R163");request.addOrderByAscending("phonetic");request.top(1);
        var rows=request.comment("Group transformed parent labels without roll-up").purpose("Verify ordinary statistical expression keys").aggregation(context).toList();
        assertEquals(1,rows.size());assertEquals("R163",rows.get(0).get("phonetic"));
        assertEquals(2,number(rows.get(0),"kinds"));assertFalse(rows.get(0).containsKey("label"));
    }
    @Test public void sourceAndParentFiltersConstrainTheSamePopulation() {
        var parent = kind("label").where("ordinal", Operator.EQUAL, 1L);
        assertEquals(4, number(execute(source(parent)).get(0), "schoolCount"));
        assertTrue(execute(source(parent).where("capacity", Operator.LESS_THAN, 500L)).isEmpty());
    }
    @Test public void explicitCopyFreezesLiveGroupedParentAndGeneratedStyleMembership() {
        var parent = kind("label"); parent.count("types");
        var request = source(parent);
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", parent, "id"));
        database.clearQueryTrace(); var frozen = request.copy(); assertTrue(database.queryTrace().isEmpty());
        assertEquals(request.getClass(), frozen.getClass());
        assertEquals(parent.getClass(), frozen.getPropagateDimensions().get("kind").getClass());
        parent.where("ordinal", Operator.EQUAL, 1L);
        assertEquals(4, number(execute(request).get(0), "schoolCount"));
        for (int round=0; round<2; round++) {
            var row = execute(frozen).get(0);
            assertEquals(5, number(row, "schoolCount")); assertEquals(3300, number(row, "capacityTotal"));
            assertEquals(2, number(object(row, "kind"), "types"));
        }
    }
    @Test public void explicitCopyRetainsIndependentGrandparentAndParentCellBounds() {
        var platform = new Request<>(StatPlatform.class, "StatPlatform"); platform.groupBy("name");
        var parent = new Request<>(StatKind.class, "StatKind"); parent.count("types"); parent.groupBy("platform", platform);
        var request = source(parent); var frozen = request.copy();
        platform.where("name", Operator.EQUAL, "Other campus");
        parent.having("types", Operator.LESS_THAN, 2L); parent.offset(0, 1);
        assertTrue(execute(request).isEmpty());
        var row = execute(frozen).get(0);
        assertEquals(5, number(row, "schoolCount")); assertEquals(2, number(object(row, "kind"), "types"));
        assertEquals("Campus", object(object(row,"kind"),"platform").get("name"));
    }
    @Test public void explicitCopyDoesNotShareMutableGroupingOperands() {
        var parent = kind("label"); var request = source(parent); var frozen = request.copy();
        var function = (FunctionApply) parent.getAggregations().getSimpleDimensions().get(0).getExpression();
        ((PropertyReference) function.first()).setPropertyName("ordinal");
        assertEquals(2, execute(request).size());
        assertEquals(1, execute(frozen).size());
        assertEquals(3300, number(execute(frozen).get(0), "capacityTotal"));
    }
    @Test public void explicitCopyRetainsTypedPolicyHooksWithoutMutatingOriginalParent() {
        var inspected = new ArrayList<String>();
        context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("sql", new PortableSQLDataService("sql", database, metadata))
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> query) {
                        assertTrue("Policy must retain the native request type", query instanceof Request<?>);
                        inspected.add(query.getTypeName());
                        if (query.getTypeName().equals("StatKind"))
                            ((Request<?>) query).where("ordinal", Operator.EQUAL, 1L);
                    }
                }).build());
        var parent = kind("label"); parent.count("types"); var source = source(parent);
        var row = execute(source.copy()).get(0);
        assertEquals(4, number(row, "schoolCount")); assertEquals(3200, number(row, "capacityTotal"));
        assertEquals(1, number(object(row, "kind"), "types"));
        assertEquals(List.of("StatSchool", "StatKind"), inspected);
        assertNull("Policy enrichment belongs to the copy", parent.getSearchCriteria());
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
    private void addMissingParentReferences() {
        // Controlled legacy-data fixture, not an application seeding path.
        database.execute("INSERT INTO rollup_school VALUES(6,1,50,NULL),(7,1,700,9999)");
    }
    @Test public void missingParentReferencesAreNotARealNullGroupingDimension() {
        addMissingParentReferences();
        database.execute("UPDATE rollup_kind SET \"Legacy Kind Label\"=NULL WHERE id=1001");
        var raw = new Request<>(StatSchool.class,"StatSchool"); raw.groupBy("kind"); raw.count("schoolCount"); raw.sum("capacityTotal","capacity");
        var rawRows = execute(raw); assertEquals(4,rawRows.size());
        assertEquals(7,rawRows.stream().mapToLong(row->number(row,"schoolCount")).sum());
        assertTrue(rawRows.stream().anyMatch(row->row.containsKey("kind")&&row.get("kind")==null&&number(row,"capacityTotal")==50));
        assertTrue(rawRows.stream().anyMatch(row->Objects.equals(row.get("kind"),9999L)||Objects.equals(row.get("kind"),9999)));
        var parent=kind("label");parent.count("types");parent.sum("ordinalTotal","ordinal");
        var rows=execute(source(parent));assertEquals(2,rows.size());
        assertEquals(5,rows.stream().mapToLong(row->number(row,"schoolCount")).sum());
        assertEquals(3300,rows.stream().mapToLong(row->number(row,"capacityTotal")).sum());
        var nullCell=rows.stream().filter(row->object(row,"kind").containsKey("label")&&object(row,"kind").get("label")==null).findFirst().orElseThrow();
        assertEquals(4,number(nullCell,"schoolCount"));assertEquals(3200,number(nullCell,"capacityTotal"));
        assertEquals(1,number(object(nullCell,"kind"),"types"));
        var oracle=database.query("SELECT COUNT(*) AS n,SUM(s.\"Legacy Capacity\") AS total FROM rollup_school s JOIN rollup_kind k ON k.id=s.\"Legacy Kind Key\"",new Object[0]).get(0);
        assertEquals(5,number(oracle,"n"));assertEquals(3300,number(oracle,"total"));
    }
    @Test public void everyRollupHopRequiresAMatchingParentEvenWhenItsLabelIsNull() {
        database.execute("INSERT INTO rollup_kind VALUES(1003,1,'no root',3,NULL),(1004,1,'orphan root',4,9999)");
        database.execute("INSERT INTO rollup_school VALUES(6,1,50,1003),(7,1,700,1004)");
        database.execute("UPDATE rollup_platform SET name=NULL");
        var oneHop=execute(source(kind("label")));assertEquals(3,oneHop.size());
        assertEquals(7,oneHop.stream().mapToLong(row->number(row,"schoolCount")).sum());
        assertEquals(4050,oneHop.stream().mapToLong(row->number(row,"capacityTotal")).sum());
        var platform=new Request<>(StatPlatform.class,"StatPlatform");platform.groupBy("name");platform.count("platforms");
        var parent=new Request<>(StatKind.class,"StatKind");parent.count("types");parent.sum("ordinalTotal","ordinal");parent.groupBy("platform",platform);
        var rows=execute(source(parent));assertEquals(1,rows.size());var row=rows.get(0);
        assertEquals(5,number(row,"schoolCount"));assertEquals(3300,number(row,"capacityTotal"));
        assertEquals(2,number(object(row,"kind"),"types"));assertEquals(3,number(object(row,"kind"),"ordinalTotal"));
        var root=object(object(row,"kind"),"platform");assertTrue(root.containsKey("name"));assertNull(root.get("name"));assertEquals(1,number(root,"platforms"));
    }
    @Test public void filteredOrMissingParentsGiveEmptyGroupsButZeroScalarCounts() {
        addMissingParentReferences();
        database.execute("UPDATE rollup_kind SET \"Legacy Kind Label\"=NULL WHERE id=1002");
        var filtered=kind("label").where("ordinal",Operator.EQUAL,2L);filtered.count("types");
        var row=execute(source(filtered)).get(0);assertEquals(1,number(row,"schoolCount"));assertEquals(100,number(row,"capacityTotal"));assertNull(object(row,"kind").get("label"));
        assertTrue(execute(source(kind("label")).where("id",Operator.GREATER_THAN_OR_EQUAL,6L)).isEmpty());
        var scalar=new Request<>(StatKind.class,"StatKind");scalar.count("types");scalar.sum("ordinalTotal","ordinal");
        var rows=execute(source(scalar).where("id",Operator.GREATER_THAN_OR_EQUAL,6L));assertEquals(1,rows.size());row=rows.get(0);
        assertEquals(0,number(row,"schoolCount"));assertNull(row.get("capacityTotal"));assertNull(row.get("capacityMean"));
        assertEquals(0,number(object(row,"kind"),"types"));assertNull(object(row,"kind").get("ordinalTotal"));
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
    @Test public void linkedMembershipInsideOrUsesTheSameParentCellsNotASecondIdQuery() {
        var parent=kind("ordinal");parent.addOrderByDescending("ordinal");parent.top(1);
        var request=source(parent);
        request.appendSearchCriteria(SearchCriteria.or(new SubQuerySearchCriteria("kind",parent,"id"),
                request.createBasicSearchCriteria("capacity",Operator.GREATER_THAN,500L)));
        var original=request.getSearchCriteria();database.clearQueryTrace();
        var row=execute(request).get(0);assertEquals(1,number(row,"schoolCount"));assertEquals(100,number(row,"capacityTotal"));
        assertEquals(1,database.queryTrace().size());assertSame(original,request.getSearchCriteria());
        var invalid=source(parent);invalid.appendSearchCriteria(SearchCriteria.or(new SubQuerySearchCriteria("kind",parent,"id"),
                invalid.createBasicSearchCriteria("missing",Operator.EQUAL,"not-for-diagnostics")));
        database.clearQueryTrace();assertThrows(TeaQLRuntimeException.class,()->execute(invalid));
        assertTrue("A logically true OR must not skip validation of another branch",database.queryTrace().isEmpty());
    }
    @Test public void negatedLinkedMembershipIsContradictoryForGroupedAndScalarParents() {
        for(boolean grouped:List.of(false,true)) {
            var parent=grouped?kind("label"):new Request<>(StatKind.class,"StatKind");parent.count("types");
            var request=source(parent);request.appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria("kind",parent,"id")));
            database.clearQueryTrace();var rows=execute(request);assertEquals(1,database.queryTrace().size());
            if(grouped)assertTrue(rows.isEmpty());
            else {assertEquals(1,rows.size());assertEquals(0,number(rows.get(0),"schoolCount"));assertNull(rows.get(0).get("capacityTotal"));assertEquals(0,number(object(rows.get(0),"kind"),"types"));}
        }
    }

    @Test public void independentParentMembershipRemainsABusinessFilter() {
        var request = source(kind("label"));
        var independent = new Request<>(StatKind.class, "StatKind").where("ordinal", Operator.EQUAL, 1L);
        independent.unlimited();
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", independent, "id"));
        var row = execute(request).get(0);
        assertEquals(4, number(row, "schoolCount")); assertEquals(3200, number(row, "capacityTotal"));
        var grouped=kind("label").where("ordinal",Operator.EQUAL,2L);
        var negated=source(grouped);negated.appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria("kind",independent,"id")));
        row=execute(negated).get(0);assertEquals(1,number(row,"schoolCount"));assertEquals(100,number(row,"capacityTotal"));
    }

    @Test public void independentGroupedMembershipRetainsItsHaving() {
        var independent = kind("id");
        independent.sum("ordinalTotal", "ordinal");
        independent.having("ordinalTotal", Operator.EQUAL, 2L);
        independent.unlimited();
        var request = source(kind("label"));
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", independent, "id"));
        var rows = execute(request);
        assertEquals(1, rows.size());
        assertEquals(1, number(rows.get(0), "schoolCount"));
        assertEquals(100, number(rows.get(0), "capacityTotal"));
        // The independent request is a business filter, not the linked roll-up parent.
        // Its selected id is a grouping key; ordinalTotal is not an extra IN column.
        var negated = source(kind("label"));
        negated.appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria("kind", independent, "id")));
        assertEquals(4, number(execute(negated).get(0), "schoolCount"));
    }

    @Test public void emptyIndependentGroupedMembershipDoesNotBroadenThePopulation() {
        var independent = kind("id");
        independent.sum("ordinalTotal", "ordinal");
        independent.having("ordinalTotal", Operator.GREATER_THAN, 2L);
        independent.unlimited();
        var request = source(kind("label"));
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", independent, "id"));
        assertTrue(execute(request).isEmpty());
        var negated = source(kind("label"));
        negated.appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria("kind", independent, "id")));
        assertEquals(5, number(execute(negated).get(0), "schoolCount"));
    }

    @Test public void independentGroupedMembershipKeepsPagingAndSelectedAliases() {
        var independent = kind("id");
        var dimension = independent.getAggregations().getSimpleDimensions().get(0);
        independent.getAggregations().getSimpleDimensions().set(0,
                new SimpleNamedExpression("kindKey", dimension.getExpression()));
        independent.sum("ordinalTotal", "ordinal");
        independent.addOrderByDescending("ordinalTotal"); independent.top(1);
        var request = source(kind("label"));
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", independent, "kindKey"));
        assertEquals(1, number(execute(request).get(0), "schoolCount"));
        assertEquals(1, independent.getSlice().getSize());
    }

    @Test public void independentGroupedMembershipRejectsAnUnselectedIdentityBeforeSql() {
        var independent = kind("label"); independent.count("types"); independent.unlimited();
        var request = source(kind("label"));
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", independent, "id"));
        database.clearQueryTrace();
        var error = assertThrows(TeaQLRuntimeException.class, () -> execute(request));
        assertTrue(error.getMessage().contains("selected statistical result"));
        assertTrue(database.queryTrace().isEmpty());
    }

    @Test public void independentGroupedMembershipReceivesTypedPolicyAndIntent() {
        var seen = new ArrayList<String>();
        context = new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata)
                .dataService("sql", new PortableSQLDataService("sql", database, metadata))
                .queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> query) {
                        assertTrue(query instanceof Request<?>);
                        seen.add(query.comment());
                        if (query.getTypeName().equals("StatKind"))
                            ((Request<?>) query).where("ordinal", Operator.EQUAL, 1L);
                    }
                }).build());
        var independent = kind("id").comment("Authorize an independent statistical predicate");
        independent.sum("ordinalTotal", "ordinal"); independent.having("ordinalTotal", Operator.EQUAL, 2L);
        independent.unlimited();
        var request = source(kind("label"));
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", independent, "id"));
        assertTrue(execute(request).isEmpty());
        assertTrue(seen.contains("Authorize an independent statistical predicate"));
    }

    @Test public void independentStatisticalMembershipCannotCrossDataServiceBoundaries() {
        metadata.resolveEntityDescriptor("StatKind").setDataService("another-service");
        var independent = kind("id"); independent.sum("ordinalTotal", "ordinal"); independent.unlimited();
        var request = new Request<>(StatSchool.class, "StatSchool"); request.count("schoolCount");
        request.appendSearchCriteria(new SubQuerySearchCriteria("kind", independent, "id"));
        database.clearQueryTrace();
        var error = assertThrows(TeaQLRuntimeException.class, () -> execute(request));
        assertTrue(error.getMessage(), error.getMessage().contains("embeddable SQL"));
        assertTrue(database.queryTrace().isEmpty());
    }
}
