package io.teaql.core;

public final class TraceNode {
    private final TraceKind kind;
    private final String name;
    private final String comment;
    private final Long entityId;

    public TraceNode(String comment) {
        this(TraceKind.ENTITY, "", comment);
    }

    public TraceNode(TraceKind kind, String name, String comment) {
        this(kind, name, null, comment);
    }

    public TraceNode(TraceKind kind, String name, Long entityId, String comment) {
        this.kind = kind;
        this.name = name;
        this.entityId = entityId;
        this.comment = comment;
    }

    public TraceKind getKind() { return kind; }

    public String getName() { return name; }

    public Long getEntityId() { return entityId; }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return kind + ":" + name + (entityId == null ? "" : "#" + entityId) + "=" + comment;
    }

    @Override public boolean equals(Object other) {
        return other instanceof TraceNode node && kind == node.kind
                && java.util.Objects.equals(name, node.name)
                && java.util.Objects.equals(entityId, node.entityId)
                && java.util.Objects.equals(comment, node.comment);
    }

    @Override public int hashCode() {
        return java.util.Objects.hash(kind, name, entityId, comment);
    }
}
