package io.teaql.runtime.log;

import io.teaql.core.log.TraceNode;
import java.util.List;

public interface LogFormatter {
    String formatExecutionLog(io.teaql.core.ExecutionMetadata metadata);
    String formatAuditLog(List<TraceNode> traceChain, AuditEvent event);
}
