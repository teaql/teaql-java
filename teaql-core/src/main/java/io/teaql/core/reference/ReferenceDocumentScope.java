package io.teaql.core.reference;

import java.nio.charset.StandardCharsets;

/** Purpose and Aggregate boundary for one UI/API document round trip. */
public record ReferenceDocumentScope(
        String documentId,
        String purpose,
        String aggregateType,
        long aggregateId,
        long aggregateRevision) {
    public ReferenceDocumentScope {
        requireText(documentId, "documentId");
        requireText(purpose, "purpose");
        requireText(aggregateType, "aggregateType");
        if (aggregateId <= 0) throw new IllegalArgumentException("aggregateId must be positive");
        if (aggregateRevision < 0) {
            throw new IllegalArgumentException("aggregateRevision must not be negative");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.getBytes(StandardCharsets.UTF_8).length > 4096) {
            throw new IllegalArgumentException(name + " must be non-blank and trimmed");
        }
    }
}
