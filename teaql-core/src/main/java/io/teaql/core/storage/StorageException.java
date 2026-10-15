package io.teaql.core.storage;

/** Sanitized storage error. Provider payloads/credentials are deliberately not retained as causes. */
public final class StorageException extends RuntimeException {
    public enum Code { INVALID_INPUT, NOT_CONFIGURED, NOT_FOUND, ACCESS_DENIED, CONFLICT,
        TIMEOUT, UNAVAILABLE, UNSUPPORTED, TOO_LARGE, IO }
    private final Code code;
    public StorageException(Code code) { super("Object storage: " + code); this.code = code; }
    public Code code() { return code; }
}
