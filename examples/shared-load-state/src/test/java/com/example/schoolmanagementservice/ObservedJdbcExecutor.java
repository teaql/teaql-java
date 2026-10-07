package com.example.schoolmanagementservice;

import io.teaql.core.CompiledRowMapper;
import io.teaql.core.Entity;
import io.teaql.dataservice.sql.SqlRowMapper;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.sql.DataSource;

/** Test-only provider-entry counter. SQL, parameters and row values are never retained. */
final class ObservedJdbcExecutor extends JdbcSqlExecutor {
    private final AtomicLongArray calls = new AtomicLongArray(3);
    private final AtomicBoolean failDynamicReadback = new AtomicBoolean();
    private final AtomicBoolean dynamicWriteSeen = new AtomicBoolean();
    private final AtomicBoolean readbackFailureSeen = new AtomicBoolean();
    ObservedJdbcExecutor(DataSource source) { super(source); }
    /** Read, write and transaction entry; nested delegations may count more than once. */
    long[] counts() { return new long[]{calls.get(0), calls.get(1), calls.get(2)}; }
    void failNextDynamicReadbackAfterWrite() {
        dynamicWriteSeen.set(false); readbackFailureSeen.set(false); failDynamicReadback.set(true);
    }
    boolean readbackFailureObserved() { return readbackFailureSeen.get(); }
    void clearReadbackFailure() { failDynamicReadback.set(false); }
    private void observeDynamicWrite(String sql, int changed) {
        if (changed > 0 && failDynamicReadback.get() && sql.contains("teaql_dynamic_field_value")) dynamicWriteSeen.set(true);
    }
    private void rejectDynamicReadback(String sql) {
        if (dynamicWriteSeen.get() && sql.contains("teaql_dynamic_field_value") && failDynamicReadback.compareAndSet(true, false)) {
            readbackFailureSeen.set(true);
            throw new IllegalStateException("CONTROLLED_DYNAMIC_READBACK_FAILURE");
        }
    }
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
        calls.incrementAndGet(0); rejectDynamicReadback(sql); return super.queryForList(sql, params);
    }
    @Override public List<Map<String,Object>> queryForList(String sql, Object[] params) {
        calls.incrementAndGet(0); rejectDynamicReadback(sql); return super.queryForList(sql, params);
    }
    @Override public Map<String,Object> queryForMap(String sql, Map<String,Object> params) {
        calls.incrementAndGet(0); return super.queryForMap(sql, params);
    }
    @Override public <T> T queryForObject(String sql, Map<String,Object> params, Class<T> type) {
        calls.incrementAndGet(0); return super.queryForObject(sql, params, type);
    }
    @Override public void execute(String sql) { calls.incrementAndGet(1); super.execute(sql); }
    @Override public int update(String sql, Map<String,Object> params) { calls.incrementAndGet(1); int changed = super.update(sql, params); observeDynamicWrite(sql, changed); return changed; }
    @Override public int update(String sql, Object[] params) { calls.incrementAndGet(1); int changed = super.update(sql, params); observeDynamicWrite(sql, changed); return changed; }
    @Override public int[] batchUpdate(String sql, List<Object[]> params) { calls.incrementAndGet(1); return super.batchUpdate(sql, params); }
    @Override public void executeInTransaction(Runnable action) { calls.incrementAndGet(2); super.executeInTransaction(action); }
}
