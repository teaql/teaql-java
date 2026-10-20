package io.teaql.core;

/** A provider could not confirm whether a transaction committed; reload before retrying. */
public final class TransactionOutcomeUnknownException extends TeaQLRuntimeException {
    private static final String MESSAGE = "TRANSACTION_OUTCOME_UNKNOWN: the provider could not confirm the transaction result; "
            + "reload the affected graph and committed events before any retry";
    private final Error originalError;

    public TransactionOutcomeUnknownException() {
        this(null);
    }

    /** Retain a business Error without exposing it as a diagnostic cause. */
    public TransactionOutcomeUnknownException(Error originalError) {
        super(MESSAGE);
        this.originalError = originalError;
    }

    /** The graph runtime rethrows this Error only after fencing the mutation ledger. */
    public Error originalError() {
        return originalError;
    }
}
