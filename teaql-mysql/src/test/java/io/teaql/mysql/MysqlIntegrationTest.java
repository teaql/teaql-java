package io.teaql.mysql;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.mysql.MysqlDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;

import org.junit.AfterClass;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class MysqlIntegrationTest {

    private static UserContext context;
    private static TeaQLRuntime runtime;

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
        // Use local mysql instance running on port 3306
        String url = "jdbc:mysql://127.0.0.1:3306/teaql_test?createDatabaseIfNotExist=true&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "0254891276";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            org.junit.Assume.assumeTrue("MySQL is reachable", true);
        } catch (SQLException e) {
            org.junit.Assume.assumeTrue("Skipping MySQL tests because MySQL is not reachable on " + url, false);
        }

        SimpleEntityMetaFactory metaFactory = new SimpleEntityMetaFactory();

        SQLEntityDescriptor taskDescriptor = new SQLEntityDescriptor();
        taskDescriptor.setType("Task");
        taskDescriptor.setTargetType(Task.class);
        taskDescriptor.setEntitySupplier(Task::new);
        taskDescriptor.setDataService("mysql");

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
        JdbcSqlExecutor jdbcSqlExecutor = new JdbcSqlExecutor(ds);
        MysqlDataServiceExecutor mysqlExecutor = new MysqlDataServiceExecutor("mysql", jdbcSqlExecutor, ds);

        AtomicLong idGen = new AtomicLong(2);
        InternalIdGenerationService idService = (c, entity) -> idGen.getAndIncrement();

        runtime = TeaQLRuntime.builder()
                .metadata(metaFactory)
                .dataService("mysql", mysqlExecutor)
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

    @AfterClass
    public static void teardown() {
    }

    @Test
    public void testMysqlCrud() {
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
