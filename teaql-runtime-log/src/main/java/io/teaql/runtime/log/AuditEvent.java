package io.teaql.runtime.log;

import java.util.List;

public class AuditEvent {
    private final String entityType;
    private final Object entityId;
    private final String mutationKind;
    private final List<FieldChange> changes;

    public AuditEvent(String entityType, Object entityId, String mutationKind, List<FieldChange> changes) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.mutationKind = mutationKind;
        this.changes = changes;
    }

    public String getEntityType() {
        return entityType;
    }

    public Object getEntityId() {
        return entityId;
    }

    public String getMutationKind() {
        return mutationKind;
    }

    public List<FieldChange> getChanges() {
        return changes;
    }
}
