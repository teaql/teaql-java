package io.teaql.provider.jdbc;

import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.dataservice.sql.SqlRowMapper;
import io.teaql.core.TransactionCompletion;
import io.teaql.core.TransactionCompletionException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class JdbcSqlExecutor implements SqlExecutionAdapter {

    private final DataSource dataSource;
    // A held view retains only this token, not the executor, connection or DataSource.
    private final Object storageIdentity = new Object();
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    private static final class TransactionIdentity {
        final List<TransactionCompletion> completions=new ArrayList<>();
        boolean rollbackOnly;
    }
    private final ThreadLocal<TransactionIdentity> transactionIdentity = new ThreadLocal<>();
    private final List<Consumer<Connection>> connectionInitializers = new CopyOnWriteArrayList<>();

    public JdbcSqlExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object storageIdentity() { return storageIdentity; }

    public JdbcSqlExecutor addConnectionInitializer(Consumer<Connection> initializer) {
        connectionInitializers.add(initializer);
        return this;
    }

    private Connection openConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            for (Consumer<Connection> initializer : connectionInitializers) initializer.accept(connection);
            return connection;
        } catch (RuntimeException exception) {
            try { connection.close(); } catch (SQLException ignored) { }
            throw exception;
        }
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
        return streamQuery(sql, params, rows -> {
            String[] columnLabels = columnLabels(rows);
            JdbcColumnRow.Layout layout = new JdbcColumnRow.Layout(columnLabels);
            return () -> readColumnRow(rows, layout, columnLabels.length);
        });
    }

    @Override public boolean supportsCompiledStreamMapping() { return true; }

    @Override
    public <T extends io.teaql.core.Entity> Stream<T> queryForStream(
            String sql, Object[] params, io.teaql.core.CompiledRowMapper<T> mapper) {
        return streamQuery(sql, params, rows -> {
            io.teaql.core.DataRow row = new JdbcDataRow(rows);
            return () -> mapper.map(row);
        });
    }

    @FunctionalInterface private interface RowReader<T> { T read() throws SQLException; }
    @FunctionalInterface private interface RowReaderFactory<T> { RowReader<T> create(ResultSet rows) throws SQLException; }

    private <T> Stream<T> streamQuery(String sql, Object[] params, RowReaderFactory<T> factory) {
        StreamResources resources = new StreamResources();
        try {
            Connection connection = resources.connection = openConnection();
            PreparedStatement ps = resources.statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(200);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    bind(ps, i + 1, params[i]);
                }
            }
            ResultSet rs = resources.resultSet = ps.executeQuery();
            RowReader<T> reader = factory.create(rs);
            java.util.Iterator<T> iterator = new java.util.Iterator<>() {
                private boolean ready;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!ready) {
                        try {
                            hasNext = rs.next();
                            ready = true;
                        } catch (SQLException e) {
                            resources.close();
                            throw new RuntimeException(e);
                        }
                    }
                    if (!hasNext) resources.close();
                    return hasNext;
                }

                @Override
                public T next() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    ready = false;
                    try {
                        return reader.read();
                    } catch (SQLException e) {
                        resources.close();
                        throw new RuntimeException(e);
                    } catch (RuntimeException | Error failure) {
                        resources.close();
                        throw failure;
                    }
                }
            };
            return java.util.stream.StreamSupport.stream(java.util.Spliterators.spliteratorUnknownSize(iterator, java.util.Spliterator.ORDERED), false)
                    .onClose(resources::close);
        } catch (SQLException e) {
            resources.close();
            throw new RuntimeException("JDBC streaming query failed", e);
        } catch (RuntimeException | Error failure) {
            resources.close();
            throw failure;
        }
    }

    private static final class StreamResources {
        private Connection connection;
        private PreparedStatement statement;
        private ResultSet resultSet;
        private boolean closed;

        private void close() {
            if (closed) return;
            closed = true;
            // Retain the adapter's existing best-effort cleanup policy, including partially acquired resources.
            try { if (resultSet != null) resultSet.close(); } catch (SQLException ignored) { }
            try { if (statement != null) statement.close(); } catch (SQLException ignored) { }
            try { if (connection != null) connection.close(); } catch (SQLException ignored) { }
        }
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
            if (connection == null) { connection = openConnection(); owned = true; }
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    bind(ps, i + 1, params[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                String[] columnLabels = columnLabels(rs);
                JdbcColumnRow.Layout layout = new JdbcColumnRow.Layout(columnLabels);
                while (rs.next()) {
                    result.add(readColumnRow(rs, layout, columnLabels.length));
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
    public <T extends io.teaql.core.Entity> List<T> query(
            String sql, Object[] params, io.teaql.core.CompiledRowMapper<T> rowMapper) {
        List<T> result = new ArrayList<>();
        Connection connection = null;
        boolean owned = false;
        try {
            connection = transactionConnection.get();
            if (connection == null) { connection = openConnection(); owned = true; }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) bind(statement, i + 1, params[i]);
                }
                try (ResultSet rows = statement.executeQuery()) {
                    io.teaql.core.DataRow dataRow = new JdbcDataRow(rows);
                    while (rows.next()) result.add(rowMapper.map(dataRow));
                }
            }
            return result;
        } catch (SQLException exception) {
            throw new RuntimeException("JDBC typed query failed for parameterized SQL: " + sql, exception);
        } finally {
            if (owned && connection != null) try { connection.close(); } catch (SQLException ignored) { }
        }
    }

    private record JdbcDataRow(ResultSet resultSet) implements io.teaql.core.DataRow {
        @Override
        public Object get(int columnIndex) {
            try {
                return resultSet.getObject(columnIndex);
            } catch (SQLException exception) {
                throw new RuntimeException("Failed to read JDBC column " + columnIndex, exception);
            }
        }

        @Override
        public <V> V get(int columnIndex, Class<V> type) {
            try {
                Object value = resultSet.getObject(columnIndex);
                return convertColumn(type, value);
            } catch (SQLException exception) {
                throw new RuntimeException("Failed to read JDBC column " + columnIndex + " as "
                        + type.getName(), exception);
            }
        }

        @SuppressWarnings("unchecked")
        private static <V> V convertColumn(Class<V> type, Object value) {
            if (value == null || type.isInstance(value)) return (V) value;
            if (value instanceof Number number) {
                if (type == Long.class) return (V) Long.valueOf(number.longValue());
                if (type == Integer.class) return (V) Integer.valueOf(number.intValue());
                if (type == Double.class) return (V) Double.valueOf(number.doubleValue());
                if (type == Float.class) return (V) Float.valueOf(number.floatValue());
                if (type == Short.class) return (V) Short.valueOf(number.shortValue());
                if (type == Byte.class) return (V) Byte.valueOf(number.byteValue());
                if (type == java.math.BigDecimal.class) {
                    return (V) (number instanceof java.math.BigInteger integer
                            ? new java.math.BigDecimal(integer)
                            : java.math.BigDecimal.valueOf(number.doubleValue()));
                }
                if (type == Boolean.class) return (V) Boolean.valueOf(number.longValue() != 0);
            }
            if (type == java.time.LocalDateTime.class && value instanceof java.sql.Timestamp timestamp) {
                return (V) timestamp.toLocalDateTime();
            }
            if (type == java.time.LocalDateTime.class) {
                return (V) java.time.LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
            }
            if (type == java.time.LocalDate.class && value instanceof java.sql.Date date) {
                return (V) date.toLocalDate();
            }
            if (type == java.time.LocalDate.class && value instanceof java.sql.Timestamp timestamp) {
                return (V) timestamp.toLocalDateTime().toLocalDate();
            }
            if (type == java.time.LocalDate.class) {
                String text = String.valueOf(value);
                return (V) java.time.LocalDate.parse(text.substring(0, Math.min(10, text.length())));
            }
            if (type == java.time.LocalTime.class && value instanceof java.sql.Time time) {
                return (V) time.toLocalTime();
            }
            if (type == java.time.LocalTime.class && value instanceof java.sql.Timestamp timestamp) {
                return (V) timestamp.toLocalDateTime().toLocalTime();
            }
            if (type == java.time.LocalTime.class) {
                String text = String.valueOf(value);
                int separator = Math.max(text.indexOf('T'), text.indexOf(' '));
                return (V) java.time.LocalTime.parse(separator < 0 ? text : text.substring(separator + 1));
            }
            if (type == String.class) return (V) String.valueOf(value);
            throw new IllegalArgumentException("Cannot convert JDBC value " + value.getClass().getName()
                    + " to " + type.getName());
        }
    }

    private static JdbcColumnRow readColumnRow(ResultSet resultSet, JdbcColumnRow.Layout layout, int width)
            throws SQLException {
        Object[] values = new Object[width];
        for (int i = 0; i < width; i++) values[i] = resultSet.getObject(i + 1);
        return new JdbcColumnRow(layout, values);
    }

    private static String[] columnLabels(ResultSet resultSet) throws SQLException {
        java.sql.ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        String[] labels = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            String label = metadata.getColumnLabel(i + 1);
            labels[i] = label == null ? null : label.toLowerCase(java.util.Locale.ROOT);
        }
        return labels;
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
            if (connection == null) { connection = openConnection(); owned = true; }
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
            if (connection == null) { connection = openConnection(); owned = true; }
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
            if (connection == null) { connection = openConnection(); owned = true; }
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
        Objects.requireNonNull(action);
        if (transactionConnection.get() != null) {
            try { action.run(); }
            catch (RuntimeException | Error failure) {
                transactionIdentity.get().rollbackOnly=true;
                throw failure;
            }
            return;
        }
        TransactionIdentity identity=new TransactionIdentity();
        TransactionCompletion.Outcome outcome=TransactionCompletion.Outcome.ROLLED_BACK;
        Connection connection=null; Boolean autoCommit=null;
        boolean active=false,settled=false;
        Throwable failure=null;
        try {
            connection=openConnection(); autoCommit=connection.getAutoCommit();
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            transactionIdentity.set(identity); active=true;
            action.run();
            if (identity.rollbackOnly) throw new TransactionCompletionException(TransactionCompletion.Outcome.ROLLED_BACK,null);
            outcome=TransactionCompletion.Outcome.UNKNOWN;
            connection.commit();
            outcome=TransactionCompletion.Outcome.COMMITTED; settled=true;
        } catch (SQLException | RuntimeException | Error operationFailure) {
            failure=operationFailure;
            if (active && outcome!=TransactionCompletion.Outcome.COMMITTED) {
                try { connection.rollback(); settled=true; }
                catch (SQLException | RuntimeException | Error rollbackFailure) {
                    outcome=TransactionCompletion.Outcome.UNKNOWN;
                    failure=appendFailure(failure,rollbackFailure);
                }
            }
        } finally {
            transactionConnection.remove(); transactionIdentity.remove();
            if (connection!=null) {
                // Changing autoCommit may itself commit. Never do so until a commit
                // or rollback returned successfully, including after commit errors.
                if (settled && autoCommit!=null) {
                    try { connection.setAutoCommit(autoCommit); }
                    catch (SQLException | RuntimeException | Error resetFailure) { failure=appendFailure(failure,resetFailure); }
                }
                try { connection.close(); }
                catch (SQLException | RuntimeException | Error closeFailure) { failure=appendFailure(failure,closeFailure); }
            }
        }
        try {
            int size=identity.completions.size();
            for (int i=0;i<size;i++) {
                int index=outcome==TransactionCompletion.Outcome.COMMITTED?i:size-1-i;
                try { identity.completions.get(index).beforeNotifications(outcome); }
                catch (RuntimeException | Error preparationFailure) { failure=appendFailure(failure,preparationFailure); }
            }
            for (int i=0;i<size;i++) {
                int index=outcome==TransactionCompletion.Outcome.COMMITTED?i:size-1-i;
                try { identity.completions.get(index).afterCompletion(outcome); }
                catch (RuntimeException | Error notificationFailure) { failure=appendFailure(failure,notificationFailure); }
            }
        } finally { identity.completions.clear(); }
        if (failure instanceof Error fatal) throw fatal;
        if (failure!=null) {
            if (outcome==TransactionCompletion.Outcome.ROLLED_BACK && failure instanceof RuntimeException known) throw known;
            throw new TransactionCompletionException(outcome,failure);
        }
    }

    private static Throwable appendFailure(Throwable first,Throwable next) {
        if (first==null) return next;
        if (next instanceof Error && !(first instanceof Error)) { next.addSuppressed(first); return next; }
        if (first!=next) first.addSuppressed(next);
        return first;
    }

    @Override public boolean supportsTransactionCompletion() { return true; }
    @Override public void afterTransaction(TransactionCompletion completion) {
        TransactionIdentity identity=transactionIdentity.get();
        if (identity==null || transactionConnection.get()==null) throw new IllegalStateException("An active physical transaction is required");
        identity.completions.add(Objects.requireNonNull(completion));
    }

    @Override public boolean hasActiveTransaction() { return transactionConnection.get() != null; }
    @Override public Object activeTransactionIdentity() { return transactionIdentity.get(); }

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
