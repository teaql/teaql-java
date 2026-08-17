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
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

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
    public Stream<Map<String, Object>> queryForStream(String sql, Object[] params) {
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(200);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    bind(ps, i + 1, params[i]);
                }
            }
            ResultSet rs = ps.executeQuery();
            java.util.Iterator<Map<String, Object>> iterator = new java.util.Iterator<>() {
                private boolean ready;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!ready) {
                        try {
                            hasNext = rs.next();
                            ready = true;
                        } catch (SQLException e) {
                            closeResources(rs, ps, connection);
                            throw new RuntimeException(e);
                        }
                    }
                    if (!hasNext) closeResources(rs, ps, connection);
                    return hasNext;
                }

                @Override
                public Map<String, Object> next() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    ready = false;
                    try {
                        Map<String, Object> row = new java.util.HashMap<>();
                        int count = rs.getMetaData().getColumnCount();
                        for (int i = 1; i <= count; i++) {
                            row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(), rs.getObject(i));
                        }
                        return row;
                    } catch (SQLException e) {
                        closeResources(rs, ps, connection);
                        throw new RuntimeException(e);
                    }
                }
            };
            return java.util.stream.StreamSupport.stream(java.util.Spliterators.spliteratorUnknownSize(iterator, java.util.Spliterator.ORDERED), false)
                    .onClose(() -> closeResources(rs, ps, connection));
        } catch (SQLException e) {
            throw new RuntimeException("JDBC streaming query failed", e);
        }
    }

    private static void closeResources(ResultSet resultSet, PreparedStatement statement, Connection connection) {
        try { resultSet.close(); } catch (SQLException ignored) { }
        try { statement.close(); } catch (SQLException ignored) { }
        try { connection.close(); } catch (SQLException ignored) { }
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, Object> params) {
        throw new UnsupportedOperationException("Not fully implemented yet");
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object[] params) {
        List<Map<String, Object>> result = new ArrayList<>();
        Connection connection = null;
        boolean owned = false;
        try {
            connection = transactionConnection.get();
            if (connection == null) { connection = dataSource.getConnection(); owned = true; }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    bind(ps, i + 1, params[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                int columnCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String label = rs.getMetaData().getColumnLabel(i);
                        if (label != null) {
                            label = label.toLowerCase();
                        }
                        row.put(label, rs.getObject(i));
                    }
                    result.add(row);
                }
            }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("JDBC query failed for parameterized SQL: " + sql, e);
        } finally {
            if (owned && connection != null) try { connection.close(); } catch (SQLException ignored) { }
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
        Connection connection = null;
        boolean owned = false;
        try {
            connection = transactionConnection.get();
            if (connection == null) { connection = dataSource.getConnection(); owned = true; }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (owned && connection != null) try { connection.close(); } catch (SQLException ignored) { }
        }
    }

    @Override
    public int update(String sql, Map<String, Object> params) {
        throw new UnsupportedOperationException("Not fully implemented yet");
    }

    @Override
    public int update(String sql, Object[] params) {
        Connection connection = null;
        boolean owned = false;
        try {
            connection = transactionConnection.get();
            if (connection == null) { connection = dataSource.getConnection(); owned = true; }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                bind(ps, i + 1, params[i]);
            }
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (owned && connection != null) try { connection.close(); } catch (SQLException ignored) { }
        }
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> paramsList) {
        Connection connection = null;
        boolean owned = false;
        try {
            connection = transactionConnection.get();
            if (connection == null) { connection = dataSource.getConnection(); owned = true; }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Object[] params : paramsList) {
                for (int i = 0; i < params.length; i++) {
                    bind(ps, i + 1, params[i]);
                }
                ps.addBatch();
            }
                return ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (owned && connection != null) try { connection.close(); } catch (SQLException ignored) { }
        }
    }

    @Override
    public void executeInTransaction(Runnable action) {
        if (transactionConnection.get() != null) {
            action.run();
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            try {
                action.run();
                connection.commit();
            } catch (RuntimeException | Error failure) {
                connection.rollback();
                throw failure;
            } finally {
                transactionConnection.remove();
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("JDBC transaction failed", e);
        }
    }

    static void bind(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value == null) {
            // Let the driver infer the target column type. DB2 rejects the
            // generic JDBC type code Types.NULL (0), while setObject(null)
            // preserves SQL NULL and uses prepared-statement metadata.
            statement.setObject(index, null);
        } else if (value instanceof Long number) {
            statement.setLong(index, number);
        } else if (value instanceof Integer number) {
            statement.setInt(index, number);
        } else if (value instanceof Short number) {
            statement.setShort(index, number);
        } else if (value instanceof Byte number) {
            statement.setByte(index, number);
        } else if (value instanceof java.math.BigDecimal number) {
            statement.setBigDecimal(index, number);
        } else if (value instanceof Double number) {
            statement.setDouble(index, number);
        } else if (value instanceof Float number) {
            statement.setFloat(index, number);
        } else if (value instanceof Boolean booleanValue) {
            statement.setBoolean(index, booleanValue);
        } else if (value instanceof String text) {
            statement.setString(index, text);
        } else if (value instanceof java.time.LocalDate date) {
            if (isSqlite(statement)) {
                statement.setString(index, date.toString());
            } else {
                statement.setDate(index, java.sql.Date.valueOf(date));
            }
        } else if (value instanceof java.time.LocalDateTime dateTime) {
            if (isSqlite(statement)) {
                statement.setString(index, java.sql.Timestamp.valueOf(dateTime).toString());
            } else {
                statement.setTimestamp(index, java.sql.Timestamp.valueOf(dateTime));
            }
        } else if (value instanceof java.time.LocalTime time) {
            if (isSqlite(statement)) {
                statement.setString(index, time.toString());
            } else {
                statement.setTime(index, java.sql.Time.valueOf(time));
            }
        } else {
            statement.setObject(index, value);
        }
    }

    private static boolean isSqlite(PreparedStatement statement) throws SQLException {
        String productName = statement.getConnection().getMetaData().getDatabaseProductName();
        return productName != null && productName.toLowerCase(java.util.Locale.ROOT).contains("sqlite");
    }
}
