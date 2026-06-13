package io.teaql.coreservice.sql;

import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.coreservice.MutationRequest;
import io.teaql.coreservice.QueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SqlDataServiceExecutorTest {

    private SqlDataServiceExecutor executor;
    private MockSqlExecutionAdapter mockAdapter;

    @BeforeEach
    void setUp() {
        mockAdapter = new MockSqlExecutionAdapter();
        executor = new SqlDataServiceExecutor("sql", mockAdapter);
    }

    @Test
    void testBasicCapabilities() {
        assertEquals("sql", executor.name());
        assertTrue(executor.capabilities().isQuery());
        assertTrue(executor.capabilities().isMutation());
        assertTrue(executor.capabilities().isTransaction());
        
        // ensure getExecutionAdapter returns exactly what we passed
        assertEquals(mockAdapter, executor.getExecutionAdapter());
    }

    @Test
    void testQueryPlaceholder() {
        // 由于真正的编译逻辑还没从 SQLRepository 完全移到这，目前会抛出 UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            executor.query(new DefaultUserContext(), null);
        });
    }

    @Test
    void testMutatePlaceholder() {
        assertThrows(UnsupportedOperationException.class, () -> {
            executor.mutate(new DefaultUserContext(), null);
        });
    }

    // 一个纯内存记录参数的 Adapter，用于做纯粹的 SQL 生成测试，不连数据库！
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
