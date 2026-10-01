package io.teaql.core;

/**
 * A wrapper that carries a mandatory audit comment with an entity.
 * Only `Audited<T>` has `.save()` and `.recover()` methods — bare entities cannot be saved directly.
 */
public class Audited<T extends Entity> {
    private final T inner;
    private final MutationIntent intent;

    public Audited(T entity, String comment) {
        this.intent = MutationIntent.of(comment);
        this.inner = entity;
        this.inner.setComment(comment);
    }

    public T entity() {
        return inner;
    }

    @SuppressWarnings("unchecked")
    public <R extends T> R save(UserContext context) {
        this.inner.setComment(intent.comment());
        context.saveGraph(this.inner);
        return (R) this.inner;
    }

    @SuppressWarnings("unchecked")
    public <R extends T> R recover(UserContext context) {
        this.inner.markAsRecover();
        this.inner.setComment(intent.comment());
        context.saveGraph(this.inner);
        return (R) this.inner;
    }
}
