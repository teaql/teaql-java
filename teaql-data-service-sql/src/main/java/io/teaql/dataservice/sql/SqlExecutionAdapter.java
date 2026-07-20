package io.teaql.dataservice.sql;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface SqlExecutionAdapter {
    
    <T> List<T> query(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper);
    
    <T> Stream<T> queryForStream(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper);
    
    List<Map<String, Object>> queryForList(String sql, Map<String, Object> params);
    
    List<Map<String, Object>> queryForList(String sql, Object[] params);
    
    Map<String, Object> queryForMap(String sql, Map<String, Object> params);
    
    <T> T queryForObject(String sql, Map<String, Object> params, Class<T> requiredType);
    
    void execute(String sql);
    
    int update(String sql, Map<String, Object> params);
    
    int update(String sql, Object[] params);
    
    int[] batchUpdate(String sql, List<Object[]> paramsList);
}
