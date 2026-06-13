package io.teaql.runtime.log;

import io.teaql.core.log.TraceNode;
import java.util.List;

public interface LogFormatter {
    String formatSqlLog(List<TraceNode> traceChain, SqlLogEntry entry);
    String formatAuditLog(List<TraceNode> traceChain, AuditEvent event);
}
