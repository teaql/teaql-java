package com.example.exactdecimalservice;

import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.util.List;
import java.util.Map;

/** SQL bridge used by framework services such as the persistent ID allocator. */
public final class WorkspaceTeaQLDatabase implements TeaQLDatabase {
    private final SqlExecutionAdapter executor;
    private final javax.sql.DataSource dataSource;

    public WorkspaceTeaQLDatabase(SqlExecutionAdapter executor, javax.sql.DataSource dataSource) {
        this.executor = executor;
        this.dataSource = dataSource;
    }

    @Override public List<Map<String, Object>> query(String sql, Object[] args) {
        return executor.queryForList(sql, args);
    }
    @Override public int executeUpdate(String sql, Object[] args) {
        return executor.update(sql, args);
    }
    @Override public int[] batchUpdate(String sql, List<Object[]> args) {
        return executor.batchUpdate(sql, args);
    }
    @Override public void execute(String sql) { executor.execute(sql); }
    @Override public void executeInTransaction(Runnable action) {
        executor.executeInTransaction(action);
    }
    @Override public List<Map<String, Object>> getTableColumns(String tableName) {
        try (var connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            var columns = new java.util.ArrayList<Map<String, Object>>();
            for (String name : List.of(tableName, tableName.toUpperCase(java.util.Locale.ROOT))) {
                try (var rows = metadata.getColumns(connection.getCatalog(), connection.getSchema(), name, null)) {
                    while (rows.next()) {
                        // JDBC table names are patterns: do not accept underscore wildcard matches.
                        if (!name.equals(rows.getString("TABLE_NAME"))) continue;
                        Map<String, Object> column = new java.util.HashMap<>();
                        column.put("column_name", rows.getString("COLUMN_NAME"));
                        column.put("data_type", rows.getInt("DATA_TYPE"));
                        column.put("type_name", rows.getString("TYPE_NAME"));
                        column.put("column_size", rows.getInt("COLUMN_SIZE"));
                        column.put("decimal_digits", rows.getInt("DECIMAL_DIGITS"));
                        column.put("nullable", rows.getInt("NULLABLE"));
                        columns.add(column);
                    }
                }
                if (!columns.isEmpty()) break;
            }
            return columns;
        } catch (java.sql.SQLException failure) {
            throw new IllegalStateException("Cannot inspect table columns: " + tableName, failure);
        }
    }

    @Override public java.util.Optional<List<String>> getTablePrimaryKeyColumns(String tableName) {
        try (var connection = dataSource.getConnection()) {
            for (String name : List.of(tableName, tableName.toUpperCase(java.util.Locale.ROOT))) {
                var ordered = new java.util.TreeMap<Short, String>();
                try (var rows = connection.getMetaData().getPrimaryKeys(
                        connection.getCatalog(), connection.getSchema(), name)) {
                    while (rows.next()) {
                        ordered.put(rows.getShort("KEY_SEQ"), rows.getString("COLUMN_NAME"));
                    }
                }
                if (!ordered.isEmpty()) return java.util.Optional.of(List.copyOf(ordered.values()));
            }
            return java.util.Optional.of(List.of());
        } catch (java.sql.SQLException failure) {
            throw new IllegalStateException("Cannot inspect table primary key: " + tableName, failure);
        }
    }
}