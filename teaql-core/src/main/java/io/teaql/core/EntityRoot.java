package io.teaql.core;

import java.util.*;

/**
 * Central change tracking context shared across all entities in a save graph.
 * Holds the change set stack, deleted keys, new keys, trace chains, and original versions.
 *
 * This is the Java equivalent of Rust's {@code EntityRoot}.
 */
public class EntityRoot {
    private final ChangeSetStack changeSets = new ChangeSetStack();
    private String comment;
    private final Set<EntityKey> deletedKeys = new TreeSet<>();
    private final Set<EntityKey> newKeys = new TreeSet<>();
    private final Map<EntityKey, String> traceChains = new TreeMap<>();
    private final Map<EntityKey, Long> originalVersions = new TreeMap<>();

    // --- Change Set Stack ---

    public void pushChangeSet() {
        changeSets.push();
    }

    public EntityChangeSet popChangeSet() {
        return changeSets.pop();
    }

    public void clearCurrentChangeSet() {
        changeSets.clearCurrent();
    }

    public void set(EntityKey key, String field, Object value) {
        changeSets.set(key, field, value);
    }

    public Object get(EntityKey key, String field) {
        return changeSets.get(key, field);
    }

    public EntityChangeSet currentChangeSet() {
        EntityChangeSet cs = changeSets.current();
        return cs != null ? cs : new EntityChangeSet();
    }

    // --- Comment ---

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    // --- New Keys ---

    public void markAsNew(EntityKey key) {
        newKeys.add(key);
    }

    public boolean isNew(EntityKey key) {
        return newKeys.contains(key);
    }

    public Set<EntityKey> newKeys() {
        return Collections.unmodifiableSet(newKeys);
    }

    // --- Deleted Keys ---

    public void markAsDelete(EntityKey key) {
        changeSets.clearEntity(key);
        deletedKeys.add(key);
    }

    public boolean isMarkedAsDelete(EntityKey key) {
        return deletedKeys.contains(key);
    }

    public Set<EntityKey> deletedKeys() {
        return Collections.unmodifiableSet(deletedKeys);
    }

    // --- Changed Fields ---

    public Set<String> changedFieldNames(EntityKey key) {
        return changeSets.changedFieldNames(key);
    }

    // --- Trace Chains ---

    public void setTraceChain(EntityKey key, String traceChain) {
        traceChains.put(key, traceChain);
    }

    public String getTraceChain(EntityKey key) {
        return traceChains.get(key);
    }

    // --- Original Versions ---

    public void setOriginalVersion(EntityKey key, Long version) {
        originalVersions.put(key, version);
    }

    public Long getOriginalVersion(EntityKey key) {
        return originalVersions.get(key);
    }
}
