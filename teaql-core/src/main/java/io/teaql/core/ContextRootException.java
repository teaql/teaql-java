package io.teaql.core;

/** Structured failure for a missing or mismatched trusted active root. */
public class ContextRootException extends TeaQLRuntimeException {
    public enum Reason { MISSING, TYPE_MISMATCH, VALUE_MISMATCH }

    private final Reason reason;
    private final String expectedType;
    private final ContextEntityRef activeRoot;

    public ContextRootException(Reason reason, String expectedType, ContextEntityRef activeRoot) {
        super("Context root " + reason.name().toLowerCase() + ": expected " + expectedType
                + (activeRoot == null ? "" : ", active " + activeRoot.entityType() + "(" + activeRoot.id() + ")"));
        this.reason = reason;
        this.expectedType = expectedType;
        this.activeRoot = activeRoot;
    }

    public Reason getReason() { return reason; }
    public String getExpectedType() { return expectedType; }
    public ContextEntityRef getActiveRoot() { return activeRoot; }
}
