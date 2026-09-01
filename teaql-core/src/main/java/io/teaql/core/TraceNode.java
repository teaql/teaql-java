package io.teaql.core;

public class TraceNode {
    private final TraceKind kind;
    private final String name;
    private final String comment;

    public TraceNode(String comment) {
        this(TraceKind.ENTITY, "", comment);
    }

    public TraceNode(TraceKind kind, String name, String comment) {
        this.kind = kind;
        this.name = name;
        this.comment = comment;
    }

    public TraceKind getKind() { return kind; }

    public String getName() { return name; }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return kind + ":" + name + "=" + comment;
    }
}
