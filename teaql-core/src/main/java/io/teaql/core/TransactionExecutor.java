package io.teaql.core;

import java.util.function.Supplier;

public interface TransactionExecutor extends DataServiceExecutor {
    @FrameworkInternal("Physical transaction ownership; not a workspace API")
    default boolean hasActiveTransaction() { return false; }
    @FrameworkInternal("Atomic graph participant identity; not a workspace API")
    default Object transactionResource() { return null; }
    /** Returns true only when callbacks belong to the currently active physical transaction. */
    @FrameworkInternal("Runtime commit audit and rollback intent; not a workspace API")
    default boolean registerTransactionSynchronization(
            UserContext context, Runnable committed, Runnable rolledBack) { return false; }
    @FrameworkInternal("Unknown physical outcome fences the affected mutation graphs")
    default boolean registerTransactionSynchronization(
            UserContext context, Runnable committed, Runnable rolledBack, Runnable unknown) {
        return registerTransactionSynchronization(context, committed, rolledBack);
    }
    /** True only when the next transaction invocation owns physical completion. */
    @FrameworkInternal("Select reconciliation timing without changing the workspace transaction API")
    default boolean ownsNextPhysicalTransaction(UserContext context) { return false; }
    @FrameworkInternal("Keep operations without the bound UserContext out of its physical transaction")
    default <T> T withoutActiveTransaction(Supplier<T> work) {
        throw new UnsupportedOperationException("The data service cannot suspend its active transaction");
    }
    <T> T executeInTransaction(UserContext context, TransactionCallback<T> action);
}
