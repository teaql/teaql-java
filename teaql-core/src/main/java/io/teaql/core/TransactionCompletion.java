package io.teaql.core;

/** Notification from the owner of a physical transaction, after resource cleanup. */
@FunctionalInterface
public interface TransactionCompletion {
    enum Outcome { COMMITTED, ROLLED_BACK, UNKNOWN }

    /** State only: resolve retained framework state without business I/O or delivery reentry. */
    @FrameworkInternal("Physical owner must run this phase for all registrations before afterCompletion")
    default void beforeNotifications(Outcome outcome) {}

    /**
     * Once per registration. Commit callbacks run in registration order; rollback
     * and unknown callbacks run in reverse order. UNKNOWN must never authorize
     * committed delivery or an automatic retry of business writes.
     */
    void afterCompletion(Outcome outcome);
}
