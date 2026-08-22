package io.teaql.dataservice.sql;

import io.teaql.core.UserContext;
import io.teaql.core.MutationRequest;
import io.teaql.core.QueryRequest;
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
    public void genericExecutorRejectsSchemaInitialization() {
        UnsupportedOperationException error = Assert.assertThrows(
                UnsupportedOperationException.class,
                () -> executor.ensureSchema(null));

        assertEquals(
                "Schema initialization is not implemented by "
                        + "io.teaql.dataservice.sql.SqlDataServiceExecutor "
                        + "(database kind: postgresql, dialect: PostgreSqlDialect). "
                        + "Use the database-specific executor "
                        + "io.teaql.core.postgres.PostgresDataServiceExecutor "
                        + "and call SchemaExecutor.ensureSchema(context) explicitly.",
                error.getMessage());
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
