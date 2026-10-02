package io.teaql.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Graph-owned immutable parent token. Context never owns a mutation scope. */
public final class MutationTraceScope {
    private final MutationTraceScope parent;
    private final TraceNode node;

    private MutationTraceScope(MutationTraceScope parent, TraceNode node) {
        this.parent = parent;
        this.node = node;
    }

    public static MutationTraceScope append(
            MutationTraceScope parent, String entityType, Long entityId, String reason) {
        if (RequestIntentException.blank(reason)) return parent;
        return new MutationTraceScope(parent,
                new TraceNode(TraceKind.AUDIT_REASON, entityType, entityId, reason));
    }

    public List<TraceNode> recover() {
        var nodes = new ArrayList<TraceNode>();
        for (MutationTraceScope scope = this; scope != null; scope = scope.parent) nodes.add(scope.node);
        Collections.reverse(nodes);
        return List.copyOf(nodes);
    }
}
