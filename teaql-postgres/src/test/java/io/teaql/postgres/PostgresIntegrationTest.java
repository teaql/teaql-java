package io.teaql.postgres;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.postgres.PostgresDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class PostgresIntegrationTest {

    private static UserContext context;
    private static TeaQLRuntime runtime;
    private static DataSource dataSource;

    private static final class JdbcTeaQLDatabase implements TeaQLDatabase {
        private final JdbcSqlExecutor executor;

        private JdbcTeaQLDatabase(DataSource dataSource) {
            this.executor = new JdbcSqlExecutor(dataSource);
        }

        @Override public List<java.util.Map<String, Object>> query(String sql, Object[] args) {
            return executor.queryForList(sql, args);
        }

        @Override public int executeUpdate(String sql, Object[] args) {
            return executor.update(sql, args);
        }

        @Override public int[] batchUpdate(String sql, List<Object[]> args) {
            return executor.batchUpdate(sql, args);
        }

        @Override public void execute(String sql) { executor.execute(sql); }

        @Override public void executeInTransaction(Runnable action) {
            executor.executeInTransaction(action);
        }

        @Override public List<java.util.Map<String, Object>> getTableColumns(String tableName) {
            throw new UnsupportedOperationException("Schema inspection belongs to the dialect executor");
        }
    }

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

    public static class RelationParent extends BaseEntity {
        @Override public String typeName() { return "RelationParent"; }
    }

    public static class RelationChild extends BaseEntity {
        @Override public String typeName() { return "RelationChild"; }
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
        String url = System.getenv("TEAQL_TEST_POSTGRES_URL");
        String user = System.getenv("TEAQL_TEST_POSTGRES_USER");
        String password = System.getenv("TEAQL_TEST_POSTGRES_PASSWORD");
        boolean required = Boolean.parseBoolean(System.getenv("TEAQL_REQUIRE_LIVE_DB"));
        if (url == null || user == null || password == null) {
            if (required) fail("PostgreSQL live gate requires URL, USER, and PASSWORD environment variables");
            org.junit.Assume.assumeTrue("PostgreSQL live test is not configured", false);
        }
        assertTrue("Use a dedicated teaql_live_* PostgreSQL database for this test",
                url.matches("jdbc:postgresql://[^/]+/teaql_live_[A-Za-z0-9_]+(\\?.*)?"));

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // A successful connection is required before any schema mutation.
        } catch (SQLException e) {
            if (required) throw new AssertionError("PostgreSQL live gate cannot connect to " + url, e);
            org.junit.Assume.assumeTrue("PostgreSQL is not reachable on " + url, false);
        }

        SimpleEntityMetaFactory metaFactory = new SimpleEntityMetaFactory();

        SQLEntityDescriptor taskDescriptor = new SQLEntityDescriptor();
        taskDescriptor.setType("Task");
        taskDescriptor.setTargetType(Task.class);
        taskDescriptor.setEntitySupplier(Task::new);
        taskDescriptor.setDataService("postgres");

        io.teaql.core.sql.GenericSQLProperty idProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("id", Long.class);
        idProp.setColumnType("BIGINT");
        io.teaql.core.sql.GenericSQLProperty versionProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("version", Long.class);
        versionProp.setColumnType("BIGINT");
        io.teaql.core.sql.GenericSQLProperty titleProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("title", String.class);
        titleProp.setColumnType("VARCHAR(200)");
        io.teaql.core.sql.GenericSQLProperty statusProp = (io.teaql.core.sql.GenericSQLProperty) taskDescriptor.addSimpleProperty("status", String.class);
        statusProp.setColumnType("VARCHAR(50)");

        metaFactory.register(taskDescriptor);
        EntityMetaFactory.registerGlobal(metaFactory);

        dataSource = new SimpleDataSource(url, user, password);
        JdbcSqlExecutor sqlExecutor = new JdbcSqlExecutor(dataSource);
        io.teaql.core.postgres.PostgresDataServiceExecutor postgresExecutor = new io.teaql.core.postgres.PostgresDataServiceExecutor("postgres", sqlExecutor);
        IdSpaceIdGenerator idGenerator = new IdSpaceIdGenerator(new JdbcTeaQLDatabase(dataSource));
        idGenerator.ensureIdSpaceTable();

        runtime = TeaQLRuntime.builder()
                .metadata(metaFactory)
                .dataService("postgres", postgresExecutor)
                .idGenerationService(idGenerator)
                .build();
        
        context = new DefaultUserContext(runtime);

        // Ensure Schema
        context.ensureSchema();
    }

    @Test
    public void testPortableIdGeneratorAcrossConcurrentInstancesAndRestart() throws Exception {
        String typeName = "PostgresLiveIdProbe";
        IdSpaceIdGenerator first = new IdSpaceIdGenerator(new JdbcTeaQLDatabase(dataSource));
        long baseline = first.nextId(typeName);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Long>> allocations = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                allocations.add(() -> new IdSpaceIdGenerator(
                        new JdbcTeaQLDatabase(dataSource)).nextId(typeName));
            }
            List<Future<Long>> futures = pool.invokeAll(allocations);
            List<Long> ids = new ArrayList<>();
            for (Future<Long> future : futures) ids.add(future.get());
            Collections.sort(ids);
            for (int i = 0; i < ids.size(); i++) {
                assertEquals(baseline + i + 1L, ids.get(i).longValue());
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(baseline + 41L,
                new IdSpaceIdGenerator(new JdbcTeaQLDatabase(dataSource)).nextId(typeName));
    }

    @Test
    public void testSchemaUsesInvokingContextMetadata() {
        SimpleEntityMetaFactory isolated = new SimpleEntityMetaFactory();
        SQLEntityDescriptor probe = new SQLEntityDescriptor();
        probe.setType("ContextProbe");
        probe.setTargetType(Task.class);
        probe.setEntitySupplier(Task::new);
        probe.setDataService("postgres");
        io.teaql.core.sql.GenericSQLProperty id =
                (io.teaql.core.sql.GenericSQLProperty) probe.addSimpleProperty("id", Long.class);
        id.setColumnType("BIGINT");
        io.teaql.core.sql.GenericSQLProperty version =
                (io.teaql.core.sql.GenericSQLProperty) probe.addSimpleProperty("version", Long.class);
        version.setColumnType("BIGINT");
        isolated.register(probe);

        DataSource dataSource = new SimpleDataSource(
                System.getenv("TEAQL_TEST_POSTGRES_URL"),
                System.getenv("TEAQL_TEST_POSTGRES_USER"),
                System.getenv("TEAQL_TEST_POSTGRES_PASSWORD"));
        JdbcSqlExecutor jdbc = new JdbcSqlExecutor(dataSource);
        TeaQLRuntime isolatedRuntime = TeaQLRuntime.builder()
                .metadata(isolated)
                .dataService("postgres", new PostgresDataServiceExecutor("postgres", jdbc))
                .idGenerationService((c, entity) -> 1L)
                .build();

        new DefaultUserContext(isolatedRuntime).ensureSchema();
        assertTrue("The invoking context, not global Task metadata, must create ContextProbe",
                jdbc.queryForList("SELECT id FROM context_probe_data WHERE 1 = 0", new Object[0]).isEmpty());
    }

    @Test
    public void testCanonicalRelationIndexIsIdempotentOnLivePostgres() {
        SimpleEntityMetaFactory isolated = relationMetadata("postgres");
        JdbcSqlExecutor jdbc = new JdbcSqlExecutor(dataSource);
        TeaQLRuntime isolatedRuntime = TeaQLRuntime.builder()
                .metadata(isolated)
                .dataService("postgres", new PostgresDataServiceExecutor("postgres", jdbc))
                .build();
        UserContext isolatedContext = new DefaultUserContext(isolatedRuntime);

        isolatedContext.ensureSchema();
        isolatedContext.ensureSchema();

        List<java.util.Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = current_schema() "
                        + "AND tablename = ? AND indexname = ?",
                new Object[] {"rel_child_data", "idx_rel_child_data_parent_id"});
        assertEquals("Repeated ensureSchema must retain exactly one canonical relation index",
                1, indexes.size());
    }

    private static SimpleEntityMetaFactory relationMetadata(String dataService) {
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        SQLEntityDescriptor parent = relationEntity(
                "RelationParent", RelationParent.class, RelationParent::new,
                "rel_parent_data", dataService);
        metadata.register(parent);
        SQLEntityDescriptor child = relationEntity(
                "RelationChild", RelationChild.class, RelationChild::new,
                "rel_child_data", dataService);
        io.teaql.core.sql.GenericSQLRelation relation =
                (io.teaql.core.sql.GenericSQLRelation) child.addObjectProperty(
                        metadata, "parent", "RelationParent", "children",
                        RelationParent.class, io.teaql.core.sql.GenericSQLRelation::new);
        relation.setTableName("rel_child_data");
        relation.setColumnName("parent");
        relation.setColumnType("BIGINT");
        metadata.register(child);
        return metadata;
    }

    private static SQLEntityDescriptor relationEntity(
            String type, Class<? extends Entity> targetType,
            java.util.function.Supplier<? extends Entity> supplier,
            String table, String dataService) {
        SQLEntityDescriptor descriptor = new SQLEntityDescriptor();
        descriptor.setType(type);
        descriptor.setTargetType(targetType);
        descriptor.setEntitySupplier(supplier);
        descriptor.setDataService(dataService);
        io.teaql.core.sql.GenericSQLProperty id =
                (io.teaql.core.sql.GenericSQLProperty) descriptor.addSimpleProperty("id", Long.class);
        id.setTableName(table);
        id.setColumnName("id");
        id.setColumnType("BIGINT");
        io.teaql.core.sql.GenericSQLProperty version =
                (io.teaql.core.sql.GenericSQLProperty) descriptor.addSimpleProperty("version", Long.class);
        version.setTableName(table);
        version.setColumnName("version");
        version.setColumnType("BIGINT");
        return descriptor;
    }

    @Test
    public void testPostgresCrud() {
        // 1. Create and Save Tasks
        Task task1 = new Task();
        task1.updateTitle("Assemble Assembly Line");
        task1.updateStatus("TODO");
        task1.auditAs("save").save(context);

        assertNotNull(task1.getId());
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
        task1.auditAs("save").save(context);

        TaskRequest reqDone = new TaskRequest().filterByStatus("DONE");
        SmartList<Task> resultDone = reqDone.comment("test").purpose("test").executeForList(context);
        assertEquals(1, resultDone.size());
        assertEquals("Assemble Assembly Line", resultDone.get(0).getTitle());

        // 4. Delete task
        task1.markForDeletion().auditAs("delete").save(context);

        SmartList<Task> resultAfterDelete = new TaskRequest().filterByStatus("DONE").comment("test").purpose("test").executeForList(context);
        assertTrue(resultAfterDelete.isEmpty());
    }
}
