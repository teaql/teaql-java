package io.teaql.core;

/** A persistence lifecycle error, distinct from a concurrent database change. */
public final class MutationVersionException extends TeaQLRuntimeException {
    public enum Code { INVALID_VERSION_STATE, VERSION_EXHAUSTED }

    private final Code code;
    private final MutationOperationKind operation;
    private final Long originalVersion;

    MutationVersionException(Code code, MutationOperationKind operation, Long originalVersion) {
        super("Cannot " + operation.name().toLowerCase(java.util.Locale.ROOT) + ": "
                + (code == Code.VERSION_EXHAUSTED ? "optimistic version is exhausted"
                : "optimistic version has an invalid lifecycle state"));
        this.code = code;
        this.operation = operation;
        this.originalVersion = originalVersion;
    }

    public Code code() { return code; }
    public MutationOperationKind operation() { return operation; }
    public Long originalVersion() { return originalVersion; }
}
