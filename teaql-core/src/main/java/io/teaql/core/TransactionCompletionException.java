package io.teaql.core;

import java.util.Objects;

/** A physical outcome survives commit acknowledgement, cleanup or notification failure. */
public final class TransactionCompletionException extends TeaQLRuntimeException {
    private final TransactionCompletion.Outcome outcome;

    public TransactionCompletionException(TransactionCompletion.Outcome outcome,Throwable cause) {
        super("Transaction outcome: "+Objects.requireNonNull(outcome),cause);
        this.outcome=outcome;
    }

    public TransactionCompletion.Outcome outcome() { return outcome; }
}
