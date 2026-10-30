package io.teaql.core.blob;

/** Immutable stable catalog key. Possession conveys no read or binding permission. */
public record BlobRef(String key) {
    public BlobRef {
        if (key == null || !key.matches("blob_[A-Za-z0-9_-]{1,123}")) {
            throw new IllegalArgumentException("BLOB_REFERENCE_INVALID");
        }
    }

    public static BlobRef of(String key) { return new BlobRef(key); }

    @Override public String toString() { return "BlobRef[masked]"; }
}
