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

public class SqlDataServiceExecutor implements QueryExecutor, MutationExecutor, TransactionExecutor, SchemaExecutor {
    private final String name;
    private final SqlExecutionAdapter executionAdapter;
    private final DataServiceCapabilities capabilities;

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
    public MutationResult mutate(UserContext ctx, MutationRequest request) {
        return getPortableService().mutate(ctx, request);
    }

    @Override
    public <T> T executeInTransaction(UserContext ctx, TransactionCallback<T> action) {
        return action.doInTransaction();
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
                    action.run();
                }
                @Override
                public java.util.List<java.util.Map<String, Object>> getTableColumns(String tableName) {
                    throw new UnsupportedOperationException("Implement in specific dialect");
                }
            };
            portableService = new io.teaql.core.sql.portable.PortableSQLDataService(name, dbAdapter, io.teaql.core.meta.EntityMetaFactory.get());
        }
        return portableService;
    }
}
