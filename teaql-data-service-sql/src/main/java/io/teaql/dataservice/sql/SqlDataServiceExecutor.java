package io.teaql.dataservice.sql;

import io.teaql.core.UserContext;
import io.teaql.core.DataServiceCapabilities;
import io.teaql.core.MutationExecutor;
import io.teaql.core.PersistenceMutation;
import io.teaql.core.MutationResult;
import io.teaql.core.QueryExecutor;
import io.teaql.core.QueryRequest;
import io.teaql.core.QueryResult;
import io.teaql.core.SchemaExecutor;
import io.teaql.core.TransactionCallback;
import io.teaql.core.TransactionExecutor;

public class SqlDataServiceExecutor implements QueryExecutor, io.teaql.core.StreamingQueryExecutor, io.teaql.core.BatchMutationExecutor, TransactionExecutor, SchemaExecutor {
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
        return getPortableService(context).query(context, request);
    }

    @Override
    public <T extends io.teaql.core.Entity> java.util.stream.Stream<T> queryForStream(UserContext context, QueryRequest request) {
        return getPortableService(context).queryForStream(context, request);
    }

    @Override
    public MutationResult mutate(UserContext context, PersistenceMutation request) {
        return getPortableService(context).mutate(context, request);
    }

    @Override
    public java.util.List<MutationResult> mutateBatch(UserContext context, io.teaql.core.MutationBatchRequest request) {
        return getPortableService(context).mutateBatch(context, request);
    }

    @Override
    public <T> T executeInTransaction(UserContext context, TransactionCallback<T> action) {
        return getPortableService(context).executeInTransaction(context, action);
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

    // The common one-runtime-per-executor path is lock-free after first use. The identity map
    // preserves isolation when an application deliberately shares one executor across runtimes.
    private volatile io.teaql.core.meta.EntityMetaFactory primaryMetadata;
    private volatile io.teaql.core.sql.portable.PortableSQLDataService primaryPortableService;
    private final java.util.Map<io.teaql.core.meta.EntityMetaFactory,
            io.teaql.core.sql.portable.PortableSQLDataService> secondaryPortableServices =
            new java.util.IdentityHashMap<>();

    private io.teaql.core.sql.portable.PortableSQLDataService getPortableService(UserContext context) {
        io.teaql.core.meta.EntityMetaFactory metadata =
                io.teaql.core.meta.EntityMetaFactory.requireFrom(context);
        io.teaql.core.sql.portable.PortableSQLDataService primary = primaryPortableService;
        if (primary != null && primaryMetadata == metadata) return primary;
        synchronized (this) {
            primary = primaryPortableService;
            if (primary != null && primaryMetadata == metadata) return primary;
            if (primary == null) {
                primary = createPortableService(metadata);
                primaryMetadata = metadata;
                primaryPortableService = primary;
                return primary;
            }
            return secondaryPortableServices.computeIfAbsent(
                    metadata, this::createPortableService);
        }
    }

    private io.teaql.core.sql.portable.PortableSQLDataService createPortableService(
            io.teaql.core.meta.EntityMetaFactory metadata) {
        io.teaql.core.sql.portable.TeaQLDatabase dbAdapter =
                new io.teaql.core.sql.portable.TeaQLDatabase() {
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
                    return queryForStream(context, sql, args, io.teaql.core.sql.portable.SqlLogBindings.UNKNOWN);
                }
                @Override
                public java.util.stream.Stream<java.util.Map<String, Object>> queryForStream(io.teaql.core.UserContext context,
                        String sql, Object[] args, io.teaql.core.sql.portable.SqlLogBindings bindings) {
                    boolean logging = context.isQueryExecutionLoggingEnabled();
                    long start = logging ? System.nanoTime() : 0L;
                    io.teaql.core.ExecutionMetadata meta = null;
                    if (logging) {
                        meta = statementMetadata(context, sql, args, bindings, io.teaql.core.DataServiceOperation.QUERY);
                        // Snapshot before request trace scopes are popped; lazy consumption may happen later.
                        if (bindings.executionTrace() == null) {
                            var trace = context.getTraceChain();
                            meta.setTraceChain(trace == null || trace.isEmpty()
                                    ? java.util.List.of(new io.teaql.core.TraceNode(io.teaql.core.TraceKind.OPERATION, "stream", "query"))
                                    : java.util.List.copyOf(trace));
                        }
                    }
                    var stream = diagnosed(context, sql, args, bindings, io.teaql.core.DataServiceOperation.QUERY,
                            logging, start, () -> executionAdapter.queryForStream(sql, args));
                    return SqlDiagnosticStream.wrap(context, stream, meta, start);
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
                    return query(context, sql, args, io.teaql.core.sql.portable.SqlLogBindings.UNKNOWN);
                }

                @Override
                public java.util.List<java.util.Map<String, Object>> query(io.teaql.core.UserContext context, String sql, Object[] args,
                        io.teaql.core.sql.portable.SqlLogBindings bindings) {
                    boolean logging = context.isQueryExecutionLoggingEnabled();
                    boolean collecting = logging || bindings.collectsStatements();
                    long start = collecting ? System.nanoTime() : 0L;
                    java.util.List<java.util.Map<String, Object>> res = diagnosed(context, sql, args, bindings,
                            io.teaql.core.DataServiceOperation.QUERY, logging, start,
                            () -> executionAdapter.queryForList(sql, args));
                    if (!collecting) return res;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.QUERY);
                    meta.setElapsedUs(elapsed);
                    meta.setResultCount(res.size());
                    meta.setResultSummary("Fetched " + res.size() + " rows");
                    meta.setExecutionOutcome("success");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(parameters(args));
                    bindings.applyTo(meta);
                    recordStatement(context, bindings, meta, logging);
                    return res;
                }

                @Override
                public <T extends io.teaql.core.Entity> java.util.List<T> query(
                        io.teaql.core.UserContext context, String sql, Object[] args,
                        io.teaql.core.CompiledRowMapper<T> rowMapper) {
                    return query(context, sql, args, rowMapper, io.teaql.core.sql.portable.SqlLogBindings.UNKNOWN);
                }

                @Override
                public <T extends io.teaql.core.Entity> java.util.List<T> query(
                        io.teaql.core.UserContext context, String sql, Object[] args,
                        io.teaql.core.CompiledRowMapper<T> rowMapper, io.teaql.core.sql.portable.SqlLogBindings bindings) {
                    boolean logging = context.isQueryExecutionLoggingEnabled();
                    long start = logging ? System.nanoTime() : 0L;
                    java.util.List<T> res = diagnosed(context, sql, args, bindings,
                            io.teaql.core.DataServiceOperation.QUERY, logging, start,
                            () -> executionAdapter.query(sql, args, rowMapper));
                    if (!logging) return res;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.QUERY);
                    meta.setElapsedUs(elapsed);
                    meta.setResultCount(res.size());
                    meta.setResultSummary("Fetched " + res.size() + " typed rows");
                    meta.setExecutionOutcome("success");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(parameters(args));
                    bindings.applyTo(meta);
                    context.recordExecutionMetadata(meta);
                    return res;
                }

                @Override
                public int executeUpdate(io.teaql.core.UserContext context, String sql, Object[] args) {
                    return executeUpdate(context, sql, args, io.teaql.core.sql.portable.SqlLogBindings.UNKNOWN);
                }

                @Override
                public int executeUpdate(io.teaql.core.UserContext context, String sql, Object[] args,
                        io.teaql.core.sql.portable.SqlLogBindings bindings) {
                    boolean logging = context.isMutationExecutionLoggingEnabled();
                    boolean collecting = logging || bindings.collectsStatements();
                    long start = collecting ? System.nanoTime() : 0L;
                    int res = diagnosed(context, sql, args, bindings,
                            io.teaql.core.DataServiceOperation.MUTATION, logging, start,
                            () -> executionAdapter.update(sql, args));
                    if (!collecting) return res;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.MUTATION);
                    meta.setElapsedUs(elapsed);
                    meta.setAffectedRows((long) res);
                    meta.setResultSummary("Affected " + res + " rows");
                    meta.setExecutionOutcome("success");
                    meta.setParameterizedQuery(sql);
                    meta.setParameters(parameters(args));
                    bindings.applyTo(meta);
                    recordStatement(context, bindings, meta, logging);
                    return res;
                }

                @Override
                public int[] batchUpdate(io.teaql.core.UserContext context, String sql, java.util.List<Object[]> batchArgs) {
                    return batchUpdate(context, sql, batchArgs, io.teaql.core.sql.portable.SqlLogBindings.UNKNOWN);
                }

                @Override
                public int[] batchUpdate(io.teaql.core.UserContext context, String sql, java.util.List<Object[]> batchArgs,
                        io.teaql.core.sql.portable.SqlLogBindings bindings) {
                    bindings.validateBatchSize(batchArgs == null ? 0 : batchArgs.size());
                    boolean logging = context.isMutationExecutionLoggingEnabled();
                    boolean collecting = logging || bindings.collectsStatements();
                    long start = collecting ? System.nanoTime() : 0L;
                    int[] res;
                    try {
                        res = executionAdapter.batchUpdate(sql, batchArgs);
                    } catch (RuntimeException failure) {
                        if (collecting) recordBatch(context, sql, batchArgs, bindings, start, batchCounts(failure), failure, logging);
                        throw failure;
                    }
                    if (collecting) recordBatch(context, sql, batchArgs, bindings, start,
                            res == null ? null : java.util.Arrays.stream(res).asLongStream().toArray(), null, logging);
                    return res;
                }

                @Override
                public void execute(io.teaql.core.UserContext context, String sql) {
                    boolean logging = context.isMutationExecutionLoggingEnabled();
                    long start = logging ? System.nanoTime() : 0L;
                    diagnosed(context, sql, null, io.teaql.core.sql.portable.SqlLogBindings.UNKNOWN,
                            io.teaql.core.DataServiceOperation.SCHEMA, logging, start,
                            () -> { executionAdapter.execute(sql); return null; });
                    if (!logging) return;
                    long elapsed = (System.nanoTime() - start) / 1000;
                    io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                    meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                    meta.setOperation(io.teaql.core.DataServiceOperation.SCHEMA);
                    meta.setElapsedUs(elapsed);
                    meta.setResultSummary("Executed");
                    meta.setExecutionOutcome("success");
                    meta.setParameterizedQuery(sql);
                    if (context.requiresSensitiveSqlLogData()) {
                        meta.setDebugQuery(sql);
                    }
                    context.recordExecutionMetadata(meta);
                }
            };
        io.teaql.core.sql.portable.PortableSQLDataService portableService =
                new io.teaql.core.sql.portable.PortableSQLDataService(name, dbAdapter, metadata);
        portableService.setDialect(this.dialect);
        portableService.setTopNRelationPlanPolicy(this.topNRelationPlanPolicy);
        return portableService;
    }

    private io.teaql.core.ExecutionMetadata statementMetadata(UserContext context, String sql, Object[] args,
            io.teaql.core.sql.portable.SqlLogBindings bindings, io.teaql.core.DataServiceOperation operation) {
        var meta = new io.teaql.core.ExecutionMetadata();
        meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
        meta.setOperation(operation);
        meta.setParameterizedQuery(sql);
        meta.setParameters(parameters(args));
        bindings.applyTo(meta);
        return meta;
    }

    private static long[] batchCounts(Throwable failure) {
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        while (failure != null && seen.add(failure)) {
            if (failure instanceof java.sql.BatchUpdateException batchFailure) return batchFailure.getLargeUpdateCounts();
            failure = failure.getCause();
        }
        return null;
    }

    private void recordBatch(UserContext context, String sql, java.util.List<Object[]> batchArgs,
            io.teaql.core.sql.portable.SqlLogBindings bindings, long start, long[] counts, RuntimeException failure,
            boolean logging) {
        long elapsed = (System.nanoTime() - start) / 1000;
        int size = batchArgs == null ? 0 : batchArgs.size();
        for (int row = 0; row < Math.max(size, failure == null ? 0 : 1); row++) {
            var meta = statementMetadata(context, sql, row < size ? batchArgs.get(row) : null,
                    row < size ? bindings.forBatchRow(row) : bindings, io.teaql.core.DataServiceOperation.MUTATION);
            meta.setElapsedUs(row == 0 ? elapsed : 0);
            meta.setBatchOutcome(failure == null ? "success"
                    : failure instanceof java.util.concurrent.CancellationException ? "cancelled" : "failure");
            long count = counts != null && row < counts.length ? counts[row] : Long.MIN_VALUE;
            meta.setExecutionOutcome(count >= 0 || count == java.sql.Statement.SUCCESS_NO_INFO ? "success"
                    : count == java.sql.Statement.EXECUTE_FAILED ? "failure" : "unknown");
            if (count >= 0) meta.setAffectedRows(count);
            meta.setResultSummary("Batch " + (failure == null ? "returned" : failure instanceof java.util.concurrent.CancellationException
                    ? "cancelled" : "failure") + "; member " + (row + 1) + " of " + size
                    + "; statement outcome " + meta.getExecutionOutcome());
            try {
                recordStatement(context, row < size ? bindings.forBatchRow(row) : bindings, meta, logging);
            } catch (RuntimeException sinkFailure) {
                if (failure == null) throw sinkFailure;
            }
        }
    }

    private <T> T diagnosed(UserContext context, String sql, Object[] args,
            io.teaql.core.sql.portable.SqlLogBindings bindings,
            io.teaql.core.DataServiceOperation operation, boolean logging, long start,
            java.util.function.Supplier<T> execute) {
        try {
            return execute.get();
        } catch (RuntimeException failure) {
            if (logging || bindings.collectsStatements()) {
                io.teaql.core.ExecutionMetadata meta = new io.teaql.core.ExecutionMetadata();
                meta.setBackend(debugDatabaseKind.toLowerCase(java.util.Locale.ROOT));
                meta.setOperation(operation);
                meta.setElapsedUs((System.nanoTime() - start) / 1000);
                meta.setExecutionOutcome(failure instanceof java.util.concurrent.CancellationException
                        ? "cancelled" : "failure");
                // Driver errors may contain raw bind values. Keep the error for the caller,
                // never copy its message/cause into diagnostics or invent affected-row counts.
                meta.setResultSummary("Statement did not complete; row count unknown");
                meta.setParameterizedQuery(sql);
                meta.setParameters(parameters(args));
                bindings.applyTo(meta);
                try {
                    recordStatement(context, bindings, meta, logging);
                } catch (RuntimeException diagnosticFailure) {
                    // Preserve the original driver error; no unsafe fallback logger.
                }
            }
            throw failure;
        }
    }

    private static void recordStatement(UserContext context,
            io.teaql.core.sql.portable.SqlLogBindings bindings, io.teaql.core.ExecutionMetadata metadata,
            boolean logging) {
        bindings.recordStatement(metadata);
        if (logging) context.recordExecutionMetadata(metadata);
    }

    public static String debugSql(String sql, Object[] args) {
        return debugSql(sql, args, "sqlite");
    }

    public static String debugSql(
            String sql, Object[] args, io.teaql.core.sql.dialect.SqlDialect dialect) {
        return debugSql(sql, args, dialect.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT));
    }

    public static String debugSql(String sql, Object[] args, String databaseKind) {
        if (sql == null) return null;
        Object[] parameters = args == null ? new Object[0] : args;
        try {
            return io.teaql.core.utils.SqlLogRenderer.render(sql, parameters.length,
                    index -> io.teaql.core.utils.SqlLogRenderer.literal(parameters[index], databaseKind),
                    databaseKind);
        } catch (IllegalArgumentException failure) {
            return "[SQL OMITTED; NOT REPLAYABLE; unsupported literal or binding mismatch]";
        }
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
