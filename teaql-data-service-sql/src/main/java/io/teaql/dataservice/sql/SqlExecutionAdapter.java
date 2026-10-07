package io.teaql.dataservice.sql;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface SqlExecutionAdapter {
    
    <T> List<T> query(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper);

    default <T extends io.teaql.core.Entity> List<T> query(
            String sql, Object[] params, io.teaql.core.CompiledRowMapper<T> rowMapper) {
        throw new UnsupportedOperationException("compiled row mapping is not supported");
    }
    
    <T> Stream<T> queryForStream(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper);
    default Stream<Map<String, Object>> queryForStream(String sql, Object[] params) {
        throw new UnsupportedOperationException("streaming query is not supported");
    }

    default boolean supportsCompiledStreamMapping() { return false; }

    default <T extends io.teaql.core.Entity> Stream<T> queryForStream(
            String sql, Object[] params, io.teaql.core.CompiledRowMapper<T> mapper) {
        throw new UnsupportedOperationException("compiled streaming row mapping is not supported");
    }
    
    List<Map<String, Object>> queryForList(String sql, Map<String, Object> params);
    
    List<Map<String, Object>> queryForList(String sql, Object[] params);
    
    Map<String, Object> queryForMap(String sql, Map<String, Object> params);
    
    <T> T queryForObject(String sql, Map<String, Object> params, Class<T> requiredType);
    
    void execute(String sql);
    
    int update(String sql, Map<String, Object> params);
    
    int update(String sql, Object[] params);
    
    int[] batchUpdate(String sql, List<Object[]> paramsList);

    default void executeInTransaction(Runnable action) {
        action.run();
    }
    default boolean hasActiveTransaction() { return false; }

    /** Opaque, non-reused instance token for held storage views; never a URL or transaction handle. */
    default Object storageIdentity() { return null; }
}
