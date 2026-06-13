package io.teaql.provider.jdbc;

import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.dataservice.sql.SqlRowMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class JdbcSqlExecutor implements SqlExecutionAdapter {

    private final DataSource dataSource;

    public JdbcSqlExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> List<T> query(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
        // Simple placeholder for named parameters translation and execution
        throw new UnsupportedOperationException("Not fully implemented yet");
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
        throw new UnsupportedOperationException("Not fully implemented yet");
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, Object> params) {
        throw new UnsupportedOperationException("Not fully implemented yet");
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object[] params) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                int columnCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Map<String, Object> params) {
        throw new UnsupportedOperationException("Not fully implemented yet");
    }

    @Override
    public <T> T queryForObject(String sql, Map<String, Object> params, Class<T> requiredType) {
        throw new UnsupportedOperationException("Not fully implemented yet");
    }

    @Override
    public void execute(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int update(String sql, Map<String, Object> params) {
        throw new UnsupportedOperationException("Not fully implemented yet");
    }

    @Override
    public int update(String sql, Object[] params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> paramsList) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Object[] params : paramsList) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
