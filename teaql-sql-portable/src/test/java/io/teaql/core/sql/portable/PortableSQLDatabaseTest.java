package io.teaql.core.sql.portable;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.GenericSQLRelation;
import io.teaql.core.sql.SQLProperty;
import io.teaql.core.sql.SQLColumn;
import io.teaql.runtime.*;

import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PortableSQLDatabaseTest {

    public static class TopNParent extends BaseEntity {
        private String name;
        private SmartList<TopNChild> children;
        @Override public String typeName() { return "TopNParent"; }
        @Override public Object __internalGet(String property) {
            if ("name".equals(property)) return name;
            if ("children".equals(property)) return children;
            return super.__internalGet(property);
        }
        @Override public void __internalSet(String property, Object value) {
            if ("name".equals(property)) { name = (String) value; return; }
            if ("children".equals(property)) { children = (SmartList<TopNChild>) value; return; }
            super.__internalSet(property, value);
        }
    }

    public static class TopNChild extends BaseEntity {
        private TopNParent parent;
        private String name;
        private String state;
        @Override public String typeName() { return "TopNChild"; }
        @Override public Object __internalGet(String property) {
            if ("parent".equals(property)) return parent;
            if ("name".equals(property)) return name;
            if ("state".equals(property)) return state;
            return super.__internalGet(property);
        }
        @Override public void __internalSet(String property, Object value) {
            if ("parent".equals(property)) { parent = (TopNParent) value; return; }
            if ("name".equals(property)) { name = (String) value; return; }
            if ("state".equals(property)) { state = (String) value; return; }
            super.__internalSet(property, value);
        }
    }

    public static class TopNParentRequest extends BaseRequest<TopNParent> {
        public TopNParentRequest() { super(TopNParent.class); }
        @Override public String getTypeName() { return "TopNParent"; }
        public TopNParentRequest where(String field, Operator op, Object... values) {
            appendSearchCriteria(createBasicSearchCriteria(field, op, values));
            return this;
        }
        public TopNParentRequest comment(String value) { super.internalComment(value); return this; }
    }

    public static class TopNChildRequest extends BaseRequest<TopNChild> {
        public TopNChildRequest() { super(TopNChild.class); }
        @Override public String getTypeName() { return "TopNChild"; }
        public TopNChildRequest where(String field, Operator op, Object... values) {
            appendSearchCriteria(createBasicSearchCriteria(field, op, values));
            return this;
        }
    }

    // ── Stub Entity and Request ──────────────────────────

    public static class Task extends BaseEntity {
        private String title;
        private String status;
        private final Set<String> loadedProperties = new HashSet<>();

        public String getTitle() {
            return title;
        }

        public Task updateTitle(String title) {
            handleUpdate("title", this.title, title);
            this.title = title;
            return this;
        }

        public String getStatus() {
            return status;
        }

        public boolean isLoaded(String property) {
            return loadedProperties.contains(property);
        }

        public Task updateStatus(String status) {
            handleUpdate("status", this.status, status);
            this.status = status;
            return this;
        }

        @Override
        public String typeName() {
            return "Task";
        }

        @Override
        public void __internalSet(String property, Object value) {
            loadedProperties.add(property);
            switch (property) {
                case "title": this.title = (String) value; break;
                case "status": this.status = (String) value; break;
                default: super.__internalSet(property, value);
            }
        }

        @Override
        public Object __internalGet(String property) {
            switch (property) {
                case "title": return this.title;
                case "status": return this.status;
                default: return super.__internalGet(property);
            }
        }
    }

    @Test
    public void testSelectedSqlNullIsMappedAsLoadedNull() {
        sqliteDb.executeUpdate(
                "INSERT INTO task_data (id, version, title, status) VALUES (?, ?, ?, ?)",
                new Object[] {900L, 1L, null, "NULL-MAPPING"});

        SmartList<Task> tasks =
                new TaskRequest()
                        .filterByStatus("NULL-MAPPING")
                        .comment("verify selected SQL null mapping")
                        .purpose("distinguish loaded null from an unselected field")
                        .executeForList(context);

        assertEquals(1, tasks.size());
        Task loaded = tasks.get(0);
        assertNull(loaded.getTitle());
        assertTrue("selected SQL NULL must still invoke the entity mapper", loaded.isLoaded("title"));
    }

    @Test
    public void TOPN_001_TO_009_windowAndProbePlansAreEquivalentOnSQLite() {
        registerTopNFixture();
        sqlDataService.setTopNRelationPlanPolicy(TopNRelationPlanPolicy.ALWAYS_PROBE);

        Map<Long, List<Long>> probe = loadTopNFixture(null);
        int probeQueries = sqliteDb.queryTrace().size();
        Map<Long, List<Long>> window = loadTopNFixture(0);
        int windowQueries = sqliteDb.queryTrace().size();

        assertEquals(window, probe);
        assertEquals(Map.of(1L, List.of(11L, 12L), 2L, List.of(21L, 22L), 3L, List.of()), window);
        assertEquals("one parent query plus one bounded child query per parent", 4, probeQueries);
        assertEquals("one parent query plus one partition window query", 2, windowQueries);
        assertTrue("Top-N plan selection must not issue COUNT", sqliteDb.queryTrace().stream()
                .noneMatch(sql -> sql.toLowerCase(Locale.ROOT).contains("count(")));
    }

    @Test
    public void TOPN_001_TO_003_AND_011_thresholdBoundaryIsStableAcrossExecutions() {
        registerTopNFixture();
        sqlDataService.setTopNRelationPlanPolicy(TopNRelationPlanPolicy.WINDOW);

        assertEquals(loadTopNFixture(3), loadTopNFixture(3));
        assertEquals(4, sqliteDb.queryTrace().size());
        loadTopNFixture(2);
        assertEquals(2, sqliteDb.queryTrace().size());
        loadTopNFixture(0);
        assertEquals(2, sqliteDb.queryTrace().size());
    }

    @Test
    public void TOPN_008_emptyParentSetReturnsTypedEmptyWithoutChildQuery() {
        registerTopNFixture();
        sqliteDb.clearQueryTrace();
        TopNParentRequest parent = new TopNParentRequest().where("name", Operator.EQUAL, "missing");
        parent.selectProperty("id");
        parent.selectProperty("version");
        parent.selectProperty("name");
        parent.enhanceRelation("children", new TopNChildRequest().top(2));
        SmartList<TopNParent> rows = parent.comment("load an empty parent set")
                .purpose("verify empty Top-N relation semantics")
                .executeForList(context);
        assertTrue(rows.isEmpty());
        assertEquals(1, sqliteDb.queryTrace().size());
    }

    @Test
    public void TOPN_012_canonicalRelationIndexEnsureIsIdempotentOnSQLite() {
        registerTopNFixture();
        sqlDataService.ensureSchema(context, "TopNChild");
        sqlDataService.ensureSchema(context, "TopNChild");

        List<Map<String, Object>> indexes = sqliteDb.query(
                "SELECT name, sql FROM sqlite_master WHERE type='index' "
                        + "AND tbl_name='top_n_child_data' AND sql LIKE '%parent%id DESC%'",
                new Object[0]);
        assertEquals(1, indexes.size());
    }

    private Map<Long, List<Long>> loadTopNFixture(Integer threshold) {
        sqliteDb.clearQueryTrace();
        TopNChildRequest child = new TopNChildRequest().where("state", Operator.EQUAL, "visible");
        child.selectProperty("id");
        child.selectProperty("version");
        child.selectProperty("name");
        child.selectProperty("state");
        child.addOrderByDescending("name");
        child.offset(0, 2);
        if (threshold != null) child.topNProbeParentThreshold(threshold);
        TopNParentRequest parent = new TopNParentRequest();
        parent.selectProperty("id");
        parent.selectProperty("version");
        parent.selectProperty("name");
        parent.addOrderByAscending("id");
        parent.enhanceRelation("children", child);
        SmartList<TopNParent> rows = parent.comment("load per-parent Top-N fixture")
                .purpose("verify governed relation plan equivalence")
                .executeForList(context);
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (TopNParent row : rows) {
            SmartList<TopNChild> children = row.getProperty("children");
            result.put(row.getId(), children == null ? List.of() : children.toList(Entity::getId));
        }
        return result;
    }

    private void registerTopNFixture() {
        boolean registered = metaFactory.allEntityDescriptors().stream()
                .anyMatch(descriptor -> "TopNParent".equals(descriptor.getType()));
        if (registered) {
            sqliteDb.execute("DELETE FROM top_n_child_data");
            sqliteDb.execute("DELETE FROM top_n_parent_data");
            seedTopNFixture();
            return;
        }
        EntityDescriptor parent = relationEntity(
                "TopNParent", TopNParent.class, TopNParent::new, "top_n_parent_data",
                List.of(new Object[] {"id", "INTEGER", Long.class},
                        new Object[] {"version", "INTEGER", Long.class},
                        new Object[] {"name", "VARCHAR(100)", String.class}));
        metaFactory.register(parent);
        EntityDescriptor child = relationEntity(
                "TopNChild", TopNChild.class, TopNChild::new, "top_n_child_data",
                List.of(new Object[] {"id", "INTEGER", Long.class},
                        new Object[] {"version", "INTEGER", Long.class},
                        new Object[] {"name", "VARCHAR(100)", String.class},
                        new Object[] {"state", "VARCHAR(100)", String.class}));
        GenericSQLRelation relation = (GenericSQLRelation) child.addObjectProperty(
                metaFactory, "parent", "TopNParent", "children", TopNParent.class,
                GenericSQLRelation::new);
        relation.setTableName("top_n_child_data");
        relation.setColumnName("parent");
        relation.setColumnType("INTEGER");
        metaFactory.register(child);
        sqlDataService.ensureSchema(context, "TopNParent");
        sqlDataService.ensureSchema(context, "TopNChild");
        sqliteDb.execute("DELETE FROM top_n_child_data");
        sqliteDb.execute("DELETE FROM top_n_parent_data");
        seedTopNFixture();
    }

    private void seedTopNFixture() {
        sqliteDb.execute("INSERT INTO top_n_parent_data VALUES (1,1,'A'),(2,1,'B'),(3,1,'C')");
        sqliteDb.execute("INSERT INTO top_n_child_data (id,version,name,state,parent) VALUES "
                + "(11,1,'same','visible',1),(12,1,'same','visible',1),(13,1,'same','visible',1),"
                + "(14,1,'hidden','hidden',1),(21,1,'same','visible',2),(22,1,'same','visible',2),"
                + "(23,1,'same','visible',2)");
    }

    @Test
    public void testConstantBootstrapIsIdempotentAndReconcilesModelChanges() throws Exception {
        SQLiteTeaQLDatabase database = new SQLiteTeaQLDatabase();
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("SchoolType");
        descriptor.setTargetType(Task.class);
        descriptor.setEntitySupplier(Task::new);
        descriptor.setParent(new EntityDescriptor());
        descriptor.with("constant", "true");

        GenericSQLProperty id = bootstrapProperty(descriptor, "id", "INTEGER", Long.class);
        id.with("candidates", "1001,1002");
        GenericSQLProperty version = bootstrapProperty(descriptor, "version", "INTEGER", Long.class);
        GenericSQLProperty code = bootstrapProperty(descriptor, "code", "VARCHAR(100)", String.class);
        code.with("identifier", "true").with("candidates", "PRIMARY,SECONDARY");
        GenericSQLProperty name = bootstrapProperty(descriptor, "name", "VARCHAR(100)", String.class);
        name.with("candidates", "Primary,Secondary");
        descriptor.setProperties(List.of(id, version, code, name));

        PortableSQLRepository<Task> repository = new PortableSQLRepository<>(descriptor, database, null);
        repository.ensureSchema(context);
        repository.ensureSchema(context);
        List<Map<String, Object>> unchanged = database.query(
                "SELECT id, version, name FROM school_type_data ORDER BY id", new Object[0]);
        assertEquals(2, unchanged.size());
        assertEquals(1L, ((Number) unchanged.get(0).get("version")).longValue());
        assertEquals(1L, ((Number) unchanged.get(1).get("version")).longValue());
        assertEquals(1003L, new IdSpaceIdGenerator(database).nextId("SchoolType"));

        name.with("candidates", "Primary School,Secondary");
        repository.ensureSchema(context);
        List<Map<String, Object>> reconciled = database.query(
                "SELECT id, version, name FROM school_type_data ORDER BY id", new Object[0]);
        assertEquals("Primary School", reconciled.get(0).get("name"));
        assertEquals(2L, ((Number) reconciled.get(0).get("version")).longValue());
        assertEquals(1L, ((Number) reconciled.get(1).get("version")).longValue());
    }

    private static GenericSQLProperty bootstrapProperty(
            EntityDescriptor owner, String name, String sqlType, Class<?> javaType) {
        GenericSQLProperty property =
                new GenericSQLProperty("school_type_data", name, sqlType);
        property.setName(name);
        property.setOwner(owner);
        property.setType(new SimplePropertyType(javaType));
        return property;
    }

    @Test
    public void testSingleDynamicAggregateIsAttachedToEachReturnedParent() {
        for (int i = 0; i < 2; i++) {
            Task task = new Task();
            task.updateTitle("Counted task " + i);
            task.updateStatus("DYNAMIC-COUNT");
            task.auditAs("create dynamic-count fixture").save(context);
        }

        TaskRequest countRequest = new TaskRequest();
        countRequest.count();
        countRequest.setPartitionProperty("id");

        TaskRequest parentRequest = new TaskRequest().filterByStatus("DYNAMIC-COUNT");
        parentRequest.addSingleAggregateDynamicProperty("selfCount", countRequest);
        SmartList<Task> tasks =
                parentRequest
                        .comment("load tasks with grouped count")
                        .purpose("verify aggregate values are attached to parent entities")
                        .executeForList(context);

        assertEquals(2, tasks.size());
        for (Task task : tasks) {
            Number count = task.getDynamicProperty("selfCount");
            assertNotNull("generated count must be attached to its parent", count);
            assertEquals(1, count.intValue());
        }
    }

    @Test
    public void testAggregateAliasesAreMappedCaseInsensitively() {
        for (int i = 0; i < 2; i++) {
            Task task = new Task();
            task.updateTitle("Alias task " + i);
            task.updateStatus("CAMEL-ALIAS");
            task.auditAs("create alias mapping fixture").save(context);
        }

        TaskRequest request = new TaskRequest().filterByStatus("CAMEL-ALIAS");
        request.count("recordCount");
        AggregationResult result = request.comment("aggregate camel-case alias")
                .purpose("verify JDBC-normalized column labels map to requested aliases")
                .aggregation(context);

        assertEquals(2, result.toNumber(0).intValue());
        assertEquals("recordCount", result.valueList().get(0).keySet().iterator().next());
    }

    @Test
    public void testMaterializedSubQueryUsesPortableInList() {
        for (int i = 0; i < 2; i++) {
            Task task = new Task();
            task.updateTitle("Nested task " + i);
            task.updateStatus("NESTED-IN");
            task.auditAs("create nested-query fixture").save(context);
        }

        TaskRequest nested = new TaskRequest() {
            @Override
            public boolean tryUseSubQuery() {
                return false;
            }
        }.filterByStatus("NESTED-IN");
        SmartList<Task> result = new TaskRequest().withIdMatching(nested)
                .comment("materialize relation-style nested query")
                .purpose("verify portable scalar IN placeholders")
                .executeForList(context);

        assertEquals(2, result.size());
    }

    @Test
    public void testNativeSubQueryExecutesPositiveAndNegativePredicatesOnSQLite() {
        for (int i = 0; i < 2; i++) {
            Task task = new Task();
            task.updateTitle("Native nested task " + i);
            task.updateStatus("NESTED-NATIVE");
            task.auditAs("create native nested-query fixture").save(context);
        }
        TaskRequest nested = new TaskRequest().filterByStatus("NESTED-NATIVE");
        nested.unlimited();
        SmartList<Task> included = new TaskRequest().withIdMatching(nested)
                .comment("execute positive SQLite subquery")
                .purpose("verify positive relation predicate")
                .executeForList(context);
        SmartList<Task> excluded = new TaskRequest().withoutIdMatching(nested)
                .comment("execute negative SQLite subquery")
                .purpose("verify negative relation predicate")
                .executeForList(context);

        assertEquals(2, included.size());
        assertTrue(excluded.stream().noneMatch(task -> "NESTED-NATIVE".equals(task.getStatus())));
    }

    public static class TaskRequest extends BaseRequest<Task> {
        public TaskRequest() {
            super(Task.class);
        }

        @Override
        public String getTypeName() {
            return "Task";
        }


        public TaskRequest filterByTitle(String title) {
            appendSearchCriteria(createBasicSearchCriteria("title", Operator.EQUAL, title));
            return this;
        }

        public TaskRequest filterByStatus(String status) {
            appendSearchCriteria(createBasicSearchCriteria("status", Operator.EQUAL, status));
            return this;
        }

        public TaskRequest withIdMatching(TaskRequest nested) {
            appendSearchCriteria(new SubQuerySearchCriteria("id", nested, "id"));
            return this;
        }

        public TaskRequest withoutIdMatching(TaskRequest nested) {
            appendSearchCriteria(SearchCriteria.not(new SubQuerySearchCriteria("id", nested, "id")));
            return this;
        }

        public TaskRequest comment(String comment) {
            super.internalComment(comment);
            return this;
        }
    }

    public static class QueryScalar extends BaseEntity {
        @Override
        public String typeName() {
            return "QueryScalar";
        }
    }

    public static class QueryGroup extends BaseEntity {
        private String name;
        public String getName() { return name; }
        @Override public Object __internalGet(String property) {
            if ("name".equals(property)) return name;
            return super.__internalGet(property);
        }
        @Override public void __internalSet(String property, Object value) {
            if ("name".equals(property)) { name = (String) value; return; }
            super.__internalSet(property, value);
        }
        @Override
        public String typeName() {
            return "QueryGroup";
        }
    }

    public static class QueryRecord extends BaseEntity {
        private Long queryGroup;
        private String name;
        public Long getQueryGroup() { return queryGroup; }
        public String getName() { return name; }
        @Override public Object __internalGet(String property) {
            if ("query_group".equals(property)) return queryGroup;
            if ("name".equals(property)) return name;
            return super.__internalGet(property);
        }
        @Override public void __internalSet(String property, Object value) {
            if ("query_group".equals(property)) {
                queryGroup = value == null ? null : ((Number) value).longValue();
                return;
            }
            if ("name".equals(property)) { name = (String) value; return; }
            super.__internalSet(property, value);
        }
        @Override
        public String typeName() {
            return "QueryRecord";
        }
    }

    public static class QueryGroupRequest extends BaseRequest<QueryGroup> {
        public QueryGroupRequest() { super(QueryGroup.class); }
        @Override public String getTypeName() { return "QueryGroup"; }
        public QueryGroupRequest where(String field, Operator op, Object... values) {
            appendSearchCriteria(createBasicSearchCriteria(field, op, values));
            return this;
        }
        public QueryGroupRequest withRecordsMatching(QueryRecordRequest child) {
            appendSearchCriteria(new SubQuerySearchCriteria("id", child, "query_group"));
            return this;
        }
        public QueryGroupRequest withoutRecordsMatching(QueryRecordRequest child) {
            appendSearchCriteria(SearchCriteria.not(
                    new SubQuerySearchCriteria("id", child, "query_group")));
            return this;
        }
        public QueryGroupRequest comment(String value) { super.internalComment(value); return this; }
    }

    public static class QueryRecordRequest extends BaseRequest<QueryRecord> {
        public QueryRecordRequest() { super(QueryRecord.class); }
        @Override public String getTypeName() { return "QueryRecord"; }
        public QueryRecordRequest where(String field, Operator op, Object... values) {
            appendSearchCriteria(createBasicSearchCriteria(field, op, values));
            return this;
        }
        public QueryRecordRequest withGroupMatching(QueryGroupRequest child) {
            appendSearchCriteria(new SubQuerySearchCriteria("query_group", child, "id"));
            return this;
        }
        public QueryRecordRequest withoutGroupMatching(QueryGroupRequest child) {
            appendSearchCriteria(SearchCriteria.not(
                    new SubQuerySearchCriteria("query_group", child, "id")));
            return this;
        }
        public QueryRecordRequest comment(String value) { super.internalComment(value); return this; }
    }

    @Test
    public void testCompleteForwardAndReverseRelationFixtureIncludingOrphanNullOnSQLite() {
        EntityDescriptor group = relationEntity(
                "QueryGroup", QueryGroup.class, QueryGroup::new, "query_group_data",
                List.of(new Object[] {"id", "INTEGER", Long.class},
                        new Object[] {"name", "VARCHAR(100)", String.class},
                        new Object[] {"version", "INTEGER", Long.class}));
        EntityDescriptor record = relationEntity(
                "QueryRecord", QueryRecord.class, QueryRecord::new, "query_record_data",
                List.of(new Object[] {"id", "INTEGER", Long.class},
                        new Object[] {"query_group", "INTEGER", Long.class},
                        new Object[] {"name", "VARCHAR(100)", String.class},
                        new Object[] {"version", "INTEGER", Long.class}));
        metaFactory.register(group);
        metaFactory.register(record);
        sqlDataService.ensureSchema(context, "QueryGroup");
        sqlDataService.ensureSchema(context, "QueryRecord");
        sqliteDb.execute("DELETE FROM query_group_data");
        sqliteDb.execute("DELETE FROM query_record_data");
        sqliteDb.execute("INSERT INTO query_group_data VALUES "
                + "(1,'Core',1),(2,'Other',1),(3,'Empty',1)");
        sqliteDb.execute("INSERT INTO query_record_data VALUES "
                + "(11,1,'included',1),(12,2,'excluded',1),(13,NULL,'orphan',1)");

        QueryGroupRequest core = new QueryGroupRequest().where("name", Operator.EQUAL, "Core");
        assertNames(List.of("included"), new QueryRecordRequest().withGroupMatching(core));
        assertNames(List.of("excluded"), new QueryRecordRequest().withoutGroupMatching(core));
        assertNames(List.of("included", "excluded"),
                new QueryRecordRequest().where("query_group", Operator.IS_NOT_NULL));
        assertNames(List.of("orphan"),
                new QueryRecordRequest().where("query_group", Operator.IS_NULL));

        QueryRecordRequest allRecords = new QueryRecordRequest();
        allRecords.unlimited();
        assertNames(List.of("Core", "Other"),
                new QueryGroupRequest().withRecordsMatching(allRecords));
        assertNames(List.of("Empty"),
                new QueryGroupRequest().withoutRecordsMatching(allRecords));
    }

    private static <T extends BaseEntity> EntityDescriptor relationEntity(
            String type, Class<T> targetType, java.util.function.Supplier<T> supplier,
            String table, List<Object[]> definitions) {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType(type);
        descriptor.setTargetType(targetType);
        descriptor.setEntitySupplier(supplier);
        descriptor.setDataService("sql");
        List<PropertyDescriptor> properties = new ArrayList<>();
        for (Object[] definition : definitions) {
            GenericSQLProperty property = new GenericSQLProperty(
                    table, (String) definition[0], (String) definition[1]);
            property.setName((String) definition[0]);
            property.setOwner(descriptor);
            property.setType(new SimplePropertyType((Class<?>) definition[2]));
            properties.add(property);
        }
        descriptor.setProperties(properties);
        return descriptor;
    }

    private static void assertNames(List<String> expected, BaseRequest<?> request) {
        request.addOrderByAscending("id");
        ExecutableRequest<?> executable;
        if (request instanceof QueryGroupRequest groupRequest) {
            executable = groupRequest.comment("execute complete relation predicate")
                    .purpose("retain complete relation fixture evidence");
        } else {
            executable = ((QueryRecordRequest) request)
                    .comment("execute complete relation predicate")
                    .purpose("retain complete relation fixture evidence");
        }
        SmartList<?> rows = executable.executeForList(context);
        assertEquals(expected, rows.stream().map(row -> (String) ((Entity) row).getProperty("name"))
                .toList());
    }

    public static class QueryScalarRequest extends BaseRequest<QueryScalar> {
        public QueryScalarRequest() {
            super(QueryScalar.class);
        }

        @Override
        public String getTypeName() {
            return "QueryScalar";
        }

        public QueryScalarRequest where(String field, Operator operator, Object... values) {
            appendSearchCriteria(createBasicSearchCriteria(field, operator, values));
            return this;
        }

        public QueryScalarRequest comment(String comment) {
            super.internalComment(comment);
            return this;
        }
    }

    @Test
    public void testCompleteScalarFixtureIncludingNullableBooleanExecutesOnSQLite() {
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("QueryScalar");
        descriptor.setTargetType(QueryScalar.class);
        descriptor.setEntitySupplier(QueryScalar::new);
        descriptor.setDataService("sql");
        List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(scalarProperty(descriptor, "id", "INTEGER", Long.class));
        properties.add(scalarProperty(descriptor, "required_text", "VARCHAR(100)", String.class));
        properties.add(scalarProperty(descriptor, "optional_text", "VARCHAR(100)", String.class));
        properties.add(scalarProperty(descriptor, "required_integer", "INTEGER", Long.class));
        properties.add(scalarProperty(descriptor, "optional_long", "INTEGER", Long.class));
        properties.add(scalarProperty(descriptor, "required_decimal", "NUMERIC", BigDecimal.class));
        properties.add(scalarProperty(descriptor, "required_float", "REAL", Float.class));
        properties.add(scalarProperty(descriptor, "required_double", "REAL", Double.class));
        properties.add(scalarProperty(descriptor, "required_date", "DATE", java.time.LocalDate.class));
        properties.add(scalarProperty(descriptor, "required_time", "INTEGER", Long.class));
        properties.add(scalarProperty(descriptor, "required_timestamp", "TIMESTAMP", Long.class));
        properties.add(scalarProperty(descriptor, "active", "BOOLEAN", Boolean.class));
        properties.add(scalarProperty(descriptor, "reviewed", "BOOLEAN", Boolean.class));
        properties.add(scalarProperty(descriptor, "version", "INTEGER", Long.class));
        descriptor.setProperties(properties);
        metaFactory.register(descriptor);
        sqlDataService.ensureSchema(context, "QueryScalar");
        sqliteDb.execute("DELETE FROM query_scalar_data");
        sqliteDb.execute(
                "INSERT INTO query_scalar_data VALUES "
                        + "(1,'Alpha','optional',42,42000000000,42.125,42.5,42.75,'2026-08-29',34200000,1777632600000,1,0,1),"
                        + "(2,'Beta',NULL,7,NULL,7.500,7.5,7.75,'2026-08-30',36000000,1777720400000,0,NULL,1),"
                        + "(3,'Gamma','tail',99,99000000000,99.875,99.5,99.75,'2026-08-31',37800000,1777808200000,1,1,1)");

        assertEquals(1, scalarCount("required_text", Operator.EQUAL, "Alpha"));
        assertEquals(2, scalarCount("required_text", Operator.NOT_EQUAL, "Alpha"));
        assertEquals(2, scalarCount("required_text", Operator.IN, "Alpha", "Gamma"));
        assertEquals(1, scalarCount("required_text", Operator.BEGIN_WITH, "Al"));
        assertEquals(1, scalarCount("required_text", Operator.END_WITH, "ma"));
        assertEquals(1, scalarCount("required_text", Operator.CONTAIN, "et"));
        assertEquals(2, scalarCount("required_integer", Operator.BETWEEN, 40L, 100L));
        assertEquals(1, scalarCount("required_decimal", Operator.GREATER_THAN, new BigDecimal("50")));
        assertEquals(1, scalarCount("required_float", Operator.LESS_THAN_OR_EQUAL, 7.5F));
        assertEquals(1, scalarCount("required_double", Operator.GREATER_THAN_OR_EQUAL, 99.75D));
        assertEquals(2, scalarCount("required_date", Operator.BETWEEN,
                java.time.LocalDate.parse("2026-08-30"), java.time.LocalDate.parse("2026-08-31")));
        assertEquals(1, scalarCount("required_time", Operator.GREATER_THAN, 36_000_000L));
        assertEquals(2, scalarCount("required_timestamp", Operator.LESS_THAN, 1_777_750_000_000L));
        assertEquals(1, scalarCount("optional_text", Operator.IS_NULL));
        assertEquals(2, scalarCount("optional_long", Operator.IS_NOT_NULL));
        assertEquals(1, scalarCount("active", Operator.EQUAL, false));
        assertEquals(1, scalarCount("reviewed", Operator.EQUAL, true));
        assertEquals(1, scalarCount("reviewed", Operator.EQUAL, false));
        assertEquals(1, scalarCount("reviewed", Operator.IS_NULL));
    }

    private static GenericSQLProperty scalarProperty(
            EntityDescriptor owner, String name, String sqlType, Class<?> javaType) {
        GenericSQLProperty property = new GenericSQLProperty("query_scalar_data", name, sqlType);
        property.setName(name);
        property.setOwner(owner);
        property.setType(new SimplePropertyType(javaType));
        return property;
    }

    private static int scalarCount(String field, Operator operator, Object... values) {
        QueryScalarRequest request = new QueryScalarRequest().where(field, operator, values);
        request.count();
        AggregationResult result = request.comment("execute complete scalar predicate")
                .purpose("retain Query conformance evidence")
                .aggregation(context);
        return result.toNumber(0).intValue();
    }

    // ── SQLite TeaQLDatabase Implementation ────────────────

    public static class SQLiteTeaQLDatabase implements TeaQLDatabase {
        private final Connection connection;
        private final List<String> queryTrace = new ArrayList<>();

        public SQLiteTeaQLDatabase() throws Exception {
            this.connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        }

        @Override
        public List<Map<String, Object>> query(String sql, Object[] args) {
            System.out.println("[SQL-QUERY] " + sql + " | args: " + Arrays.toString(args));
            queryTrace.add(sql + " | args: " + Arrays.toString(args));
            List<Map<String, Object>> results = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    bindSqlite(stmt, i + 1, args[i]);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= cols; i++) {
                            row.put(meta.getColumnLabel(i).toLowerCase(Locale.ROOT), rs.getObject(i));
                        }
                        results.add(row);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return results;
        }

        public void clearQueryTrace() {
            queryTrace.clear();
        }

        public List<String> queryTrace() {
            return List.copyOf(queryTrace);
        }

        @Override
        public int executeUpdate(String sql, Object[] args) {
            System.out.println("[SQL-UPDATE] " + sql + " | args: " + Arrays.toString(args));
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    bindSqlite(stmt, i + 1, args[i]);
                }
                return stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
            System.out.println("[SQL-BATCH] " + sql + " | batch count: " + batchArgs.size());
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (Object[] args : batchArgs) {
                    for (int i = 0; i < args.length; i++) {
                        bindSqlite(stmt, i + 1, args[i]);
                    }
                    stmt.addBatch();
                }
                return stmt.executeBatch();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void execute(String sql) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void executeInTransaction(Runnable action) {
            try {
                connection.setAutoCommit(false);
                try {
                    action.run();
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private static void bindSqlite(PreparedStatement statement, int index, Object value)
                throws SQLException {
            if (value instanceof java.time.LocalDate date) {
                statement.setString(index, date.toString());
            } else if (value instanceof java.time.LocalDateTime dateTime) {
                statement.setString(index, java.sql.Timestamp.valueOf(dateTime).toString());
            } else if (value instanceof java.time.LocalTime time) {
                statement.setString(index, time.toString());
            } else {
                statement.setObject(index, value);
            }
        }

        @Override
        public List<Map<String, Object>> getTableColumns(String tableName) {
            List<Map<String, Object>> columns = new ArrayList<>();
            String sql = "PRAGMA table_info(" + tableName + ")";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> col = new HashMap<>();
                    col.put("column_name", rs.getString("name"));
                    columns.add(col);
                }
            } catch (SQLException e) {
                // table doesn't exist yet
            }
            return columns;
        }
    }

    // ── Setup metadata and runtime ───────────────────────

    private static SimpleEntityMetaFactory metaFactory;
    private static SQLiteTeaQLDatabase sqliteDb;
    private static PortableSQLDataService sqlDataService;
    private static UserContext context;

    @BeforeClass
    public static void setup() throws Exception {
        metaFactory = new SimpleEntityMetaFactory();

        // Describe Task Entity mapped to SQL DB
        EntityDescriptor taskDescriptor = new EntityDescriptor();
        taskDescriptor.setType("Task");
        taskDescriptor.setTargetType(Task.class);
        taskDescriptor.setEntitySupplier(Task::new);
        taskDescriptor.setDataService("sql");

        List<PropertyDescriptor> props = new ArrayList<>();

        // SQLite columns mapping using GenericSQLProperty constructor
        GenericSQLProperty idProp = new GenericSQLProperty("task_data", "id", "INTEGER");
        idProp.setName("id");
        idProp.setOwner(taskDescriptor);
        idProp.setType(new SimplePropertyType(Long.class));
        props.add(idProp);

        GenericSQLProperty versionProp = new GenericSQLProperty("task_data", "version", "INTEGER");
        versionProp.setName("version");
        versionProp.setOwner(taskDescriptor);
        versionProp.setType(new SimplePropertyType(Long.class));
        props.add(versionProp);

        GenericSQLProperty titleProp = new GenericSQLProperty("task_data", "title", "VARCHAR(100)");
        titleProp.setName("title");
        titleProp.setOwner(taskDescriptor);
        titleProp.setType(new SimplePropertyType(String.class));
        props.add(titleProp);

        GenericSQLProperty statusProp = new GenericSQLProperty("task_data", "status", "VARCHAR(100)");
        statusProp.setName("status");
        statusProp.setOwner(taskDescriptor);
        statusProp.setType(new SimplePropertyType(String.class));
        props.add(statusProp);

        taskDescriptor.setProperties(props);

        metaFactory.register(taskDescriptor);
        EntityMetaFactory.registerGlobal(metaFactory);

        // Build SQLite Database and Portable SQL Service
        sqliteDb = new SQLiteTeaQLDatabase();
        sqlDataService = new PortableSQLDataService("sql", sqliteDb, metaFactory);

        AtomicLong idGen = new AtomicLong(200);
        InternalIdGenerationService idService = (c, entity) -> idGen.getAndIncrement();

        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(metaFactory)
                .dataService("sql", sqlDataService)
                .idGenerationService(idService)
                .build();

        context = new DefaultUserContext(runtime);
        context.putAttribute("ensureTable", true); // enable schema generation

        // Generate schema
        sqlDataService.ensureSchema(context, "Task");
    }

    @Test
    public void testContinuousPageFetchUsesSeekForTheNextIdDescendingPage() {
        for (int i = 0; i < 25; i++) {
            Task task = new Task();
            task.updateTitle("Browse task " + i);
            task.updateStatus("CONTINUOUS-PAGE");
            task.auditAs("seed continuous page fixture").save(context);
        }

        TaskRequest first = new TaskRequest().filterByStatus("CONTINUOUS-PAGE");
        first.addOrderByDescending("id");
        first.offset(0, 10);
        first.optimizeForContinuousPageFetch("continuous-page-test", 60);
        SmartList<Task> firstPage = first.comment("load first browse page")
                .purpose("verify continuous pagination")
                .executeForList(context);
        assertEquals(10, firstPage.size());
        assertEquals("OFFSET_FALLBACK:FIRST_PAGE",
                context.getAttribute(PortableSQLRepository.CONTINUOUS_PAGE_PLAN));

        sqliteDb.clearQueryTrace();
        TaskRequest second = new TaskRequest().filterByStatus("CONTINUOUS-PAGE");
        second.addOrderByDescending("id");
        second.offset(10, 10);
        second.optimizeForContinuousPageFetch("continuous-page-test", 60);
        SmartList<Task> secondPage = second.comment("load next browse page")
                .purpose("verify continuous pagination")
                .executeForList(context);

        assertEquals(10, secondPage.size());
        assertEquals("CURSOR_SEEK", context.getAttribute(PortableSQLRepository.CONTINUOUS_PAGE_PLAN));
        assertNotNull(context.getAttribute(PortableSQLRepository.CONTINUOUS_PAGE_CURSOR_ID));
        assertTrue(firstPage.get(9).getId() > secondPage.get(0).getId());
        String executed = sqliteDb.queryTrace().get(0);
        assertTrue(executed, executed.contains("<"));
        assertTrue(executed, executed.contains("OFFSET 0"));
    }

    @Test
    public void testContinuousPageFetchUsesSeekForTheNextIdAscendingPage() {
        seedContinuousPageTasks("CONTINUOUS-PAGE-ASC", 25);

        SmartList<Task> firstPage = continuousPage("CONTINUOUS-PAGE-ASC", false, 0);
        sqliteDb.clearQueryTrace();
        SmartList<Task> secondPage = continuousPage("CONTINUOUS-PAGE-ASC", false, 10);

        assertEquals(10, secondPage.size());
        assertEquals("CURSOR_SEEK", context.getAttribute(PortableSQLRepository.CONTINUOUS_PAGE_PLAN));
        assertTrue(firstPage.get(9).getId() < secondPage.get(0).getId());
        assertTrue(sqliteDb.queryTrace().get(0), sqliteDb.queryTrace().get(0).contains(">"));
    }

    @Test
    public void testContinuousPageFetchFallsBackWhenCheckpointIsMissing() {
        seedContinuousPageTasks("CONTINUOUS-PAGE-MISS", 25);

        SmartList<Task> page = continuousPage("CONTINUOUS-PAGE-MISS", true, 10);

        assertEquals(10, page.size());
        assertEquals("OFFSET_FALLBACK:CACHE_MISS",
                context.getAttribute(PortableSQLRepository.CONTINUOUS_PAGE_PLAN));
    }

    @Test
    public void testContinuousPageFetchFallsBackWhenCursorStoreIsUnavailable() {
        seedContinuousPageTasks("CONTINUOUS-PAGE-STORE-FAIL", 25);
        ContinuousPageCursorStore unavailable = new ContinuousPageCursorStore() {
            @Override
            public Optional<ContinuousPageCursor> get(String queryKey, long targetOffset) {
                throw new IllegalStateException("simulated cursor store outage");
            }

            @Override
            public void put(String queryKey, ContinuousPageCursor cursor) {
                throw new IllegalStateException("simulated cursor store outage");
            }

            @Override
            public void invalidate(String queryKey) {
                throw new IllegalStateException("simulated cursor store outage");
            }
        };
        context.putAttribute(ContinuousPageCursorStore.class.getName(), unavailable);
        try {
            SmartList<Task> firstPage = continuousPage("CONTINUOUS-PAGE-STORE-FAIL", true, 0);
            assertEquals(10, firstPage.size());
            assertEquals("OFFSET_FALLBACK:STORE_UNAVAILABLE",
                    context.getAttribute(PortableSQLRepository.CONTINUOUS_PAGE_PLAN));

            SmartList<Task> secondPage = continuousPage("CONTINUOUS-PAGE-STORE-FAIL", true, 10);
            assertEquals(10, secondPage.size());
            assertEquals("OFFSET_FALLBACK:STORE_UNAVAILABLE",
                    context.getAttribute(PortableSQLRepository.CONTINUOUS_PAGE_PLAN));
        } finally {
            context.putAttribute(ContinuousPageCursorStore.class.getName(), null);
        }
    }

    private void seedContinuousPageTasks(String status, int count) {
        for (int i = 0; i < count; i++) {
            Task task = new Task();
            task.updateTitle("Browse task " + i);
            task.updateStatus(status);
            task.auditAs("seed continuous page fixture").save(context);
        }
    }

    private SmartList<Task> continuousPage(String status, boolean descending, int offset) {
        TaskRequest request = new TaskRequest().filterByStatus(status);
        if (descending) request.addOrderByDescending("id");
        else request.addOrderBy("id", true);
        request.offset(offset, 10);
        request.optimizeForContinuousPageFetch("continuous-page-test-" + status, 60);
        return request.comment("load browse page")
                .purpose("verify continuous pagination")
                .executeForList(context);
    }

    @Test
    public void testIdSetPaginationBuildsOnceJumpsAndReturnsExactCount() {
        seedContinuousPageTasks("ID-SET-PAGE", 5);
        sqliteDb.clearQueryTrace();

        TaskRequest jumpedRequest = new TaskRequest().filterByStatus("ID-SET-PAGE");
        jumpedRequest.addOrderByDescending("id");
        jumpedRequest.optimizePaginationWithIdSet("id-set-page", 60, 100);
        SmartList<Task> jumped = jumpedRequest.comment("jump to retained ID page")
                .purpose("verify ID set pagination").executeForPage(context, 2, 2);

        assertEquals(2, jumped.size());
        assertEquals(5, jumped.getTotalCount());
        assertTrue(jumped.get(0).getId() > jumped.get(1).getId());
        assertEquals("ID_SET_BUILD", context.getAttribute(PortableSQLRepository.ID_SET_PLAN));
        assertEquals("EXACT", context.getAttribute(PortableSQLRepository.ID_SET_COUNT_ACCURACY));
        assertEquals(2, sqliteDb.queryTrace().size());
        assertTrue(sqliteDb.queryTrace().stream().noneMatch(sql -> sql.toLowerCase().contains("count(")));

        sqliteDb.clearQueryTrace();
        TaskRequest firstRequest = new TaskRequest().filterByStatus("ID-SET-PAGE");
        firstRequest.addOrderByDescending("id");
        firstRequest.optimizePaginationWithIdSet("id-set-page", 60, 100);
        SmartList<Task> first = firstRequest.comment("load retained first page")
                .purpose("verify ID set cache hit").executeForPage(context, 0, 2);
        assertEquals(5, first.getTotalCount());
        assertEquals("ID_SET_HIT", context.getAttribute(PortableSQLRepository.ID_SET_PLAN));
        assertEquals(1, sqliteDb.queryTrace().size());
        assertTrue(first.get(0).getId() > first.get(1).getId());
        assertTrue(first.get(1).getId() > jumped.get(0).getId());
    }

    @Test
    public void testIdSetPaginationEmptyOverflowStoreFailureAndUnsupportedShape() {
        TaskRequest emptyRequest = new TaskRequest().filterByStatus("ID-SET-EMPTY");
        emptyRequest.addOrderByDescending("id");
        emptyRequest.offset(0, 2).optimizePaginationWithIdSet("empty", 60, 10);
        assertTrue(emptyRequest.comment("empty ID set").purpose("cache empty result")
                .executeForList(context).isEmpty());
        assertEquals("ID_SET_BUILD", context.getAttribute(PortableSQLRepository.ID_SET_PLAN));
        assertEquals(0L, context.getAttribute(PortableSQLRepository.ID_SET_COUNT));
        sqliteDb.clearQueryTrace();
        TaskRequest emptyHit = new TaskRequest().filterByStatus("ID-SET-EMPTY");
        emptyHit.addOrderByDescending("id");
        emptyHit.offset(0, 2).optimizePaginationWithIdSet("empty", 60, 10);
        assertTrue(emptyHit.comment("empty ID set hit").purpose("reuse empty result")
                .executeForList(context).isEmpty());
        assertEquals("ID_SET_HIT", context.getAttribute(PortableSQLRepository.ID_SET_PLAN));
        assertTrue(sqliteDb.queryTrace().isEmpty());

        seedContinuousPageTasks("ID-SET-OVERFLOW", 5);
        TaskRequest overflow = new TaskRequest().filterByStatus("ID-SET-OVERFLOW");
        overflow.addOrderByDescending("id");
        overflow.optimizePaginationWithIdSet("overflow", 60, 3);
        SmartList<Task> overflowPage = overflow.comment("overflow fallback")
                .purpose("preserve ordinary page semantics").executeForPage(context, 0, 2);
        assertEquals(2, overflowPage.size());
        assertEquals(5, overflowPage.getTotalCount());
        assertEquals("ID_SET_FALLBACK_LIMIT_EXCEEDED",
                context.getAttribute(PortableSQLRepository.ID_SET_PLAN));
        assertEquals("LOWER_BOUND", context.getAttribute(PortableSQLRepository.ID_SET_COUNT_ACCURACY));
        assertEquals(4L, context.getAttribute(PortableSQLRepository.ID_SET_COUNT));

        IdSetStore unavailable = new IdSetStore() {
            @Override public Optional<RetainedIdSet> get(String key) { throw new IllegalStateException("down"); }
            @Override public void put(RetainedIdSet value) { throw new IllegalStateException("down"); }
            @Override public void invalidate(String key) { throw new IllegalStateException("down"); }
        };
        context.putAttribute(IdSetStore.class.getName(), unavailable);
        try {
            TaskRequest fallback = new TaskRequest().filterByStatus("ID-SET-OVERFLOW");
            fallback.addOrderByDescending("id"); fallback.offset(0, 2);
            fallback.optimizePaginationWithIdSet("store-down", 60, 10);
            assertEquals(2, fallback.comment("store fallback").purpose("preserve rows")
                    .executeForList(context).size());
            assertEquals("ID_SET_FALLBACK_STORE_UNAVAILABLE",
                    context.getAttribute(PortableSQLRepository.ID_SET_PLAN));
        } finally { context.putAttribute(IdSetStore.class.getName(), null); }

        TaskRequest unsupported = new TaskRequest().filterByStatus("ID-SET-OVERFLOW");
        unsupported.setPartitionProperty("status"); unsupported.offset(0, 2);
        unsupported.optimizePaginationWithIdSet("unsupported", 60, 10);
        unsupported.comment("unsupported ID set shape").purpose("verify fallback")
                .executeForList(context);
        assertEquals("ID_SET_FALLBACK_UNSUPPORTED_SHAPE",
                context.getAttribute(PortableSQLRepository.ID_SET_PLAN));
    }

    @Test
    public void testIdSetPaginationTtlIsolationTieBreakerAndDeletedSnapshot() throws Exception {
        seedContinuousPageTasks("ID-SET-LIFECYCLE", 5);
        context.putAttribute("userId", "alice");
        TaskRequest first = new TaskRequest().filterByStatus("ID-SET-LIFECYCLE");
        first.addOrderByDescending("status");
        first.offset(0, 2).optimizePaginationWithIdSet("lifecycle", 1, 10);
        SmartList<Task> firstRows = first.comment("build scoped ID set")
                .purpose("verify lifecycle").executeForList(context);
        assertEquals(2, firstRows.size());
        assertTrue(sqliteDb.queryTrace().stream().anyMatch(sql -> sql.contains("status DESC, id ASC")));

        context.putAttribute("userId", "bob");
        TaskRequest isolated = new TaskRequest().filterByStatus("ID-SET-LIFECYCLE");
        isolated.addOrderByDescending("status");
        isolated.offset(0, 2).optimizePaginationWithIdSet("lifecycle", 1, 10);
        isolated.comment("build isolated ID set").purpose("verify principal isolation")
                .executeForList(context);
        assertEquals("ID_SET_BUILD", context.getAttribute(PortableSQLRepository.ID_SET_PLAN));

        context.withActiveRoot(new ContextEntityRef("Platform", 1L));
        TaskRequest rooted = new TaskRequest().filterByStatus("ID-SET-LIFECYCLE");
        rooted.addOrderByDescending("status");
        rooted.offset(0, 2).optimizePaginationWithIdSet("lifecycle", 1, 10);
        rooted.comment("build root-scoped ID set").purpose("verify root isolation")
                .executeForList(context);
        assertEquals("ID_SET_BUILD", context.getAttribute(PortableSQLRepository.ID_SET_PLAN));
        context.putAttribute(UserContext.TEAQL_ACTIVE_ROOT, null);

        Thread.sleep(1_050);
        TaskRequest expired = new TaskRequest().filterByStatus("ID-SET-LIFECYCLE");
        expired.addOrderByDescending("status");
        expired.offset(0, 2).optimizePaginationWithIdSet("lifecycle", 1, 10);
        expired.comment("rebuild expired ID set").purpose("verify TTL")
                .executeForList(context);
        assertEquals("ID_SET_BUILD", context.getAttribute(PortableSQLRepository.ID_SET_PLAN));

        TaskRequest snapshot = new TaskRequest().filterByStatus("ID-SET-LIFECYCLE");
        snapshot.addOrderByDescending("id");
        snapshot.offset(0, 2).optimizePaginationWithIdSet("snapshot", 60, 10);
        snapshot.comment("build deletion snapshot").purpose("verify no shifting")
                .executeForList(context);
        TaskRequest beforeDelete = new TaskRequest().filterByStatus("ID-SET-LIFECYCLE");
        beforeDelete.addOrderByDescending("id");
        beforeDelete.offset(2, 2).optimizePaginationWithIdSet("snapshot", 60, 10);
        SmartList<Task> expected = beforeDelete.comment("read retained page")
                .purpose("capture retained member").executeForList(context);
        assertEquals(2, expected.size());
        sqliteDb.executeUpdate("DELETE FROM task_data WHERE id = ?", new Object[] {expected.get(0).getId()});
        TaskRequest afterDelete = new TaskRequest().filterByStatus("ID-SET-LIFECYCLE");
        afterDelete.addOrderByDescending("id");
        afterDelete.offset(2, 2).optimizePaginationWithIdSet("snapshot", 60, 10);
        SmartList<Task> retained = afterDelete.comment("read page after deletion")
                .purpose("do not shift another ID").executeForList(context);
        assertEquals(1, retained.size());
        assertEquals(expected.get(1).getId(), retained.get(0).getId());
        context.putAttribute("userId", null);
    }

    @Test
    public void testIdSetPaginationCoalescesConcurrentMisses() throws Exception {
        seedContinuousPageTasks("ID-SET-CONCURRENT", 5);
        InMemoryIdSetStore delegate = new InMemoryIdSetStore();
        AtomicInteger puts = new AtomicInteger();
        AtomicInteger gets = new AtomicInteger();
        CountDownLatch initialGets = new CountDownLatch(2);
        IdSetStore store = new IdSetStore() {
            @Override public Optional<RetainedIdSet> get(String key) {
                if (gets.incrementAndGet() <= 2) {
                    initialGets.countDown();
                    try { assertTrue(initialGets.await(5, TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { throw new RuntimeException(e); }
                }
                return delegate.get(key);
            }
            @Override public void put(RetainedIdSet value) { puts.incrementAndGet(); delegate.put(value); }
            @Override public void invalidate(String key) { delegate.invalidate(key); }
        };
        TeaQLRuntime runtime = ((DefaultUserContext) context).getRuntime();
        UserContext firstContext = new DefaultUserContext(runtime);
        UserContext secondContext = new DefaultUserContext(runtime);
        firstContext.putAttribute(IdSetStore.class.getName(), store);
        secondContext.putAttribute(IdSetStore.class.getName(), store);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("id-set-" + gets.get());
            return thread;
        });
        try {
            var firstFuture = executor.submit(() -> executeConcurrentIdSet(firstContext));
            var secondFuture = executor.submit(() -> executeConcurrentIdSet(secondContext));
            assertEquals(2, firstFuture.get(10, TimeUnit.SECONDS).size());
            assertEquals(2, secondFuture.get(10, TimeUnit.SECONDS).size());
            assertEquals(1, puts.get());
        } finally { executor.shutdownNow(); }
    }

    private SmartList<Task> executeConcurrentIdSet(UserContext executionContext) {
        TaskRequest request = new TaskRequest().filterByStatus("ID-SET-CONCURRENT");
        request.addOrderByDescending("id"); request.offset(0, 2);
        request.optimizePaginationWithIdSet("concurrent", 60, 10);
        return request.comment("concurrent ID set").purpose("verify single flight")
                .executeForList(executionContext);
    }

    @Test
    public void testPortableSQLDatabaseWorkflow() {
        // 1. Create and Save Tasks
        Task task1 = new Task();
        task1.updateTitle("Assemble Engine");
        task1.updateStatus("TODO");
        task1.auditAs("save").save(context);

        assertNotNull("ID should be generated automatically", task1.getId());
        assertEquals(Long.valueOf(200), task1.getId());
        assertEquals("Status should transition to PERSISTED", EntityStatus.PERSISTED, task1.get$status());

        Task task2 = new Task();
        task2.updateTitle("Verify Engine Parts");
        task2.updateStatus("TODO");
        task2.auditAs("save").save(context);
        assertEquals(Long.valueOf(201), task2.getId());

        // 2. Query Tasks by criteria
        TaskRequest req = new TaskRequest().filterByTitle("Assemble Engine");
        SmartList<Task> resultList = req.comment("test").purpose("test").executeForList(context);
        assertEquals(1, resultList.size());
        assertEquals("Assemble Engine", resultList.get(0).getTitle());

        // Test filter no results
        TaskRequest reqEmpty = new TaskRequest().filterByTitle("Unknown Task");
        assertTrue(reqEmpty.comment("test").purpose("test").executeForList(context).isEmpty());

        // 3. Update task
        task1.updateStatus("DONE");
        task1.auditAs("save").save(context);

        TaskRequest reqDone = new TaskRequest().filterByStatus("DONE");
        SmartList<Task> resultDone = reqDone.comment("test").purpose("test").executeForList(context);
        assertEquals(1, resultDone.size());
        assertEquals("Assemble Engine", resultDone.get(0).getTitle());

        // 4. Delete task
        task1.markForDeletion().auditAs("delete").save(context);

        SmartList<Task> resultAfterDelete = new TaskRequest().filterByStatus("DONE").comment("test").purpose("test").executeForList(context);
        assertTrue("Task should be removed from DB", resultAfterDelete.isEmpty());
    }
}
