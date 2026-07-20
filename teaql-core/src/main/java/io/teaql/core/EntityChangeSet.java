package io.teaql.core;

import java.util.*;

/**
 * Tracks field-level changes for a set of entities.
 * Each entry maps an {@link EntityKey} to a record of changed fields and their new values.
 */
public class EntityChangeSet {
    private final Map<EntityKey, Map<String, Object>> changes = new TreeMap<>();

    public boolean isEmpty() {
        return changes.isEmpty();
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

    public void clearEntity(EntityKey key) {
        changes.remove(key);
    }

    public Set<String> fieldNames(EntityKey key) {
        Map<String, Object> record = changes.get(key);
        return record != null ? Collections.unmodifiableSet(record.keySet()) : Collections.emptySet();
    }
}
