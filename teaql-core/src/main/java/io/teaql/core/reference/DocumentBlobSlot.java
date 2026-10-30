package io.teaql.core.reference;

import java.nio.charset.StandardCharsets;

/** Outbound-only position of a Blob field in an application-issued document. */
public record DocumentBlobSlot(String rowKey, String canonicalField) {
    public DocumentBlobSlot {
        text(rowKey);
        text(canonicalField);
    }

    @Override public String toString() { return "DocumentBlobSlot[redacted]"; }

    private static void text(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.getBytes(StandardCharsets.UTF_8).length > 16384) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                    "DOCUMENT_PROJECTION_VIOLATION");
        }
    }
}
