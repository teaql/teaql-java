package io.teaql.runtime;

import io.teaql.core.Entity;
import io.teaql.core.PersistenceMutation;
import io.teaql.core.MutationIntent;
import io.teaql.core.MutationTraceScope;
import io.teaql.core.TraceNode;
import java.util.List;
import java.util.Objects;

public final class EntityPersistenceMutation implements PersistenceMutation {
    public enum Action { SAVE, DELETE }

    private final Entity entity;
    private final Action action;
    private final MutationIntent intent;
    private final List<TraceNode> traceChain;
    private final transient Entity diagnosticSource;

    public EntityPersistenceMutation(Entity entity, Action action) {
        this(entity, action, MutationIntent.of(entity.getComment()));
    }

    public EntityPersistenceMutation(Entity entity, Action action, MutationIntent intent) {
        this(entity, action, intent,
                MutationTraceScope.append(null, entity.typeName(), entity.getId(), intent.comment()).recover());
    }

    public EntityPersistenceMutation(Entity entity, Action action, MutationIntent intent, List<TraceNode> traceChain) {
        this(entity, action, intent, traceChain, entity);
    }

    public EntityPersistenceMutation(Entity entity, Action action, MutationIntent intent, List<TraceNode> traceChain,
            Entity diagnosticSource) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.action = Objects.requireNonNull(action, "action");
        this.intent = Objects.requireNonNull(intent, "intent");
        this.traceChain = List.copyOf(Objects.requireNonNull(traceChain, "traceChain"));
        this.diagnosticSource = diagnosticSource == null ? entity : diagnosticSource;
    }

    /** Original loaded values for invocation-local redaction; never used as the write payload. */
    @io.teaql.core.FrameworkInternal("Mutation diagnostic provenance only")
    public Entity diagnosticSource() { return diagnosticSource; }

    @Override public MutationIntent intent() { return intent; }

    public List<TraceNode> getTraceChain() { return traceChain; }

    public Entity getEntity() {
        return entity;
    }

    public Action getAction() {
        return action;
    }
}
