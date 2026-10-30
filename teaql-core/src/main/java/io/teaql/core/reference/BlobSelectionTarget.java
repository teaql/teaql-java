package io.teaql.core.reference;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Exact document, field, row/client key and command to which a file choice is bound. */
public record BlobSelectionTarget(
        ReferenceDocumentScope scope, String targetEntity, String canonicalField,
        Kind kind, String targetKey, String commandId) {
    public enum Kind { EXISTING_ROW, NEW_CLIENT_KEY }

    public BlobSelectionTarget {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(kind, "kind");
        text(targetEntity);
        text(canonicalField);
        text(targetKey);
        text(commandId);
    }

    @Override public String toString() { return "BlobSelectionTarget[redacted]"; }

    private static void text(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.getBytes(StandardCharsets.UTF_8).length > 16384) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.DOCUMENT_VALIDATION_FAILED,
                    "DOCUMENT_VALIDATION_FAILED");
        }
    }
}
