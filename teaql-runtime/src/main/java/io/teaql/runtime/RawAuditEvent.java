package io.teaql.runtime;

import io.teaql.core.TraceNode;
import java.time.Instant;
import java.util.List;

public record RawAuditEvent(
        MutationAuditKind kind,
        String entityType,
        Object entityId,
        List<AuditFieldChange> changes,
        List<TraceNode> traceChain,
        String actor,
        String category,
        String reason,
        Long resultingVersion,
        Instant occurredAt) {

    public RawAuditEvent {
        changes = List.copyOf(changes == null ? List.of() : changes);
        traceChain = List.copyOf(traceChain == null ? List.of() : traceChain);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
