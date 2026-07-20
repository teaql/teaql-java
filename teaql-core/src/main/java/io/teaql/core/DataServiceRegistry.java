package io.teaql.core;

import java.util.Optional;

public interface DataServiceRegistry {
    DataServiceExecutor resolve(String name);

    QueryExecutor resolveQueryExecutor(String name);

    MutationExecutor resolveMutationExecutor(String name);

    Optional<TransactionExecutor> resolveTransactionExecutor(String name);
}
