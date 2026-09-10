package io.teaql.core;

import java.util.Optional;

public interface DataServiceRegistry {
    DataServiceExecutor resolve(String name);

    QueryExecutor resolveQueryExecutor(String name);

    MutationExecutor resolveMutationExecutor(String name);

    Optional<TransactionExecutor> resolveTransactionExecutor(String name);
    
    // Type-safe route overloads
    default DataServiceExecutor resolve(DataServiceRoute route) {
        return resolve(route.name());
    }
    
    default QueryExecutor resolveQueryExecutor(DataServiceRoute route) {
        return resolveQueryExecutor(route.name());
    }
    
    default MutationExecutor resolveMutationExecutor(DataServiceRoute route) {
        return resolveMutationExecutor(route.name());
    }
    
    default Optional<TransactionExecutor> resolveTransactionExecutor(DataServiceRoute route) {
        return resolveTransactionExecutor(route.name());
    }
}
