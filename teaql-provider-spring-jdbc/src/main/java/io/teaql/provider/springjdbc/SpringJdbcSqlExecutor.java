package io.teaql.provider.springjdbc;

import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.dataservice.sql.SqlRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class SpringJdbcSqlExecutor implements SqlExecutionAdapter {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public SpringJdbcSqlExecutor(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(
                Objects.requireNonNull(jdbcTemplate.getJdbcTemplate().getDataSource(),
                        "A DataSource is required for transactional SQL execution")));
    }

    @Override
    public <T> List<T> query(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
        return jdbcTemplate.query(sql, params, rowMapper::mapRow);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
        return jdbcTemplate.queryForStream(sql, params, rowMapper::mapRow);
    }

    @Override
    public Stream<Map<String, Object>> queryForStream(String sql, Object[] params) {
        return jdbcTemplate.getJdbcOperations().queryForStream(sql, (rs, rowNum) -> {
            Map<String,Object> row = new java.util.HashMap<>();
            for (int i=1;i<=rs.getMetaData().getColumnCount();i++) row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(), rs.getObject(i));
            return row;
        }, params);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, Object> params) {
        return jdbcTemplate.queryForList(sql, params);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object[] params) {
        return jdbcTemplate.getJdbcOperations().queryForList(sql, params);
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Map<String, Object> params) {
        return jdbcTemplate.queryForMap(sql, params);
    }

    @Override
    public <T> T queryForObject(String sql, Map<String, Object> params, Class<T> requiredType) {
        return jdbcTemplate.queryForObject(sql, params, requiredType);
    }

    @Override
    public void execute(String sql) {
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    @Override
    public int update(String sql, Map<String, Object> params) {
        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int update(String sql, Object[] params) {
        return jdbcTemplate.getJdbcTemplate().update(sql, params);
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> paramsList) {
        return jdbcTemplate.getJdbcTemplate().batchUpdate(sql, paramsList);
    }

    @Override
    public void executeInTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
