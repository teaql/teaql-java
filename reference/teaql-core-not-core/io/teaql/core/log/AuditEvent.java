package io.teaql.core.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Business audit event. Records the business semantics of entity changes.
 * Design aligned with teaql-rs RawAuditEvent.
 */
public class AuditEvent {

    public enum Kind {
        CREATED, UPDATED, DELETED, RECOVERED,
        SCHEMA_CREATED, SCHEMA_VERIFIED, FIELD_ADDED, DATA_SEEDED
    }

    private final Kind kind;
    private final String entity;
    private final Map<String, Object> values;
    private final List<String> updatedFields;
    private final Map<String, Object> oldValues;
    private final Map<String, Object> newValues;
    private final List<PropertyChange> changes;
    private final String comment;

    public AuditEvent(Kind kind, String entity, Map<String, Object> values,
                      List<String> updatedFields,
                      Map<String, Object> oldValues, Map<String, Object> newValues,
                      List<PropertyChange> changes, String comment) {
        this.kind = kind;
        this.entity = entity;
        this.values = values;
        this.updatedFields = updatedFields;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.changes = changes;
        this.comment = comment;
    }

    // --- Factory methods ---

    public static AuditEvent created(String entity, Map<String, Object> values, String comment) {
        List<PropertyChange> changes = new ArrayList<>();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            changes.add(new PropertyChange(e.getKey(), null, e.getValue()));
        }
        return new AuditEvent(Kind.CREATED, entity, values, List.of(), null, values, changes, comment);
    }

    public static AuditEvent updated(String entity, Map<String, Object> values,
                                      List<String> updatedFields,
                                      Map<String, Object> oldValues,
                                      Map<String, Object> newValues, String comment) {
        List<PropertyChange> changes = new ArrayList<>();
        for (String field : updatedFields) {
            Object oldVal = oldValues != null ? oldValues.get(field) : null;
            Object newVal = newValues != null ? newValues.get(field) : null;
            changes.add(new PropertyChange(field, oldVal, newVal));
        }
        return new AuditEvent(Kind.UPDATED, entity, values, updatedFields, oldValues, newValues, changes, comment);
    }

    public static AuditEvent deleted(String entity, Object id, Long expectedVersion, String comment) {
        Map<String, Object> values = Map.of("id", id);
        return new AuditEvent(Kind.DELETED, entity, values, List.of(), null, null, List.of(), comment);
    }

    public static AuditEvent recovered(String entity, Object id, Long expectedVersion, String comment) {
        Map<String, Object> values = Map.of("id", id);
        return new AuditEvent(Kind.RECOVERED, entity, values, List.of(), null, null, List.of(), comment);
    }

    // --- Getter ---

    public Kind getKind() { return kind; }
    public String getEntity() { return entity; }
    public Map<String, Object> getValues() { return values; }
    public List<String> getUpdatedFields() { return updatedFields; }
    public Map<String, Object> getOldValues() { return oldValues; }
    public Map<String, Object> getNewValues() { return newValues; }
    public List<PropertyChange> getChanges() { return changes; }
    public String getComment() { return comment; }

    /**
     * Field-level change record.
     */
    public static class PropertyChange {
        private final String field;
        private final Object oldValue;
        private final Object newValue;

        public PropertyChange(String field, Object oldValue, Object newValue) {
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getField() { return field; }
        public Object getOldValue() { return oldValue; }
        public Object getNewValue() { return newValue; }
    }
}
