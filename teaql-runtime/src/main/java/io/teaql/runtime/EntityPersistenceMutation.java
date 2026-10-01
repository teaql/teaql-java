package io.teaql.runtime;

import io.teaql.core.Entity;
import io.teaql.core.PersistenceMutation;
import io.teaql.core.MutationIntent;
import java.util.Objects;

public final class EntityPersistenceMutation implements PersistenceMutation {
    public enum Action { SAVE, DELETE }

    private final Entity entity;
    private final Action action;
    private final MutationIntent intent;

    public EntityPersistenceMutation(Entity entity, Action action) {
        this(entity, action, MutationIntent.of(entity.getComment()));
    }

    public EntityPersistenceMutation(Entity entity, Action action, MutationIntent intent) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.action = Objects.requireNonNull(action, "action");
        this.intent = Objects.requireNonNull(intent, "intent");
    }

    @Override public MutationIntent intent() { return intent; }

    public Entity getEntity() {
        return entity;
    }

    public Action getAction() {
        return action;
    }
}
