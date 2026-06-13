package io.teaql.core.log;

public class TraceNode {
    private final String comment;

    public TraceNode(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return comment;
    }
}
