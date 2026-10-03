package io.teaql.core;

public final class DefaultMutationResult implements MutationResult {
    private final Entity persistedEntity;
    private final java.util.List<ExecutionMetadata> statements;

    public DefaultMutationResult(Entity persistedEntity) {
        this(persistedEntity, java.util.List.of());
    }

    public DefaultMutationResult(Entity persistedEntity, java.util.List<ExecutionMetadata> statements) {
        this.persistedEntity = persistedEntity;
        this.statements = java.util.List.copyOf(statements);
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.util.List<ExecutionMetadata> statements() { return statements; }

    @Override
    public Entity persistedEntity() {
        return persistedEntity;
    }
}
