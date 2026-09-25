package io.teaql.core.reference;

/** Runtime identity carried inside a protected round-trip reference. */
public record InternalEntityIdentity(String entityType, long id, long version) {
    public InternalEntityIdentity {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType must not be blank");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("version must be positive");
        }
    }
}
