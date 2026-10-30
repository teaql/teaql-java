package io.teaql.core.reference;

import io.teaql.core.blob.BlobRef;
import java.nio.charset.StandardCharsets;

/** One authenticated Blob binding in a tqd2 document. The stable Key stays encrypted. */
public record DocumentBlobBindingSnapshot(
        String rowKey, String canonicalField, String entityType, BlobRef reference,
        long bindingVersion, boolean nullable, boolean writable) {
    public DocumentBlobBindingSnapshot {
        text(rowKey);
        text(canonicalField);
        text(entityType);
    }

    /** bindingVersion carries the unsigned 64-bit wire value as raw long bits. */
    @Override public String toString() { return "DocumentBlobBindingSnapshot[redacted]"; }

    private static void text(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.getBytes(StandardCharsets.UTF_8).length > 16384) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                    "DOCUMENT_PROJECTION_VIOLATION");
        }
    }
}
