package io.teaql.core;

import java.util.ArrayList;
import java.util.List;

/** Immutable statement-owned source path and separate graph mutation lineage. */
public record SqlExecutionTrace(List<TraceNode> source, List<TraceNode> mutationLineage, String operation,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.function.Consumer<ExecutionMetadata> statementObserver,
        @com.fasterxml.jackson.annotation.JsonIgnore MutationIntent mutationIntent,
        @com.fasterxml.jackson.annotation.JsonIgnore QueryRequestScope queryScope) {
    public SqlExecutionTrace(List<TraceNode> source, List<TraceNode> mutationLineage, String operation,
            java.util.function.Consumer<ExecutionMetadata> statementObserver, MutationIntent mutationIntent) {
        this(source, mutationLineage, operation, statementObserver, mutationIntent, null);
    }
    public SqlExecutionTrace(List<TraceNode> source, List<TraceNode> mutationLineage, String operation) {
        this(source, mutationLineage, operation, null, null);
    }
    public SqlExecutionTrace(List<TraceNode> source, List<TraceNode> mutationLineage, String operation,
            java.util.function.Consumer<ExecutionMetadata> statementObserver) {
        this(source, mutationLineage, operation, statementObserver, null);
    }

    /** Invocation-owned collection; never installed on Context or a cached repository. */
    public SqlExecutionTrace collecting(java.util.function.Consumer<ExecutionMetadata> observer) {
        return new SqlExecutionTrace(source, mutationLineage, operation, observer, mutationIntent, queryScope);
    }

    public void recordStatement(ExecutionMetadata metadata) {
        var path = SqlTracePath.canonical(metadata.getTraceChain(), metadata.getBackend(), operation);
        metadata.setTraceChain(path.path());
        metadata.setComment(path.comment());
        metadata.setPurpose(path.purpose());
        if (queryScope != null) {
            metadata.setRequestTraceChain(queryScope.traceChain());
            metadata.setComment(queryScope.intent().comment());
            metadata.setPurpose(queryScope.purpose());
        }
        // Canonicalization extracts the last local reason for standalone source
        // vectors. Executed mutations instead own their root intent independently
        // of the graph lineage, including write readback and prepared batches.
        metadata.setAuditReason(mutationIntent == null ? path.auditReason() : mutationIntent.auditReason());
        if (statementObserver != null) statementObserver.accept(metadata);
    }
    public SqlExecutionTrace {
        source = List.copyOf(source);
        mutationLineage = List.copyOf(mutationLineage);
    }

    public static SqlExecutionTrace mutation(Entity entity, List<TraceNode> lineage, String operation,
            MutationIntent intent) {
        java.util.Objects.requireNonNull(intent, "mutation intent");
        var source = new ArrayList<>(lineage);
        source.add(new TraceNode(TraceKind.ENTITY, entity.typeName(), entity.getId(), ""));
        return new SqlExecutionTrace(source, lineage, operation, null, intent);
    }

    public static SqlExecutionTrace query(SearchRequest<?> request) {
        List<TraceNode> source = request.sqlTraceSource();
        if (source.isEmpty()) {
            QueryIntent intent = request.inheritedQueryIntent() == null
                    ? QueryIntent.of(request.comment(), request.purpose()) : request.inheritedQueryIntent();
            source = List.of(new TraceNode(TraceKind.COMMENT, request.getTypeName(), intent.comment()),
                    new TraceNode(TraceKind.PURPOSE, request.getTypeName(), intent.purpose()));
        }
        return new SqlExecutionTrace(source, List.of(), "select", null, null, QueryRequestScope.capture(request));
    }

    public SqlExecutionTrace readback(MutationIntent intent) {
        var frames = new ArrayList<>(source);
        String root = frames.isEmpty() ? "unknown" : frames.get(0).getName();
        frames.add(new TraceNode(TraceKind.COMMENT, root, intent.comment()));
        frames.add(new TraceNode(TraceKind.PURPOSE, root, intent.readbackIntent().purpose()));
        return new SqlExecutionTrace(frames, mutationLineage, "select", statementObserver, intent);
    }

    public void applyTo(ExecutionMetadata metadata) {
        metadata.setTraceChain(source);
        if (queryScope != null) metadata.setRequestTraceChain(queryScope.traceChain());
        metadata.setMutationLineage(mutationLineage);
        metadata.setStatementOperation(operation);
    }
}
