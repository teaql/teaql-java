package io.teaql.core;

public interface TransactionExecutor extends DataServiceExecutor {
    <T> T executeInTransaction(UserContext context, TransactionCallback<T> action);
}
