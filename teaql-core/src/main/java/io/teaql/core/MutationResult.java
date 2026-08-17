package io.teaql.core;

public interface MutationResult {
    default Entity persistedEntity() {
        return null;
    }
}
