package io.teaql.runtime;

import io.teaql.core.TraceNode;
import java.util.List;

public record SafeAuditEvent(
        MutationAuditKind kind,
        String entityType,
        Object entityId,
        List<SafeAuditField> fields,
        List<TraceNode> traceChain) {

    public SafeAuditEvent {
        fields = List.copyOf(fields == null ? List.of() : fields);
        traceChain = List.copyOf(traceChain == null ? List.of() : traceChain);
    }
}
