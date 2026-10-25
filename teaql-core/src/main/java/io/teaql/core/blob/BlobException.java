package io.teaql.core.blob;

/** Stable, sanitized failures. Messages contain no storage address or credential. */
public final class BlobException extends RuntimeException {
    public enum Code {
        BLOB_CAPABILITY_MISSING, BLOB_SCHEMA_MISSING, BLOB_POLICY_INVALID,
        BLOB_REFERENCE_INVALID, BLOB_FORMAT_NOT_ALLOWED, BLOB_SIZE_EXCEEDED,
        BLOB_UPLOAD_EXPIRED, BLOB_REFERENCE_EXPIRED, BLOB_UPLOAD_INCOMPLETE,
        BLOB_BIND_DENIED, BLOB_READ_DENIED, BLOB_NOT_FOUND, BLOB_TRANSACTION_REQUIRED,
        BLOB_SIGN_FAILED, BLOB_IDEMPOTENCY_CONFLICT, BLOB_SELECTION_INVALID,
        BLOB_SELECTION_SCOPE_MISMATCH, BLOB_SELECTION_EXPIRED
    }
    private final Code code;
    public BlobException(Code code) { super(java.util.Objects.requireNonNull(code).name()); this.code = code; }
    public Code code() { return code; }
}
