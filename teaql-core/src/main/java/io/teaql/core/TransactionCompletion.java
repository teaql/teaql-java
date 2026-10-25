package io.teaql.core;

/** Notification from the owner of a physical transaction, after resource cleanup. */
@FunctionalInterface
public interface TransactionCompletion {
    enum Outcome { COMMITTED, ROLLED_BACK, UNKNOWN }

    /**
     * Once per registration. Commit callbacks run in registration order; rollback
     * and unknown callbacks run in reverse order. UNKNOWN must never authorize
     * committed delivery or an automatic retry of business writes.
     */
    void afterCompletion(Outcome outcome);
}
