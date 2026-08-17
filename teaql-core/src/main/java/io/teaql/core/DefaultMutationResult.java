package io.teaql.core;

public final class DefaultMutationResult implements MutationResult {
    private final Entity persistedEntity;

    public DefaultMutationResult(Entity persistedEntity) {
        this.persistedEntity = persistedEntity;
    }

    @Override
    public Entity persistedEntity() {
        return persistedEntity;
    }
}
