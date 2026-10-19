package io.teaql.runtime;

import io.teaql.core.TeaQLRuntimeException;

/** The graph committed, but one or more post-commit audit deliveries failed. */
public final class AuditDeliveryAfterCommitException extends TeaQLRuntimeException {
    private final int failureCount;

    public AuditDeliveryAfterCommitException(int failureCount) {
        super("AUDIT_DELIVERY_AFTER_COMMIT: transaction committed but audit delivery failed; "
                + "do not retry the mutation blindly");
        if (failureCount < 1) throw new IllegalArgumentException("failureCount must be positive");
        this.failureCount = failureCount;
    }

    public int failureCount() {
        return failureCount;
    }
}
