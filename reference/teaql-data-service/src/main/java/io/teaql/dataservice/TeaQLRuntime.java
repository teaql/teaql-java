package io.teaql.coreservice;

import io.teaql.core.RequestPolicy;
import io.teaql.core.log.LogManager;
import io.teaql.core.meta.EntityMetaFactory;

import java.util.HashMap;
import java.util.Map;

public class TeaQLRuntime implements DataServiceRegistry {
    private EntityMetaFactory metadata;
    private RequestPolicy requestPolicy;
    private LogManager logManager;
    private Map<String, DataServiceExecutor> dataServices = new HashMap<>();

    private TeaQLRuntime() {}

    public static Builder builder() {
        return new Builder();
    }

    public EntityMetaFactory getMetadata() { return metadata; }
    public RequestPolicy getRequestPolicy() { return requestPolicy; }
    public LogManager getLogManager() { return logManager; }
    public Map<String, DataServiceExecutor> getDataServices() { return dataServices; }

    @Override
    public DataServiceExecutor resolve(String name) {
        DataServiceExecutor executor = dataServices.get(name);
        if (executor == null) {
            throw new IllegalArgumentException("No DataServiceExecutor registered for: " + name);
        }
        return executor;
    }

    @Override
    public QueryExecutor resolveQueryExecutor(String name) {
        DataServiceExecutor executor = resolve(name);
        if (!(executor instanceof QueryExecutor)) {
            throw new IllegalArgumentException("DataServiceExecutor for " + name + " is not a QueryExecutor");
        }
        return (QueryExecutor) executor;
    }

    @Override
    public MutationExecutor resolveMutationExecutor(String name) {
        DataServiceExecutor executor = resolve(name);
        if (!(executor instanceof MutationExecutor)) {
            throw new IllegalArgumentException("DataServiceExecutor for " + name + " is not a MutationExecutor");
        }
        return (MutationExecutor) executor;
    }

    @Override
    public java.util.Optional<TransactionExecutor> resolveTransactionExecutor(String name) {
        DataServiceExecutor executor = resolve(name);
        if (executor instanceof TransactionExecutor) {
            return java.util.Optional.of((TransactionExecutor) executor);
        }
        return java.util.Optional.empty();
    }

    public static class Builder {
        private TeaQLRuntime runtime = new TeaQLRuntime();

        public Builder metadata(EntityMetaFactory metadata) {
            runtime.metadata = metadata;
            return this;
        }

        public Builder requestPolicy(RequestPolicy requestPolicy) {
            runtime.requestPolicy = requestPolicy;
            return this;
        }

        public Builder logManager(LogManager logManager) {
            runtime.logManager = logManager;
            return this;
        }

        public Builder dataService(String name, DataServiceExecutor executor) {
            runtime.dataServices.put(name, executor);
            return this;
        }

        public TeaQLRuntime build() {
            return runtime;
        }
    }
}
