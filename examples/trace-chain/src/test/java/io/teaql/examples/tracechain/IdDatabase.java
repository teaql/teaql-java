package io.teaql.examples.tracechain;

import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.util.List;
import java.util.Map;

/** Infrastructure bridge only: business data uses generated Q/E/Mutation APIs. */
final class IdDatabase implements TeaQLDatabase {
    private final JdbcSqlExecutor executor;
    IdDatabase(JdbcSqlExecutor executor) { this.executor = executor; }
    @Override public List<Map<String, Object>> query(String sql, Object[] args) {
        return executor.queryForList(sql, args);
    }
    @Override public int executeUpdate(String sql, Object[] args) { return executor.update(sql, args); }
    @Override public int[] batchUpdate(String sql, List<Object[]> args) { return executor.batchUpdate(sql, args); }
    @Override public void execute(String sql) { executor.execute(sql); }
    @Override public void executeInTransaction(Runnable action) { executor.executeInTransaction(action); }
    @Override public List<Map<String, Object>> getTableColumns(String table) {
        throw new UnsupportedOperationException("Schema inspection belongs to the SQLite data service");
    }
}
