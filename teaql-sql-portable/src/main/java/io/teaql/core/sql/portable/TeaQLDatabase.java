package io.teaql.core.sql.portable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * TeaQL database abstraction layer.
 * Android uses SQLiteDatabase, JVM uses JDBC.
 * No dependency on spring-jdbc or javax.sql.DataSource.
 */
public interface TeaQLDatabase {

    /**
     * Execute a query and return a list of rows. Each row is a Map (column name -> value).
     */
    List<Map<String, Object>> query(String sql, Object[] args);
    default List<Map<String, Object>> query(io.teaql.core.UserContext context, String sql, Object[] args) {
        return query(sql, args);
    }
    default <T extends io.teaql.core.Entity> List<T> query(
            io.teaql.core.UserContext context, String sql, Object[] args,
            io.teaql.core.CompiledRowMapper<T> rowMapper) {
        throw new UnsupportedOperationException("compiled row mapping is not supported");
    }
    default Stream<Map<String, Object>> queryForStream(io.teaql.core.UserContext context, String sql, Object[] args) {
        throw new UnsupportedOperationException("streaming query is not supported");
    }

    /**
     * Execute an update (INSERT/UPDATE/DELETE) and return the number of affected rows.
     */
    int executeUpdate(String sql, Object[] args);
    default int executeUpdate(io.teaql.core.UserContext context, String sql, Object[] args) {
        return executeUpdate(sql, args);
    }

    /**
     * Execute a batch update.
     */
    int[] batchUpdate(String sql, List<Object[]> batchArgs);
    default int[] batchUpdate(io.teaql.core.UserContext context, String sql, List<Object[]> batchArgs) {
        return batchUpdate(sql, batchArgs);
    }

    /**
     * Execute arbitrary SQL (DDL, etc.).
     */
    void execute(String sql);
    default void execute(io.teaql.core.UserContext context, String sql) {
        execute(sql);
    }

    /**
     * Execute an operation within a transaction.
     */
    void executeInTransaction(Runnable action);
    default void executeInTransaction(io.teaql.core.UserContext context, Runnable action) {
        executeInTransaction(action);
    }

    /**
     * Get column information for a database table.
     */
    List<Map<String, Object>> getTableColumns(String tableName);
}
