package io.teaql.runtime;

import io.teaql.core.Entity;
import io.teaql.core.PersistenceMutation;

public class EntityPersistenceMutation implements PersistenceMutation {
    public enum Action { SAVE, DELETE }

    private final Entity entity;
    private final Action action;

    public EntityPersistenceMutation(Entity entity, Action action) {
        this.entity = entity;
        this.action = action;
    }

    public Entity getEntity() {
        return entity;
    }

    public Action getAction() {
        return action;
    }
}
