package io.teaql.core.reference;

import java.nio.charset.StandardCharsets;

/** Runtime identity carried inside a protected round-trip reference. */
public record InternalEntityIdentity(String entityType, long id, long version) {
    public InternalEntityIdentity {
        if (entityType == null || entityType.isBlank() || !entityType.equals(entityType.trim())
                || entityType.getBytes(StandardCharsets.UTF_8).length > 4096) {
            throw new IllegalArgumentException("entityType must be non-blank, trimmed, and bounded");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
