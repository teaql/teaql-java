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
    protected String debugDatabaseKind = "postgresql";
    protected io.teaql.core.sql.portable.TopNRelationPlanPolicy topNRelationPlanPolicy =
            io.teaql.core.sql.portable.TopNRelationPlanPolicy.WINDOW;

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
    public QueryResult query(UserContext context, QueryRequest request) {
        return getPortableService().query(context, request);
    }

    @Override
    public <T extends io.teaql.core.Entity> java.util.stream.Stream<T> queryForStream(UserContext context, io.teaql.core.SearchRequest<T> request) {
        return getPortableService().queryForStream(context, request);
    }

    @Override
    public MutationResult mutate(UserContext context, MutationRequest request) {
        return getPortableService().mutate(context, request);
    }

    @Override
    public <T> T executeInTransaction(UserContext context, TransactionCallback<T> action) {
        return getPortableService().executeInTransaction(context, action);
    }

    @Override
    public void ensureSchema(UserContext context, io.teaql.core.SchemaExecutor.Invocation invocation) {
        io.teaql.core.SchemaExecutor.Invocation.requireContextOwned(invocation);
        throw new UnsupportedOperationException(
                "Schema initialization is not implemented by " + getClass().getName()
                        + " (database kind: " + debugDatabaseKind
                        + ", dialect: " + dialect.getClass().getSimpleName() + "). "
                        + "Use the database-specific executor "
                        + suggestedSchemaExecutor(debugDatabaseKind)
                        + " and call context.ensureSchema() explicitly.");
    }

    private static String suggestedSchemaExecutor(String databaseKind) {
        return switch (databaseKind == null ? "" : databaseKind.toLowerCase(java.util.Locale.ROOT)) {
            case "postgres", "postgresql" -> "io.teaql.core.postgres.PostgresDataServiceExecutor";
            case "mysql" -> "io.teaql.core.mysql.MysqlDataServiceExecutor";
            case "sqlite" -> "io.teaql.core.sqlite.SqliteDataServiceExecutor";
            case "oracle" -> "io.teaql.core.oracle.OracleDataServiceExecutor";
            case "mssql", "sqlserver" -> "io.teaql.core.mssql.MssqlDataServiceExecutor";
            case "db2" -> "io.teaql.core.db2.DB2DataServiceExecutor";
            case "hana" -> "io.teaql.core.hana.HanaDataServiceExecutor";
            case "dm8" -> "io.teaql.core.dm8.Dm8DataServiceExecutor";
            case "duckdb" -> "io.teaql.core.duck.DuckDataServiceExecutor";
            case "snowflake" -> "io.teaql.core.snowflake.SnowflakeDataServiceExecutor";
            default -> "a matching database-specific DataServiceExecutor";
        };
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
                public boolean supportsCompiledRowMapping() {
                    return true;
                }

                @Override
                public java.util.List<java.util.Map<String, Object>> query(String sql, Object[] args) {
                    return executionAdapter.queryForList(sql, args);
                }
                @Override
                public java.util.stream.Stream<java.util.Map<String, Object>> queryForStream(io.teaql.core.UserContext context, String sql, Object[] args) {
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
                public java.util.List<java.util.Map<String, Object>> query(io.teaql.core.UserContext context, String sql, Object[] args) {
                    boolean logging = context.isQueryExecutionLoggingEnabled();
                    long start = logging ? System.nanoTime() : 0L;
                    java.util.List<java.util.Map<String, Object>> res = executionAdapter.queryForList(sql, args);
                    if (!logging) return res;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.QUERY);
                    meta.setElapsedUs(elapsed);
                    meta.setResultCount(res.size());
                    meta.setResultSummary("Fetched " + res.size() + " rows");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(parameters(args));
                    meta.setDebugQuery(debugSql(sql, args, debugDatabaseKind));
                    context.recordExecutionMetadata(meta);
                    return res;
                }

                @Override
                public <T extends io.teaql.core.Entity> java.util.List<T> query(
                        io.teaql.core.UserContext context, String sql, Object[] args,
                        io.teaql.core.CompiledRowMapper<T> rowMapper) {
                    boolean logging = context.isQueryExecutionLoggingEnabled();
                    long start = logging ? System.nanoTime() : 0L;
                    java.util.List<T> res = executionAdapter.query(sql, args, rowMapper);
                    if (!logging) return res;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.QUERY);
                    meta.setElapsedUs(elapsed);
                    meta.setResultCount(res.size());
                    meta.setResultSummary("Fetched " + res.size() + " typed rows");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(parameters(args));
                    meta.setDebugQuery(debugSql(sql, args, debugDatabaseKind));
                    context.recordExecutionMetadata(meta);
                    return res;
                }

                @Override
                public int executeUpdate(io.teaql.core.UserContext context, String sql, Object[] args) {
                    boolean logging = context.isMutationExecutionLoggingEnabled();
                    long start = logging ? System.nanoTime() : 0L;
                    int res = executionAdapter.update(sql, args);
                    if (!logging) return res;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.MUTATION);
                    meta.setElapsedUs(elapsed);
                    meta.setAffectedRows((long) res);
                    meta.setResultSummary("Affected " + res + " rows");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(parameters(args));
                    meta.setDebugQuery(debugSql(sql, args, debugDatabaseKind));
                    context.recordExecutionMetadata(meta);
                    return res;
                }

                @Override
                public int[] batchUpdate(io.teaql.core.UserContext context, String sql, java.util.List<Object[]> batchArgs) {
                    boolean logging = context.isMutationExecutionLoggingEnabled();
                    long start = logging ? System.nanoTime() : 0L;
                    int[] res = executionAdapter.batchUpdate(sql, batchArgs);
                    if (!logging) return res;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    int total = 0; if (res != null) { for(int i: res) total += i; }
                    String loggedSql = sql;
                    if (batchArgs != null && !batchArgs.isEmpty()) {
                        loggedSql = debugSql(sql, batchArgs.get(0), debugDatabaseKind);
                        if (batchArgs.size() > 1) {
                            loggedSql += " /* + " + (batchArgs.size() - 1) + " more batches */";
                        }
                    }
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.MUTATION);
                    meta.setElapsedUs(elapsed);
                    meta.setAffectedRows((long) total);
                    meta.setResultSummary("Batch affected " + total + " rows");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(batchParameters(batchArgs));
                    meta.setDebugQuery(loggedSql);
                    context.recordExecutionMetadata(meta);
                    return res;
                }

                @Override
                public void execute(io.teaql.core.UserContext context, String sql) {
                    boolean logging = context.isMutationExecutionLoggingEnabled();
                    long start = logging ? System.nanoTime() : 0L;
                    executionAdapter.execute(sql);
                    if (!logging) return;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.SCHEMA);
                    meta.setElapsedUs(elapsed);
                    meta.setResultSummary("Executed");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(java.util.List.of());
                    meta.setDebugQuery(sql);
                    context.recordExecutionMetadata(meta);
                }
            };
            portableService = new io.teaql.core.sql.portable.PortableSQLDataService(name, dbAdapter, io.teaql.core.meta.EntityMetaFactory.get());
            portableService.setDialect(this.dialect);
            portableService.setTopNRelationPlanPolicy(this.topNRelationPlanPolicy);
        }
        return portableService;
    }

    public static String debugSql(String sql, Object[] args) {
        return debugSql(sql, args, "sqlite");
    }

    public static String debugSql(
            String sql, Object[] args, io.teaql.core.sql.dialect.SqlDialect dialect) {
        return debugSql(sql, args, dialect.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT));
    }

    public static String debugSql(String sql, Object[] args, String databaseKind) {
        if (sql == null || args == null || args.length == 0) {
            return sql;
        }
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        SqlScanState state = SqlScanState.SQL;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (state == SqlScanState.SQL && c == '\'') {
                sb.append(c);
                state = SqlScanState.SINGLE_QUOTE;
                continue;
            }
            if (state == SqlScanState.SQL && c == '"') {
                sb.append(c);
                state = SqlScanState.DOUBLE_QUOTE;
                continue;
            }
            if (state == SqlScanState.SQL && c == '-' && next == '-') {
                sb.append("--"); i++; state = SqlScanState.LINE_COMMENT; continue;
            }
            if (state == SqlScanState.SQL && c == '/' && next == '*') {
                sb.append("/*"); i++; state = SqlScanState.BLOCK_COMMENT; continue;
            }
            if (state == SqlScanState.SINGLE_QUOTE) {
                sb.append(c);
                if (c == '\'' && next == '\'') { sb.append(next); i++; }
                else if (c == '\'') { state = SqlScanState.SQL; }
                continue;
            }
            if (state == SqlScanState.DOUBLE_QUOTE) {
                sb.append(c);
                if (c == '"' && next == '"') { sb.append(next); i++; }
                else if (c == '"') { state = SqlScanState.SQL; }
                continue;
            }
            if (state == SqlScanState.LINE_COMMENT) {
                sb.append(c);
                if (c == '\r' || c == '\n') { state = SqlScanState.SQL; }
                continue;
            }
            if (state == SqlScanState.BLOCK_COMMENT) {
                sb.append(c);
                if (c == '*' && next == '/') { sb.append(next); i++; state = SqlScanState.SQL; }
                continue;
            }
            if (c == '?' && argIndex < args.length) {
                Object arg = args[argIndex++];
                if (arg == null) {
                    sb.append("NULL");
                    continue;
                }
                if (arg instanceof java.time.LocalDate date) {
                    String literal = "'" + date + "'";
                    if (databaseKind.contains("mysql") || databaseKind.contains("mssql")) sb.append("CAST(").append(literal).append(" AS DATE)");
                    else if (databaseKind.contains("sqlite")) sb.append(literal);
                    else sb.append("DATE ").append(literal);
                    continue;
                }
                if (arg instanceof java.time.LocalDateTime dateTime) {
                    String literal = "'" + java.sql.Timestamp.valueOf(dateTime) + "'";
                    if (databaseKind.contains("mysql")) sb.append("CAST(").append(literal).append(" AS DATETIME(3))");
                    else if (databaseKind.contains("mssql")) sb.append("CAST(").append(literal).append(" AS DATETIME2(3))");
                    else if (databaseKind.contains("sqlite")) sb.append(literal);
                    else sb.append("TIMESTAMP ").append(literal);
                    continue;
                }
                if (arg instanceof String || arg instanceof java.util.Date || arg instanceof java.time.temporal.Temporal) {
                    sb.append("'").append(arg.toString().replace("'", "''")).append("'");
                    continue;
                }
                if (arg instanceof Boolean) {
                    sb.append(Boolean.TRUE.equals(arg) ? "TRUE" : "FALSE");
                    continue;
                }
                sb.append(arg.toString());
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private enum SqlScanState { SQL, SINGLE_QUOTE, DOUBLE_QUOTE, LINE_COMMENT, BLOCK_COMMENT }

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
