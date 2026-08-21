package io.teaql.core;

/** Trusted typed entity reference used for context-bound ownership fixes. */
public record ContextEntityRef(String entityType, Long id) {
    public ContextEntityRef {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType must not be blank");
        }
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
    }
}
