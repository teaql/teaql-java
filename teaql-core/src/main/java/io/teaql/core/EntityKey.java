package io.teaql.core;

import java.util.Objects;

/**
 * Identifies a specific entity instance by type name and id.
 * Used as the key in change sets to track modifications per entity.
 */
public class EntityKey implements Comparable<EntityKey> {
    private final String entity;
    private final Long id;

    public EntityKey(String entity, Long id) {
        this.entity = Objects.requireNonNull(entity, "entity type must not be null");
        this.id = id;
    }

    public String entity() {
        return entity;
    }

    public Long id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityKey other)) return false;
        return Objects.equals(entity, other.entity) && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, id);
    }

    @Override
    public int compareTo(EntityKey other) {
        int cmp = this.entity.compareTo(other.entity);
        if (cmp != 0) return cmp;
        return Long.compare(
            this.id != null ? this.id : Long.MIN_VALUE,
            other.id != null ? other.id : Long.MIN_VALUE
        );
    }

    @Override
    public String toString() {
        return entity + ":" + id;
    }
}
