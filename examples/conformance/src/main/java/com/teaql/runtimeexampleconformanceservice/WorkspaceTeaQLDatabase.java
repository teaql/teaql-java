package com.teaql.runtimeexampleconformanceservice;

import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.util.List;
import java.util.Map;

/** SQL bridge used by framework services such as the persistent ID allocator. */
public final class WorkspaceTeaQLDatabase implements TeaQLDatabase {
    private final SqlExecutionAdapter executor;

    public WorkspaceTeaQLDatabase(SqlExecutionAdapter executor) {
        this.executor = executor;
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
        throw new UnsupportedOperationException("Schema inspection belongs to the data-service dialect");
    }
}