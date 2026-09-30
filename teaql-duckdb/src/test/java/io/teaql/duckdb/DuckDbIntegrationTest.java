package io.teaql.duckdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import io.teaql.core.EntityStatus;
import io.teaql.core.InternalIdGenerationService;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.Operator;
import io.teaql.core.duck.DuckDataServiceExecutor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DuckDbIntegrationTest {

    private static UserContext context;
    private static JdbcSqlExecutor sql;
    private static InspectableDuckDataServiceExecutor executor;
    private static Path databaseDirectory;
    private static Path databaseFile;
    private static String url;

    public static class Task extends BaseEntity {
        private String title;
        private String status;
        private BigDecimal amount;

        public String getTitle() {
            return title;
        }

        public Task updateTitle(String value) {
            handleUpdate("title", title, value);
            title = value;
            return this;
        }

        public Task updateStatus(String value) {
            handleUpdate("status", status, value);
            status = value;
            return this;
        }

        @Override
        public String typeName() {
            return "Task";
        }

        @Override
        public void __internalSet(String property, Object value) {
            switch (property) {
                case "title" -> title = (String) value;
                case "status" -> status = (String) value;
                case "amount" -> amount = (BigDecimal) value;
                default -> super.__internalSet(property, value);
            }
        }

        @Override
        public Object __internalGet(String property) {
            return switch (property) {
                case "title" -> title;
                case "status" -> status;
                case "amount" -> amount;
                default -> super.__internalGet(property);
            };
        }
    }

    public static class TaskRequest extends BaseRequest<Task> {
        public TaskRequest() {
            super(Task.class);
        }

        @Override
        public String getTypeName() {
            return "Task";
        }

        public TaskRequest withStatusIs(String value) {
            appendSearchCriteria(createBasicSearchCriteria("status", Operator.EQUAL, value));
            return this;
        }

        public TaskRequest comment(String value) {
            internalComment(value);
            return this;
        }
    }

    private static final class DriverManagerDataSource implements DataSource {
        private final String connectionUrl;

        private DriverManagerDataSource(String connectionUrl) {
            this.connectionUrl = connectionUrl;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(connectionUrl);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    private static final class InspectableDuckDataServiceExecutor
            extends DuckDataServiceExecutor {
        private InspectableDuckDataServiceExecutor(
                String name, JdbcSqlExecutor executionAdapter) {
            super(name, executionAdapter);
        }

        private String debugDatabaseKind() {
            return debugDatabaseKind;
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        databaseDirectory = Files.createTempDirectory("teaql-duckdb-");
        databaseFile = databaseDirectory.resolve("runtime-" + UUID.randomUUID() + ".db");
        url = "jdbc:duckdb:" + databaseFile;

        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        SQLEntityDescriptor descriptor = new SQLEntityDescriptor();
        descriptor.setType("Task");
        descriptor.setTargetType(Task.class);
        descriptor.setEntitySupplier(Task::new);
        descriptor.setDataService("duckdb");
        ((GenericSQLProperty) descriptor.addSimpleProperty("id", Long.class))
                .setColumnType("BIGINT");
        ((GenericSQLProperty) descriptor.addSimpleProperty("version", Long.class))
                .setColumnType("BIGINT");
        ((GenericSQLProperty) descriptor.addSimpleProperty("title", String.class))
                .setColumnType("VARCHAR(200)");
        ((GenericSQLProperty) descriptor.addSimpleProperty("status", String.class))
                .setColumnType("VARCHAR(50)");
        ((GenericSQLProperty) descriptor.addSimpleProperty("amount", BigDecimal.class))
                .setColumnType("DECIMAL(19,7)");
        descriptor.with("table_name", "task_data");
        metadata.register(descriptor);

        sql = new JdbcSqlExecutor(new DriverManagerDataSource(url));
        executor = new InspectableDuckDataServiceExecutor("duckdb", sql);
        AtomicLong ids = new AtomicLong(1);
        InternalIdGenerationService idService = (ignored, entity) -> ids.getAndIncrement();
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(metadata)
                .dataService("duckdb", executor)
                .idGenerationService(idService)
                .build();
        context = new DefaultUserContext(runtime);
        context.ensureSchema();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.execute("CHECKPOINT");
        } finally {
            Files.deleteIfExists(Path.of(databaseFile + ".wal"));
            Files.deleteIfExists(databaseFile);
            Files.deleteIfExists(databaseDirectory);
        }
    }

    @Test
    public void exposesExpectedEngineVersion() throws Exception {
        assertEquals("duckdb", executor.debugDatabaseKind());
        try (Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT version()")) {
            assertTrue(rows.next());
            assertTrue(rows.getString(1).matches("v?[0-9]+\\.[0-9]+.*"));
        }
    }

    @Test
    public void executesTeaQlCrudAndTypedQuery() {
        Task task = new Task().updateTitle("Verify DuckDB").updateStatus("OPEN");
        Task created = task.auditAs("create DuckDB compatibility fixture").save(context);
        assertSame(task, created);
        assertNotNull(created.getId());
        assertEquals(Long.valueOf(1), created.getVersion());

        SmartList<Task> open = new TaskRequest()
                .withStatusIs("OPEN")
                .comment("what: load the DuckDB compatibility fixture")
                .purpose("why: verify typed TeaQL query execution")
                .executeForList(context);
        assertEquals(1, open.size());
        assertEquals("Verify DuckDB", open.get(0).getTitle());

        task.updateStatus("DONE")
                .auditAs("complete DuckDB compatibility fixture")
                .save(context);
        assertEquals(Long.valueOf(2), task.getVersion());
        task.markForDeletion()
                .auditAs("delete DuckDB compatibility fixture")
                .save(context);
        assertEquals(EntityStatus.PERSISTED_DELETED, task.get$status());
        assertTrue(new TaskRequest()
                .withStatusIs("DONE")
                .comment("what: confirm the deleted fixture is hidden")
                .purpose("why: verify TeaQL deletion semantics on DuckDB")
                .executeForList(context)
                .isEmpty());
    }

    @Test
    public void supportsFacetWindowAndTransactionSql() throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.executeUpdate(
                    "INSERT INTO task_data(id, version, title, status) VALUES "
                            + "(101, 1, 'A', 'GROUPED'), "
                            + "(102, 1, 'B', 'GROUPED'), "
                            + "(103, 1, 'C', 'OTHER')");
            connection.commit();

            try (ResultSet facet = statement.executeQuery(
                    "SELECT status, count(*) FROM task_data GROUP BY status ORDER BY status")) {
                assertTrue(facet.next());
            }
            try (ResultSet window = statement.executeQuery(
                    "SELECT id, row_number() OVER (PARTITION BY status ORDER BY id) rn "
                            + "FROM task_data WHERE status = 'GROUPED' ORDER BY id")) {
                assertTrue(window.next());
                assertEquals(1, window.getInt("rn"));
                assertTrue(window.next());
                assertEquals(2, window.getInt("rn"));
                assertFalse(window.next());
            }
        }
    }

    @Test
    public void validatesSchemaValueDomainAndNullability() throws Exception {
        try {
            sql.execute("ALTER TABLE task_data ALTER COLUMN amount SET DATA TYPE DECIMAL(38,10)");
            context.ensureSchema();

            sql.execute("ALTER TABLE task_data ALTER COLUMN amount SET DATA TYPE DECIMAL(18,2)");
            assertSchemaFailure("required precision=19, scale=7");
            sql.execute("ALTER TABLE task_data ALTER COLUMN amount SET DATA TYPE DECIMAL(38,10)");

            sql.execute("ALTER TABLE task_data ALTER COLUMN status SET NOT NULL");
            assertSchemaFailure("expected nullable=true");
        } finally {
            sql.execute("ALTER TABLE task_data ALTER COLUMN amount SET DATA TYPE DECIMAL(19,7)");
            sql.execute("ALTER TABLE task_data ALTER COLUMN status DROP NOT NULL");
        }
        context.ensureSchema();
    }

    private static void assertSchemaFailure(String expectedMessage) {
        IllegalStateException error = org.junit.Assert.assertThrows(
                IllegalStateException.class,
                () -> context.ensureSchema());
        assertTrue(error.getMessage(), error.getMessage().contains(expectedMessage));
        assertTrue(error.getMessage(), error.getMessage().contains("Task"));
        assertTrue(error.getMessage(), error.getMessage().contains("task_data"));
    }
}
