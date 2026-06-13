package io.teaql.core.sql.portable;

import java.util.List;
import java.util.Map;

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

    /**
     * Execute an update (INSERT/UPDATE/DELETE) and return the number of affected rows.
     */
    int executeUpdate(String sql, Object[] args);

    /**
     * Execute a batch update.
     */
    int[] batchUpdate(String sql, List<Object[]> batchArgs);

    /**
     * Execute arbitrary SQL (DDL, etc.).
     */
    void execute(String sql);

    /**
     * Execute an operation within a transaction.
     */
    void executeInTransaction(Runnable action);

    /**
     * Get column information for a database table.
     */
    List<Map<String, Object>> getTableColumns(String tableName);
}
