package io.teaql.core;

public interface TransactionExecutor extends DataServiceExecutor {
    @FrameworkInternal("Atomic graph participant identity; not a workspace API")
    default Object transactionResource() { return null; }
    default boolean supportsTransactionCompletion() { return false; }
    @FrameworkInternal("Register only inside the physical business transaction; never infer completion from nested return")
    default void afterTransaction(TransactionCompletion completion) {
        throw new UnsupportedOperationException("Physical transaction completion is not supported");
    }
    <T> T executeInTransaction(UserContext context, TransactionCallback<T> action);
}
