package io.teaql.dataservice.sql;

import io.teaql.core.UserContext;
import io.teaql.core.MutationRequest;
import io.teaql.core.QueryRequest;
import io.teaql.core.BaseEntity;
import io.teaql.core.BaseRequest;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.runtime.DefaultQueryRequest;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SqlDataServiceExecutorTest {

    private SqlDataServiceExecutor executor;
    private MockSqlExecutionAdapter mockAdapter;

    @Before
    public void setUp() {
        mockAdapter = new MockSqlExecutionAdapter();
        executor = new SqlDataServiceExecutor("sql", mockAdapter);
    }

    @Test
    public void testBasicCapabilities() {
        assertEquals("sql", executor.name());
        assertTrue(executor.capabilities().isQuery());
        assertTrue(executor.capabilities().isMutation());
        assertTrue(executor.capabilities().isTransaction());
        
        // ensure getExecutionAdapter returns exactly what we passed
        assertEquals(mockAdapter, executor.getExecutionAdapter());
    }

    @Test
    public void testQueryPlaceholder() {
        SqlDataServiceExecutor executor = new SqlDataServiceExecutor("sql", new MockSqlExecutionAdapter());
        Assert.assertThrows(io.teaql.core.TeaQLRuntimeException.class, () -> {
            executor.query(null, new QueryRequest() {});
        });
    }

    @Test
    public void testMutatePlaceholder() {
        SqlDataServiceExecutor executor = new SqlDataServiceExecutor("sql", new MockSqlExecutionAdapter());
        Assert.assertThrows(io.teaql.core.TeaQLRuntimeException.class, () -> {
            executor.mutate(null, new MutationRequest() {});
        });
    }

    @Test
    public void portableServiceIsScopedByInvokingContextMetadata() {
        EntityMetaFactory previous = EntityMetaFactory.get();
        try {
            EntityMetaFactory.registerGlobal(null);
            UserContext alphaContext = contextFor(metadataFor("alpha_task_data"));
            UserContext betaContext = contextFor(metadataFor("beta_task_data"));

            executor.query(alphaContext, new DefaultQueryRequest(new ScopedTaskRequest()));
            assertTrue(mockAdapter.lastSql, mockAdapter.lastSql.contains("alpha_task_data"));

            executor.query(betaContext, new DefaultQueryRequest(new ScopedTaskRequest()));
            assertTrue(mockAdapter.lastSql, mockAdapter.lastSql.contains("beta_task_data"));

            executor.query(alphaContext, new DefaultQueryRequest(new ScopedTaskRequest()));
            assertTrue(mockAdapter.lastSql, mockAdapter.lastSql.contains("alpha_task_data"));
        } finally {
            EntityMetaFactory.registerGlobal(previous);
        }
    }

    private UserContext contextFor(EntityMetaFactory metadata) {
        return new DefaultUserContext(TeaQLRuntime.builder().metadata(metadata).build());
    }

    private SimpleEntityMetaFactory metadataFor(String tableName) {
        SimpleEntityMetaFactory metadata = new SimpleEntityMetaFactory();
        EntityDescriptor descriptor = new EntityDescriptor();
        descriptor.setType("ScopedTask");
        descriptor.setTargetType(ScopedTask.class);
        descriptor.setEntitySupplier(ScopedTask::new);
        descriptor.setDataService("sql");
        GenericSQLProperty id = new GenericSQLProperty(tableName, "id", "BIGINT");
        id.setName("id");
        id.setOwner(descriptor);
        id.setType(new SimplePropertyType(Long.class));
        GenericSQLProperty version =
                new GenericSQLProperty(tableName, "version", "BIGINT");
        version.setName("version");
        version.setOwner(descriptor);
        version.setType(new SimplePropertyType(Long.class));
        descriptor.setProperties(List.of(id, version));
        metadata.register(descriptor);
        return metadata;
    }

    public static class ScopedTask extends BaseEntity {
        @Override public String typeName() { return "ScopedTask"; }
    }

    public static class ScopedTaskRequest extends BaseRequest<ScopedTask> {
        public ScopedTaskRequest() { super(ScopedTask.class); }
        @Override public String getTypeName() { return "ScopedTask"; }
    }

    @Test
    public void genericExecutorRejectsSchemaInitialization() {
        SecurityException boundary = Assert.assertThrows(
                SecurityException.class,
                () -> executor.ensureSchema(null, null));
        assertEquals("Ensure Schema must be invoked through UserContext.ensureSchema()", boundary.getMessage());

    }

    @Test
    public void debugSqlRendersCopyPasteStatement() {
        String sql = "SELECT * FROM school WHERE name = ? AND active = ? AND phone IS ? AND note = '?'";
        Object[] parameters = {"O'Brien School", true, null};

        assertEquals(
                "SELECT * FROM school WHERE name = 'O''Brien School' AND active = TRUE AND phone IS NULL AND note = '?'",
                SqlDataServiceExecutor.debugSql(sql, parameters));
    }

    @Test
    public void debugSqlPreservesCommentsAndTemporalStorageLiterals() {
        String sql = "-- line ? $1\nSELECT '?', \"identifier?\", ?, ? /* block ? */";
        Object[] parameters = {
                java.time.LocalDate.of(2024, 2, 29),
                java.time.LocalDateTime.of(2026, 8, 19, 9, 30, 0, 123_000_000)
        };

        assertEquals(
                "-- line ? $1\nSELECT '?', \"identifier?\", '2024-02-29', '2026-08-19 09:30:00.123' /* block ? */",
                SqlDataServiceExecutor.debugSql(sql, parameters));
    }

    @Test
    public void debugSqlUsesTypedPostgresAndMysqlTemporalLiterals() {
        Object[] parameters = {
                java.time.LocalDate.of(2024, 2, 29),
                java.time.LocalDateTime.of(2026, 8, 19, 3, 30, 0, 123_000_000)
        };
        assertEquals(
                "SELECT DATE '2024-02-29', TIMESTAMP '2026-08-19 03:30:00.123' /* ignored ? */",
                SqlDataServiceExecutor.debugSql("SELECT ?, ? /* ignored ? */", parameters, "postgresql"));
        assertEquals(
                "SELECT CAST('2024-02-29' AS DATE), CAST('2026-08-19 03:30:00.123' AS DATETIME(3)) /* ignored ? */",
                SqlDataServiceExecutor.debugSql("SELECT ?, ? /* ignored ? */", parameters, "mysql"));
        assertEquals(
                "SELECT CAST('2024-02-29' AS DATE), CAST('2026-08-19 03:30:00.123' AS DATETIME2(3)) /* ignored ? */",
                SqlDataServiceExecutor.debugSql("SELECT ?, ? /* ignored ? */", parameters, "mssql"));
    }

    private static class MockSqlExecutionAdapter implements SqlExecutionAdapter {
        public String lastSql;
        public Map<String, Object> lastParams;

        @Override
        public <T> List<T> query(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
            this.lastSql = sql;
            this.lastParams = params;
            return Collections.emptyList();
        }

        @Override
        public <T> Stream<T> queryForStream(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
            return Stream.empty();
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Map<String, Object> params) {
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object[] params) {
            this.lastSql = sql;
            return Collections.emptyList();
        }

        @Override
        public <T extends io.teaql.core.Entity> List<T> query(
                String sql, Object[] params, io.teaql.core.CompiledRowMapper<T> rowMapper) {
            this.lastSql = sql;
            return Collections.emptyList();
        }

        @Override
        public Map<String, Object> queryForMap(String sql, Map<String, Object> params) {
            return Collections.emptyMap();
        }

        @Override
        public <T> T queryForObject(String sql, Map<String, Object> params, Class<T> requiredType) {
            return null;
        }

        @Override
        public void execute(String sql) {
            this.lastSql = sql;
        }

        @Override
        public int update(String sql, Map<String, Object> params) {
            this.lastSql = sql;
            this.lastParams = params;
            return 1;
        }

        @Override
        public int update(String sql, Object[] params) {
            this.lastSql = sql;
            return 1;
        }

        @Override
        public int[] batchUpdate(String sql, List<Object[]> paramsList) {
            this.lastSql = sql;
            return new int[]{1};
        }
    }
}
