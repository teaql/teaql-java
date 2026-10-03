package io.teaql.core;

import java.util.ArrayList;
import java.util.List;

/** Immutable statement-owned source path and separate graph mutation lineage. */
public record SqlExecutionTrace(List<TraceNode> source, List<TraceNode> mutationLineage, String operation,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.function.Consumer<ExecutionMetadata> statementObserver) {
    public SqlExecutionTrace(List<TraceNode> source, List<TraceNode> mutationLineage, String operation) {
        this(source, mutationLineage, operation, null);
    }

    /** Invocation-owned collection; never installed on Context or a cached repository. */
    public SqlExecutionTrace collecting(java.util.function.Consumer<ExecutionMetadata> observer) {
        return new SqlExecutionTrace(source, mutationLineage, operation, observer);
    }

    public void recordStatement(ExecutionMetadata metadata) {
        if (statementObserver == null) return;
        var path = SqlTracePath.canonical(metadata.getTraceChain(), metadata.getBackend(), operation);
        metadata.setTraceChain(path.path());
        metadata.setComment(path.comment());
        metadata.setPurpose(path.purpose());
        metadata.setAuditReason(path.auditReason());
        statementObserver.accept(metadata);
    }
    public SqlExecutionTrace {
        source = List.copyOf(source);
        mutationLineage = List.copyOf(mutationLineage);
    }

    public static SqlExecutionTrace mutation(Entity entity, List<TraceNode> lineage, String operation) {
        var source = new ArrayList<>(lineage);
        source.add(new TraceNode(TraceKind.ENTITY, entity.typeName(), entity.getId(), ""));
        return new SqlExecutionTrace(source, lineage, operation);
    }

    public static SqlExecutionTrace query(SearchRequest<?> request) {
        List<TraceNode> source = request.sqlTraceSource();
        if (source.isEmpty()) {
            QueryIntent intent = request.inheritedQueryIntent() == null
                    ? QueryIntent.of(request.comment(), request.purpose()) : request.inheritedQueryIntent();
            source = List.of(new TraceNode(TraceKind.COMMENT, request.getTypeName(), intent.comment()),
                    new TraceNode(TraceKind.PURPOSE, request.getTypeName(), intent.purpose()));
        }
        return new SqlExecutionTrace(source, List.of(), "select");
    }

    public SqlExecutionTrace readback(MutationIntent intent) {
        var frames = new ArrayList<>(source);
        String root = frames.isEmpty() ? "unknown" : frames.get(0).getName();
        frames.add(new TraceNode(TraceKind.COMMENT, root, intent.comment()));
        frames.add(new TraceNode(TraceKind.PURPOSE, root, intent.readbackIntent().purpose()));
        return new SqlExecutionTrace(frames, mutationLineage, "select", statementObserver);
    }

    public void applyTo(ExecutionMetadata metadata) {
        metadata.setTraceChain(source);
        metadata.setMutationLineage(mutationLineage);
        metadata.setStatementOperation(operation);
    }
}
