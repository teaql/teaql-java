package com.example.schoolmanagementservice;

import io.teaql.core.CompiledRowMapper;
import io.teaql.core.Entity;
import io.teaql.dataservice.sql.SqlRowMapper;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Stream;
import javax.sql.DataSource;

/** Test-only provider-entry counter. SQL, parameters and row values are never retained. */
final class ObservedJdbcExecutor extends JdbcSqlExecutor {
    private final AtomicLongArray calls = new AtomicLongArray(3);
    ObservedJdbcExecutor(DataSource source) { super(source); }
    /** Read, write and transaction entry; nested delegations may count more than once. */
    long[] counts() { return new long[]{calls.get(0), calls.get(1), calls.get(2)}; }
    @Override public <T> List<T> query(String sql, Map<String,Object> params, SqlRowMapper<T> mapper) {
        calls.incrementAndGet(0); return super.query(sql, params, mapper);
    }
    @Override public <T extends Entity> List<T> query(String sql, Object[] params, CompiledRowMapper<T> mapper) {
        calls.incrementAndGet(0); return super.query(sql, params, mapper);
    }
    @Override public <T> Stream<T> queryForStream(String sql, Map<String,Object> params, SqlRowMapper<T> mapper) {
        calls.incrementAndGet(0); return super.queryForStream(sql, params, mapper);
    }
    @Override public Stream<Map<String,Object>> queryForStream(String sql, Object[] params) {
        calls.incrementAndGet(0); return super.queryForStream(sql, params);
    }
    @Override public <T extends Entity> Stream<T> queryForStream(String sql, Object[] params, CompiledRowMapper<T> mapper) {
        calls.incrementAndGet(0); return super.queryForStream(sql, params, mapper);
    }
    @Override public List<Map<String,Object>> queryForList(String sql, Map<String,Object> params) {
        calls.incrementAndGet(0); return super.queryForList(sql, params);
    }
    @Override public List<Map<String,Object>> queryForList(String sql, Object[] params) {
        calls.incrementAndGet(0); return super.queryForList(sql, params);
    }
    @Override public Map<String,Object> queryForMap(String sql, Map<String,Object> params) {
        calls.incrementAndGet(0); return super.queryForMap(sql, params);
    }
    @Override public <T> T queryForObject(String sql, Map<String,Object> params, Class<T> type) {
        calls.incrementAndGet(0); return super.queryForObject(sql, params, type);
    }
    @Override public void execute(String sql) { calls.incrementAndGet(1); super.execute(sql); }
    @Override public int update(String sql, Map<String,Object> params) { calls.incrementAndGet(1); return super.update(sql, params); }
    @Override public int update(String sql, Object[] params) { calls.incrementAndGet(1); return super.update(sql, params); }
    @Override public int[] batchUpdate(String sql, List<Object[]> params) { calls.incrementAndGet(1); return super.batchUpdate(sql, params); }
    @Override public void executeInTransaction(Runnable action) { calls.incrementAndGet(2); super.executeInTransaction(action); }
}
