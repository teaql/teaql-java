package io.teaql.core;

public interface MutationResult {
    /** Actual physical statements, in execution order. Trusted diagnostics, not a wire response. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    default java.util.List<ExecutionMetadata> statements() {
        return java.util.List.of();
    }

    default Entity persistedEntity() {
        return null;
    }
}
