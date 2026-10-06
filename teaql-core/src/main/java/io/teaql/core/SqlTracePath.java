package io.teaql.core;

import java.util.ArrayList;
import java.util.List;

/** Rust-compatible canonical SQL path. Intent is extracted, never repeated on path frames. */
public final class SqlTracePath {
    private SqlTracePath() {}

    public record Result(List<TraceNode> path, String comment, String purpose, String auditReason) {}

    public static Result canonical(List<TraceNode> source, String backend, String operation) {
        source = source == null ? List.of() : source;
        String comment = null, purpose = null, auditReason = null;
        for (TraceNode node : source) {
            if (node.getKind() == TraceKind.COMMENT) comment = node.getComment();
            if (node.getKind() == TraceKind.PURPOSE) purpose = node.getComment();
            if (node.getKind() == TraceKind.AUDIT_REASON) auditReason = node.getComment();
        }
        boolean canonical = has(source, TraceKind.OPERATION) && has(source, TraceKind.PROVIDER) && has(source, TraceKind.SQL);
        if (canonical) {
            return new Result(source.stream().filter(node -> !intent(node.getKind())).toList(), comment, purpose, auditReason);
        }
        String root = source.stream().map(TraceNode::getName).filter(name -> !RequestIntentException.blank(name))
                .findFirst().orElse("unknown");
        boolean query = "select".equals(operation);
        String statementEntity = root;
        if (!query) {
            for (TraceNode node : source) {
                if (node.getKind() == TraceKind.ENTITY && !RequestIntentException.blank(node.getName()))
                    statementEntity = node.getName();
            }
        }
        var path = new ArrayList<TraceNode>();
        path.add(new TraceNode(TraceKind.OPERATION, root, query ? "query" : "mutation"));
        path.add(new TraceNode(query ? TraceKind.REQUEST : TraceKind.ENTITY, query ? root : statementEntity, ""));
        source.stream().filter(node -> node.getKind() == TraceKind.RELATION).forEach(path::add);
        path.add(new TraceNode(TraceKind.PROVIDER, RequestIntentException.blank(backend) ? "unknown" : backend, ""));
        path.add(new TraceNode(TraceKind.SQL, operation, ""));
        return new Result(List.copyOf(path), comment, purpose, auditReason);
    }

    private static boolean has(List<TraceNode> source, TraceKind kind) {
        return source.stream().anyMatch(node -> node.getKind() == kind);
    }

    private static boolean intent(TraceKind kind) {
        return kind == TraceKind.COMMENT || kind == TraceKind.PURPOSE || kind == TraceKind.AUDIT_REASON;
    }
}
