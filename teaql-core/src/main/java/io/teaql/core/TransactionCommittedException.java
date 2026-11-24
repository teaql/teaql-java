package io.teaql.core;

/** Database commit was confirmed before a completion action failed. */
public final class TransactionCommittedException extends TeaQLRuntimeException {
    public TransactionCommittedException(Throwable cause) {
        super("TRANSACTION_ALREADY_COMMITTED: a completion action failed", cause);
    }
}
