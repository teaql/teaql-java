package io.teaql.runtime;

import io.teaql.core.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public class DefaultDataServiceRegistry implements DataServiceRegistry {
    private final Map<String, DataServiceExecutor> executors = new ConcurrentHashMap<>();

    public void register(String name, DataServiceExecutor executor) {
        executors.put(name, executor);
    }

    @Override
    public DataServiceExecutor resolve(String name) {
        return executors.get(name);
    }

    @Override
    public QueryExecutor resolveQueryExecutor(String name) {
        DataServiceExecutor executor = executors.get(name);
        return executor instanceof QueryExecutor ? (QueryExecutor) executor : null;
    }

    @Override
    public MutationExecutor resolveMutationExecutor(String name) {
        DataServiceExecutor executor = executors.get(name);
        return executor instanceof MutationExecutor ? (MutationExecutor) executor : null;
    }

    @Override
    public Optional<TransactionExecutor> resolveTransactionExecutor(String name) {
        DataServiceExecutor executor = executors.get(name);
        if (executor instanceof TransactionExecutor) {
            return Optional.of((TransactionExecutor) executor);
        }
        return Optional.empty();
    }
}
