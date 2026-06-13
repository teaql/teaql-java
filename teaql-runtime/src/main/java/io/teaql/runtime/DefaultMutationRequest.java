package io.teaql.runtime;

import io.teaql.core.Entity;
import io.teaql.core.MutationRequest;

public class DefaultMutationRequest implements MutationRequest {
    public enum Action { SAVE, DELETE }

    private final Entity entity;
    private final Action action;

    public DefaultMutationRequest(Entity entity, Action action) {
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
