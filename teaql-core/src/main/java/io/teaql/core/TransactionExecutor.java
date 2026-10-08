package io.teaql.core;

public interface TransactionExecutor extends DataServiceExecutor {
    @FrameworkInternal("Atomic graph participant identity; not a workspace API")
    default Object transactionResource() { return null; }
    <T> T executeInTransaction(UserContext context, TransactionCallback<T> action);
}
