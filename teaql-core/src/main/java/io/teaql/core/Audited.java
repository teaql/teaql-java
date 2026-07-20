package io.teaql.core;

/**
 * A wrapper that carries a mandatory audit comment with an entity.
 * Only `Audited<T>` has `.save()`, `.delete()`, and `.recover()` methods — bare entities cannot be saved directly.
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

    public T save(UserContext ctx) {
        ctx.saveGraph(this.inner);
        return this.inner;
    }

    public void delete(UserContext ctx) {
        this.inner.markAsDeleted();
        ctx.saveGraph(this.inner);
    }

    public T recover(UserContext ctx) {
        this.inner.markAsRecover();
        ctx.saveGraph(this.inner);
        return this.inner;
    }
}
