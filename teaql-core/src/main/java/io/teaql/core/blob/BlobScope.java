package io.teaql.core.blob;

import java.nio.charset.StandardCharsets;

/** Trusted host isolation boundary, independent of document keys and document mode. */
public record BlobScope(String realm, String boundary) {
    public BlobScope { validate(realm); validate(boundary); }
    private static void validate(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.indexOf('\0') >= 0
                || value.getBytes(StandardCharsets.UTF_8).length > 256) {
            throw new BlobException(BlobException.Code.BLOB_BIND_DENIED);
        }
    }
    @Override public String toString() { return "BlobScope[redacted]"; }
}
