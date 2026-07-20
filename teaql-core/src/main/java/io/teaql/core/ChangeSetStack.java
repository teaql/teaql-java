package io.teaql.core;

import java.util.*;

/**
 * A stack of {@link EntityChangeSet} instances supporting nested save scopes.
 * Changes are recorded to the topmost change set.
 * When reading, the stack is searched from top to bottom to find the most recent value.
 */
public class ChangeSetStack {
    private final List<EntityChangeSet> stack = new ArrayList<>();

    public EntityChangeSet currentMut() {
        if (stack.isEmpty()) {
            stack.add(new EntityChangeSet());
        }
        return stack.get(stack.size() - 1);
    }

    public EntityChangeSet current() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    public void push() {
        stack.add(new EntityChangeSet());
    }

    public EntityChangeSet pop() {
        return stack.isEmpty() ? null : stack.remove(stack.size() - 1);
    }

    public Object get(EntityKey key, String field) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            EntityChangeSet changeSet = stack.get(i);
            if (changeSet.contains(key, field)) {
                return changeSet.get(key, field);
            }
        }
        return null;
    }

    public void set(EntityKey key, String field, Object value) {
        currentMut().set(key, field, value);
    }

    public void clearCurrent() {
        if (!stack.isEmpty()) {
            stack.set(stack.size() - 1, new EntityChangeSet());
        }
    }

    public void clearEntity(EntityKey key) {
        for (EntityChangeSet changeSet : stack) {
            changeSet.clearEntity(key);
        }
    }

    public Set<String> changedFieldNames(EntityKey key) {
        Set<String> fields = new TreeSet<>();
        for (EntityChangeSet changeSet : stack) {
            fields.addAll(changeSet.fieldNames(key));
        }
        return fields;
    }
}
