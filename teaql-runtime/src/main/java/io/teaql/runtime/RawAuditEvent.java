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
        Instant occurredAt,
        MutationGovernanceSnapshot governance) {

    public RawAuditEvent {
        changes = List.copyOf(changes == null ? List.of() : changes);
        traceChain = List.copyOf(traceChain == null ? List.of() : traceChain);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public RawAuditEvent(
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
        this(kind, entityType, entityId, changes, traceChain, actor, category, reason,
                resultingVersion, occurredAt, null);
    }
}
