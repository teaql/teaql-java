package io.teaql.core;

public interface TransactionExecutor extends DataServiceExecutor {
    @FrameworkInternal("Atomic graph participant identity; not a workspace API")
    default Object transactionResource() { return null; }
    /** Returns true only when callbacks belong to the currently active physical transaction. */
    @FrameworkInternal("Runtime commit audit and rollback intent; not a workspace API")
    default boolean registerTransactionSynchronization(
            UserContext context, Runnable committed, Runnable rolledBack) { return false; }
    <T> T executeInTransaction(UserContext context, TransactionCallback<T> action);
}
