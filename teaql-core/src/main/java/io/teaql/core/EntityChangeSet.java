package io.teaql.core;

import java.util.*;

/**
 * Tracks field-level changes for a set of entities.
 * Each entry maps an {@link EntityKey} to a record of changed fields and their new values.
 */
public class EntityChangeSet {
    private final Map<EntityKey, Map<String, Object>> changes = new TreeMap<>();
    private Map<EntityKey, Map<String, io.teaql.data.dynamic.DynamicFieldMutation>> dynamicChanges;

    public boolean isEmpty() {
        return changes.isEmpty() && (dynamicChanges == null || dynamicChanges.isEmpty());
    }

    public void set(EntityKey key, String field, Object value) {
        changes.computeIfAbsent(key, k -> new TreeMap<>()).put(field, value);
    }

    public Object get(EntityKey key, String field) {
        Map<String, Object> record = changes.get(key);
        return record != null ? record.get(field) : null;
    }

    boolean contains(EntityKey key, String field) {
        Map<String, Object> record = changes.get(key);
        return record != null && record.containsKey(field);
    }

    public Map<EntityKey, Map<String, Object>> changes() {
        return Collections.unmodifiableMap(changes);
    }

    public void setDynamic(EntityKey key, io.teaql.data.dynamic.DynamicFieldMutation mutation) {
        if (dynamicChanges == null) dynamicChanges = new TreeMap<>();
        dynamicChanges.computeIfAbsent(key, ignored -> new TreeMap<>()).put(mutation.code(), mutation);
    }

    public Map<EntityKey, Map<String, io.teaql.data.dynamic.DynamicFieldMutation>> dynamicChanges() {
        if (dynamicChanges == null) return Collections.emptyMap();
        Map<EntityKey, Map<String, io.teaql.data.dynamic.DynamicFieldMutation>> view = new TreeMap<>();
        dynamicChanges.forEach((key, values) -> view.put(key, Collections.unmodifiableMap(values)));
        return Collections.unmodifiableMap(view);
    }

    public void clearEntity(EntityKey key) {
        changes.remove(key);
        if (dynamicChanges != null) dynamicChanges.remove(key);
    }

    public Set<String> fieldNames(EntityKey key) {
        Map<String, Object> record = changes.get(key);
        var dynamic = dynamicChanges == null ? null : dynamicChanges.get(key);
        if (dynamic != null && !dynamic.isEmpty()) {
            Set<String> names = new TreeSet<>(record == null ? Collections.emptySet() : record.keySet());
            dynamic.keySet().forEach(code -> names.add("#" + code));
            return Collections.unmodifiableSet(names);
        }
        return record != null ? Collections.unmodifiableSet(record.keySet()) : Collections.emptySet();
    }
}
