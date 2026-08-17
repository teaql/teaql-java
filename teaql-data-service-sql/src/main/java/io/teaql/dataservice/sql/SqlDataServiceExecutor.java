package io.teaql.dataservice.sql;

import io.teaql.core.UserContext;
import io.teaql.core.DataServiceCapabilities;
import io.teaql.core.MutationExecutor;
import io.teaql.core.MutationRequest;
import io.teaql.core.MutationResult;
import io.teaql.core.QueryExecutor;
import io.teaql.core.QueryRequest;
import io.teaql.core.QueryResult;
import io.teaql.core.SchemaExecutor;
import io.teaql.core.TransactionCallback;
import io.teaql.core.TransactionExecutor;

public class SqlDataServiceExecutor implements QueryExecutor, io.teaql.core.StreamingQueryExecutor, MutationExecutor, TransactionExecutor, SchemaExecutor {
    private final String name;
    private final SqlExecutionAdapter executionAdapter;
    private final DataServiceCapabilities capabilities;
    protected io.teaql.core.sql.dialect.SqlDialect dialect = new io.teaql.core.sql.dialect.PostgreSqlDialect();

    public SqlDataServiceExecutor(String name, SqlExecutionAdapter executionAdapter) {
        this.name = name;
        this.executionAdapter = executionAdapter;
        this.capabilities = new DataServiceCapabilities();
        this.capabilities.setQuery(true);
        this.capabilities.setStreamingQuery(true);
        this.capabilities.setMutation(true);
        this.capabilities.setBatchMutation(true);
        this.capabilities.setAggregation(true);
        this.capabilities.setTransaction(true);
        this.capabilities.setSchema(true);
        this.capabilities.setRelationLoad(true);
        this.capabilities.setRelationMutation(true);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public DataServiceCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public QueryResult query(UserContext ctx, QueryRequest request) {
        return getPortableService().query(ctx, request);
    }

    @Override
    public <T extends io.teaql.core.Entity> java.util.stream.Stream<T> queryForStream(UserContext ctx, io.teaql.core.SearchRequest<T> request) {
        return getPortableService().queryForStream(ctx, request);
    }

    @Override
    public MutationResult mutate(UserContext ctx, MutationRequest request) {
        return getPortableService().mutate(ctx, request);
    }

    @Override
    public <T> T executeInTransaction(UserContext ctx, TransactionCallback<T> action) {
        return getPortableService().executeInTransaction(ctx, action);
    }

    @Override
    public void ensureSchema(UserContext ctx) {
    }

    public SqlExecutionAdapter getExecutionAdapter() {
        return executionAdapter;
    }

    // Lazy load the portable service
    private io.teaql.core.sql.portable.PortableSQLDataService portableService;

    private synchronized io.teaql.core.sql.portable.PortableSQLDataService getPortableService() {
        if (portableService == null) {
            io.teaql.core.sql.portable.TeaQLDatabase dbAdapter = new io.teaql.core.sql.portable.TeaQLDatabase() {
                @Override
                public java.util.List<java.util.Map<String, Object>> query(String sql, Object[] args) {
                    return executionAdapter.queryForList(sql, args);
                }
                @Override
                public java.util.stream.Stream<java.util.Map<String, Object>> queryForStream(io.teaql.core.UserContext ctx, String sql, Object[] args) {
                    return executionAdapter.queryForStream(sql, args);
                }
                @Override
                public int executeUpdate(String sql, Object[] args) {
                    return executionAdapter.update(sql, args);
                }
                @Override
                public int[] batchUpdate(String sql, java.util.List<Object[]> batchArgs) {
                    return executionAdapter.batchUpdate(sql, batchArgs);
                }
                @Override
                public void execute(String sql) {
                    executionAdapter.execute(sql);
                }
                @Override
                public void executeInTransaction(Runnable action) {
                    executionAdapter.executeInTransaction(action);
                }
                @Override
                public java.util.List<java.util.Map<String, Object>> getTableColumns(String tableName) {
                    throw new UnsupportedOperationException("Implement in specific dialect");
                }

                @Override
                public java.util.List<java.util.Map<String, Object>> query(io.teaql.core.UserContext ctx, String sql, Object[] args) {
                    long start = System.nanoTime();
                    java.util.List<java.util.Map<String, Object>> res = executionAdapter.queryForList(sql, args);
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend("SQL-" + name);
                    meta.setOperation(io.teaql.core.DataServiceOperation.QUERY);
                    meta.setElapsedUs(elapsed);
                    meta.setResultCount(res.size());
                    meta.setResultSummary("Fetched " + res.size() + " rows");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(parameters(args));
                    meta.setDebugQuery(formatSqlWithArgs(sql, args));
                    ctx.recordExecutionMetadata(meta);
                    return res;
                }

                @Override
                public int executeUpdate(io.teaql.core.UserContext ctx, String sql, Object[] args) {
                    long start = System.nanoTime();
                    int res = executionAdapter.update(sql, args);
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend("SQL-" + name);
                    meta.setOperation(io.teaql.core.DataServiceOperation.MUTATION);
                    meta.setElapsedUs(elapsed);
                    meta.setAffectedRows((long) res);
                    meta.setResultSummary("Affected " + res + " rows");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(parameters(args));
                    meta.setDebugQuery(formatSqlWithArgs(sql, args));
                    ctx.recordExecutionMetadata(meta);
                    return res;
                }

                @Override
                public int[] batchUpdate(io.teaql.core.UserContext ctx, String sql, java.util.List<Object[]> batchArgs) {
                    long start = System.nanoTime();
                    int[] res = executionAdapter.batchUpdate(sql, batchArgs);
                    long elapsed = (System.nanoTime() - start) / 1000;
                    int total = 0; if (res != null) { for(int i: res) total += i; }
                    String loggedSql = sql;
                    if (batchArgs != null && !batchArgs.isEmpty()) {
                        loggedSql = formatSqlWithArgs(sql, batchArgs.get(0));
                        if (batchArgs.size() > 1) {
                            loggedSql += " /* + " + (batchArgs.size() - 1) + " more batches */";
                        }
                    }
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend("SQL-" + name);
                    meta.setOperation(io.teaql.core.DataServiceOperation.MUTATION);
                    meta.setElapsedUs(elapsed);
                    meta.setAffectedRows((long) total);
                    meta.setResultSummary("Batch affected " + total + " rows");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(batchParameters(batchArgs));
                    meta.setDebugQuery(loggedSql);
                    ctx.recordExecutionMetadata(meta);
                    return res;
                }

                @Override
                public void execute(io.teaql.core.UserContext ctx, String sql) {
                    long start = System.nanoTime();
                    executionAdapter.execute(sql);
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend("SQL-" + name);
                    meta.setOperation(io.teaql.core.DataServiceOperation.SCHEMA);
                    meta.setElapsedUs(elapsed);
                    meta.setResultSummary("Executed");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(java.util.List.of());
                    meta.setDebugQuery(sql);
                    ctx.recordExecutionMetadata(meta);
                }
            };
            portableService = new io.teaql.core.sql.portable.PortableSQLDataService(name, dbAdapter, io.teaql.core.meta.EntityMetaFactory.get());
            portableService.setDialect(this.dialect);
        }
        return portableService;
    }

    private static String formatSqlWithArgs(String sql, Object[] args) {
        if (sql == null || args == null || args.length == 0) {
            return sql;
        }
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        boolean inString = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'') {
                inString = !inString;
                sb.append(c);
                continue;
            }
            if (c == '?' && !inString && argIndex < args.length) {
                Object arg = args[argIndex++];
                if (arg == null) {
                    sb.append("NULL");
                    continue;
                }
                if (arg instanceof String || arg instanceof java.util.Date || arg instanceof java.time.temporal.Temporal) {
                    sb.append("'").append(arg.toString().replace("'", "''")).append("'");
                    continue;
                }
                sb.append(arg.toString());
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static java.util.List<Object> parameters(Object[] args) {
        if (args == null || args.length == 0) {
            return java.util.List.of();
        }
        return new java.util.ArrayList<>(java.util.Arrays.asList(args));
    }

    private static java.util.List<Object> batchParameters(java.util.List<Object[]> batchArgs) {
        if (batchArgs == null || batchArgs.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<Object> result = new java.util.ArrayList<>();
        for (Object[] args : batchArgs) {
            if (args != null) {
                java.util.Collections.addAll(result, args);
            }
        }
        return result;
    }
}
