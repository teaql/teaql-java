package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class SqliteIntegrationTest {

    @Test
    public void localDynamicSearchPreservesTrustedScopeInSqlite() {
        for (String scope : new String[] {"SEARCH-SCOPE-A", "SEARCH-SCOPE-B"}) {
            for (int i = 0; i < 3; i++) {
                Task task = new Task();
                task.updateTitle("dynamic-search-match");
                task.updateStatus(scope);
                task.auditAs("seed scoped dynamic search counterexamples").save(context);
            }
        }
        TaskRequest request = new TaskRequest().filterByStatus("SEARCH-SCOPE-A");
        request.setSize(2);
        request.addOrderBy("id", false);
        int originalHardLimit = request.hardLimit();
        var warnings = new ArrayList<io.teaql.query.json.LocalDynamicSearch.Warning>();
        var models = java.util.Map.of("Task", new io.teaql.query.json.LocalDynamicSearch.Model(
                java.util.Map.of("id", "integer", "title", "string", "status", "string"), java.util.Map.of()));
        io.teaql.query.json.LocalDynamicSearch.merge(request,
                "{\"filter\":{\"title\":\"dynamic-search-match\",\"removed\":\"SECRET\"},"
                        + "\"orderBy\":[{\"field\":\"removed\",\"direction\":\"asc\"}]}",
                models, filter -> request.createBasicSearchCriteria(filter.fieldPath(), Operator.EQUAL, filter.value().textValue()),
                order -> new OrderBy(order.fieldPath(), order.direction().toUpperCase(java.util.Locale.ROOT)), warnings::add);
        SmartList<Task> rows = request.comment("what: scoped dynamic search")
                .purpose("why: verify unknown clauses preserve server scope").executeForList(context);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(task -> "SEARCH-SCOPE-A".equals(task.getStatus())));
        assertTrue(rows.get(0).getId() > rows.get(1).getId());
        assertEquals(originalHardLimit, request.hardLimit());
        assertEquals(2, warnings.size());
    }

    private static UserContext context;
    private static TeaQLRuntime runtime;
    private static JdbcSqlExecutor jdbcSqlExecutor;

    public static class Task extends BaseEntity {
        public String title;
        public String status;

        public String getTitle() { return title; }
        public Task updateTitle(String title) {
            handleUpdate("title", this.title, title);
            this.title = title;
            return this;
        }

        public String getStatus() { return status; }
        public Task updateStatus(String status) {
            handleUpdate("status", this.status, status);
            this.status = status;
            return this;
        }

        @Override
        public String typeName() { return "Task"; }

        @Override
        public void __internalSet(String property, Object value) {
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

    public static class TaskRequest extends BaseRequest<Task> {
        public TaskRequest() { super(Task.class); }

        @Override
        public String getTypeName() { return "Task"; }

        public TaskRequest filterByTitle(String title) {
            appendSearchCriteria(createBasicSearchCriteria("title", Operator.EQUAL, title));
            return this;
        }

        public TaskRequest filterByStatus(String status) {
            appendSearchCriteria(createBasicSearchCriteria("status", Operator.EQUAL, status));
            return this;
        }

        public TaskRequest comment(String comment) {
            internalComment(comment);
            return this;
        }
    }

    private static class SimpleDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;

        public SimpleDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override public PrintWriter getLogWriter() throws SQLException { return null; }
        @Override public void setLogWriter(PrintWriter out) throws SQLException {}
        @Override public void setLoginTimeout(int seconds) throws SQLException {}
        @Override public int getLoginTimeout() throws SQLException { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
    }

    @BeforeClass
    public static void setup() throws Exception {
        // Use embedded sqlite
        String url = "jdbc:sqlite:teaql_test.db";
        String user = "";
        String password = "";

        SimpleEntityMetaFactory metaFactory = new SimpleEntityMetaFactory();

        SQLEntityDescriptor taskDescriptor = new SQLEntityDescriptor();
        taskDescriptor.setType("Task");
        taskDescriptor.setTargetType(Task.class);
        taskDescriptor.setEntitySupplier(Task::new);
        taskDescriptor.setDataService("sqlite");

        io.teaql.core.sql.GenericSQLProperty idProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("id", Long.class);
        idProp.setColumnType("BIGINT");
        io.teaql.core.sql.GenericSQLProperty versionProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("version", Long.class);
        versionProp.setColumnType("BIGINT");
        io.teaql.core.sql.GenericSQLProperty titleProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("title", String.class);
        titleProp.setColumnType("VARCHAR(200)");
        io.teaql.core.sql.GenericSQLProperty statusProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("status", String.class);
        statusProp.setColumnType("VARCHAR(50)");
        
        taskDescriptor.with("table_name", "task_data");
        metaFactory.register(taskDescriptor);
        EntityMetaFactory.registerGlobal(metaFactory);

        DataSource ds = new SimpleDataSource(url, user, password);
        jdbcSqlExecutor = new JdbcSqlExecutor(ds);
        io.teaql.core.sqlite.SqliteDataServiceExecutor sqliteExecutor = new io.teaql.core.sqlite.SqliteDataServiceExecutor("sqlite", jdbcSqlExecutor, ds);

        AtomicLong idGen = new AtomicLong(2);
        InternalIdGenerationService idService = (c, entity) -> idGen.getAndIncrement();

        runtime = TeaQLRuntime.builder()
                .metadata(metaFactory)
                .dataService("sqlite", sqliteExecutor)
                .idGenerationService(idService)
                .build();
        
        context = new DefaultUserContext(runtime);

        // Drop existing tables for clean test state
        try {
            jdbcSqlExecutor.execute("DROP TABLE IF EXISTS task_data");
            jdbcSqlExecutor.execute("DROP TABLE IF EXISTS teaql_id_space");
        } catch (Exception e) {
            // ignore
        }

        // Ensure Schema
        context.ensureSchema();
    }

    @Test
    public void testEnsureSchemaRegistersSoundexIdempotently() {
        context.ensureSchema();
        List<java.util.Map<String, Object>> rows = jdbcSqlExecutor.queryForList(
                "SELECT soundex('Robert') AS robert, soundex('Rupert') AS rupert, soundex(NULL) AS empty",
                new Object[0]);
        assertEquals("R163", rows.get(0).get("robert"));
        assertEquals(rows.get(0).get("robert"), rows.get(0).get("rupert"));
        assertEquals("?000", rows.get(0).get("empty"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        Thread.sleep(500); // Allow asynchronous provider work to settle.
    }

    @Test
    public void testSqliteCrud() {
        context.pushTrace("SqliteIntegrationTest.testSqliteCrud");
        
        // 1. Create and Save Tasks
        Task task1 = new Task();
        task1.updateTitle("Assemble Assembly Line");
        task1.updateStatus("TODO");
        Task created = task1.auditAs("save").save(context);

        assertSame(task1, created);
        assertNotNull(task1.getId());
        assertEquals(Long.valueOf(1L), task1.getVersion());
        assertEquals("Status should transition to PERSISTED", EntityStatus.PERSISTED, task1.get$status());

        Task task2 = new Task();
        task2.updateTitle("Write Integration Tests");
        task2.updateStatus("TODO");
        task2.auditAs("save").save(context);

        // 2. Query Tasks by criteria
        TaskRequest req = new TaskRequest().filterByTitle("Assemble Assembly Line");
        SmartList<Task> resultList = req.comment("test").purpose("test").executeForList(context);
        assertEquals(1, resultList.size());
        assertEquals("Assemble Assembly Line", resultList.get(0).getTitle());

        // Test filter no results
        TaskRequest reqEmpty = new TaskRequest().filterByTitle("Clean up workspace");
        assertTrue(reqEmpty.comment("test").purpose("test").executeForList(context).isEmpty());

        // 3. Update task
        task1.updateStatus("DONE");
        Task updated = task1.auditAs("save").save(context);
        assertSame(task1, updated);
        assertEquals(Long.valueOf(2L), updated.getVersion());

        TaskRequest reqDone = new TaskRequest().filterByStatus("DONE");
        SmartList<Task> resultDone = reqDone.comment("test").purpose("test").executeForList(context);
        assertEquals(1, resultDone.size());
        assertEquals("Assemble Assembly Line", resultDone.get(0).getTitle());

        // 4. Delete task
        Task deleted = task1.markForDeletion().auditAs("delete").save(context);
        assertSame(task1, deleted);
        assertEquals(Long.valueOf(-3L), deleted.getVersion());
        assertEquals(EntityStatus.PERSISTED_DELETED, deleted.get$status());

        SmartList<Task> resultAfterDelete = new TaskRequest().filterByStatus("DONE").comment("test").purpose("test").executeForList(context);
        assertTrue(resultAfterDelete.isEmpty());
    }

    @Test
    public void temporalDebugSqlMatchesPreparedSqliteStorage() throws Exception {
        String sql = "INSERT INTO temporal_fixture VALUES (?, ?, ?) /* ignored ? */";
        Object[] args = {
            1L,
            LocalDate.of(2024, 2, 29),
            LocalDateTime.of(2026, 8, 19, 2, 3, 4, 123_000_000)
        };
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.createStatement().execute(
                    "CREATE TABLE temporal_fixture(id INTEGER, d TEXT, local_time TEXT)");
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, (Long) args[0]);
                statement.setString(2, args[1].toString());
                statement.setString(3, args[2].toString().replace('T', ' '));
                statement.executeUpdate();
            }
            String literal = SqlDataServiceExecutor.debugSql(sql, args)
                    .replaceFirst("VALUES \\(1,", "VALUES (2,");
            connection.createStatement().executeUpdate(literal);
            try (ResultSet rows = connection.createStatement().executeQuery(
                    "SELECT d, local_time, typeof(d), typeof(local_time) "
                            + "FROM temporal_fixture ORDER BY id")) {
                for (int i = 0; i < 2; i++) {
                    assertTrue(rows.next());
                    assertEquals("2024-02-29", rows.getString(1));
                    assertEquals("2026-08-19 02:03:04.123", rows.getString(2));
                    assertEquals("text", rows.getString(3));
                    assertEquals("text", rows.getString(4));
                }
                assertFalse(rows.next());
            }
        }
    }
}
