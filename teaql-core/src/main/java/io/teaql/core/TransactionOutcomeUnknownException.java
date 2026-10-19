package io.teaql.core;

/** A provider could not confirm whether a transaction committed; reload before retrying. */
public final class TransactionOutcomeUnknownException extends TeaQLRuntimeException {
    public TransactionOutcomeUnknownException() {
        super("TRANSACTION_OUTCOME_UNKNOWN: the provider could not confirm the transaction result; "
                + "reload the affected graph and committed events before any retry");
    }
}
