package io.teaql.core;

import java.util.ArrayList;
import java.util.List;

/** Immutable statement-owned source path and separate graph mutation lineage. */
public record SqlExecutionTrace(List<TraceNode> source, List<TraceNode> mutationLineage, String operation) {
    public SqlExecutionTrace {
        source = List.copyOf(source);
        mutationLineage = List.copyOf(mutationLineage);
    }

    public static SqlExecutionTrace mutation(Entity entity, List<TraceNode> lineage, String operation) {
        var source = new ArrayList<>(lineage);
        source.add(new TraceNode(TraceKind.ENTITY, entity.typeName(), entity.getId(), ""));
        return new SqlExecutionTrace(source, lineage, operation);
    }

    public SqlExecutionTrace readback(MutationIntent intent) {
        var frames = new ArrayList<>(source);
        String root = frames.isEmpty() ? "unknown" : frames.get(0).getName();
        frames.add(new TraceNode(TraceKind.COMMENT, root, intent.comment()));
        frames.add(new TraceNode(TraceKind.PURPOSE, root, intent.readbackIntent().purpose()));
        return new SqlExecutionTrace(frames, mutationLineage, "select");
    }

    public void applyTo(ExecutionMetadata metadata) {
        metadata.setTraceChain(source);
        metadata.setMutationLineage(mutationLineage);
        metadata.setStatementOperation(operation);
    }
}
