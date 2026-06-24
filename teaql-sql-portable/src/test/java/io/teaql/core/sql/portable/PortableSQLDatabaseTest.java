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
import io.teaql.core.sql.SQLProperty;
import io.teaql.core.sql.SQLColumn;
import io.teaql.runtime.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class PortableSQLDatabaseTest {

    // ── Stub Entity and Request ──────────────────────────

    public static class Task extends BaseEntity {
        private String title;
        private String status;

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
        public void internalSet(String property, Object value) {
            switch (property) {
                case "title": this.title = (String) value; break;
                case "status": this.status = (String) value; break;
                default: super.internalSet(property, value);
            }
        }

        @Override
        public Object internalGet(String property) {
            switch (property) {
                case "title": return this.title;
                case "status": return this.status;
                default: return super.internalGet(property);
            }
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


        public TaskRequest filterByTitle(String title) {
            appendSearchCriteria(createBasicSearchCriteria("title", Operator.EQUAL, title));
            return this;
        }

        public TaskRequest filterByStatus(String status) {
            appendSearchCriteria(createBasicSearchCriteria("status", Operator.EQUAL, status));
            return this;
        }

        public TaskRequest comment(String comment) {
            super.internalComment(comment);
            return this;
        }
    }

    // ── SQLite TeaQLDatabase Implementation ────────────────

    public static class SQLiteTeaQLDatabase implements TeaQLDatabase {
        private final Connection connection;

        public SQLiteTeaQLDatabase() throws Exception {
            this.connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        }

        @Override
        public List<Map<String, Object>> query(String sql, Object[] args) {
            System.out.println("[SQL-QUERY] " + sql + " | args: " + Arrays.toString(args));
            List<Map<String, Object>> results = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    stmt.setObject(i + 1, args[i]);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= cols; i++) {
                            row.put(meta.getColumnLabel(i), rs.getObject(i));
                        }
                        results.add(row);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return results;
        }

        @Override
        public int executeUpdate(String sql, Object[] args) {
            System.out.println("[SQL-UPDATE] " + sql + " | args: " + Arrays.toString(args));
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    stmt.setObject(i + 1, args[i]);
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
                        stmt.setObject(i + 1, args[i]);
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
    private static UserContext ctx;

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

        ctx = new DefaultUserContext(runtime);
        ctx.put("ensureTable", true); // enable schema generation

        // Generate schema
        sqlDataService.ensureSchema(ctx, "Task");
    }

    @Test
    public void testPortableSQLDatabaseWorkflow() {
        // 1. Create and Save Tasks
        Task task1 = new Task();
        task1.updateTitle("Assemble Engine");
        task1.updateStatus("TODO");
        task1.auditAs("save").save(ctx);

        assertNotNull("ID should be generated automatically", task1.getId());
        assertEquals(Long.valueOf(200), task1.getId());
        assertEquals("Status should transition to PERSISTED", EntityStatus.PERSISTED, task1.get$status());

        Task task2 = new Task();
        task2.updateTitle("Verify Engine Parts");
        task2.updateStatus("TODO");
        task2.auditAs("save").save(ctx);
        assertEquals(Long.valueOf(201), task2.getId());

        // 2. Query Tasks by criteria
        TaskRequest req = new TaskRequest().filterByTitle("Assemble Engine");
        SmartList<Task> resultList = req.comment("test").purpose("test").executeForList(ctx);
        assertEquals(1, resultList.size());
        assertEquals("Assemble Engine", resultList.get(0).getTitle());

        // Test filter no results
        TaskRequest reqEmpty = new TaskRequest().filterByTitle("Unknown Task");
        assertTrue(reqEmpty.comment("test").purpose("test").executeForList(ctx).isEmpty());

        // 3. Update task
        task1.updateStatus("DONE");
        task1.auditAs("save").save(ctx);

        TaskRequest reqDone = new TaskRequest().filterByStatus("DONE");
        SmartList<Task> resultDone = reqDone.comment("test").purpose("test").executeForList(ctx);
        assertEquals(1, resultDone.size());
        assertEquals("Assemble Engine", resultDone.get(0).getTitle());

        // 4. Delete task
        task1.auditAs("delete").delete(ctx);

        SmartList<Task> resultAfterDelete = new TaskRequest().filterByStatus("DONE").comment("test").purpose("test").executeForList(ctx);
        assertTrue("Task should be removed from DB", resultAfterDelete.isEmpty());
    }
}
