package io.teaql.runtime;

import io.teaql.core.TraceNode;
import java.util.List;

public record RawAuditEvent(
        MutationAuditKind kind,
        String entityType,
        Object entityId,
        List<AuditFieldChange> changes,
        List<TraceNode> traceChain) {

    public RawAuditEvent {
        changes = List.copyOf(changes == null ? List.of() : changes);
        traceChain = List.copyOf(traceChain == null ? List.of() : traceChain);
    }
}
