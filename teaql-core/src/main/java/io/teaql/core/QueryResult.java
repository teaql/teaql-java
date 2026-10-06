package io.teaql.core;

public interface QueryResult {
    /** Actual physical statements in execution order; trusted diagnostics, not a wire response. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    default java.util.List<ExecutionMetadata> statements() {
        return java.util.List.of();
    }
}
