package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.UserContext;
import java.util.Map;

/** The default server sink; existing text sinks remain explicit application choices. */
public final class RuntimeDiagnosticLogSink extends DefaultTextRuntimeLogSink {
    private final RuntimeLogger logger;
    public RuntimeDiagnosticLogSink(RuntimeLogger logger) { this.logger = logger; }
    @Override public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) { logger.writeSql(metadata); }
    @Override public void writeAuditEvent(UserContext context, RawAuditEvent event) { logger.writeAudit(event); }
    @Override public void writeMutationGovernanceEvent(UserContext context, MutationGovernanceEvent event) {
        if (event.firstOccurrence()) logger.write("WARN", "mutation.policy.warning", "TeaQL mutation policy warning",
                Map.of("category", "AUDIT", "outcome", "rejected", "error_code", event.warningCode()));
    }
}
