package io.teaql.core;

/**
 * A wrapper that carries a mandatory audit comment with an entity.
 * Only `Audited<T>` has `.save()` and `.recover()` methods — bare entities cannot be saved directly.
 */
public class Audited<T extends Entity> {
    private final T inner;

    public Audited(T entity, String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Audit comment must not be empty");
        }
        this.inner = entity;
        this.inner.setComment(comment);
    }

    public T entity() {
        return inner;
    }

    @SuppressWarnings("unchecked")
    public <R extends T> R save(UserContext context) {
        context.saveGraph(this.inner);
        return (R) this.inner;
    }

    @SuppressWarnings("unchecked")
    public <R extends T> R recover(UserContext context) {
        this.inner.markAsRecover();
        context.saveGraph(this.inner);
        return (R) this.inner;
    }
}
