package io.teaql.coreservice.sql;

import io.teaql.core.UserContext;
import io.teaql.coreservice.DataServiceCapabilities;
import io.teaql.coreservice.MutationExecutor;
import io.teaql.coreservice.MutationRequest;
import io.teaql.coreservice.MutationResult;
import io.teaql.coreservice.QueryExecutor;
import io.teaql.coreservice.QueryRequest;
import io.teaql.coreservice.QueryResult;
import io.teaql.coreservice.SchemaExecutor;
import io.teaql.coreservice.TransactionCallback;
import io.teaql.coreservice.TransactionExecutor;

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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public MutationResult mutate(UserContext ctx, MutationRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
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
}
