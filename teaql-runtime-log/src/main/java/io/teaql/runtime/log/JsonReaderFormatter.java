package io.teaql.runtime.log;

import io.teaql.core.TraceNode;
import java.util.List;
import java.util.stream.Collectors;

public class JsonReaderFormatter implements LogFormatter {
    private String formatTraceChain(List<TraceNode> traceChain) {
        if (traceChain == null || traceChain.isEmpty()) {
            return "[]";
        }
        return "[" + traceChain.stream()
                .map(t -> (CharSequence)("\"" + escapeJson(t.getComment()) + "\""))
                .collect(Collectors.joining(",")) + "]";
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    @Override
    public String formatExecutionLog(io.teaql.core.ExecutionMetadata metadata) {
        return String.format("{\"type\":\"EXEC_LOG\",\"trace\":%s,\"backend\":\"%s\",\"elapsedUs\":%d,\"summary\":\"%s\",\"query\":\"%s\"}",
                formatTraceChain(metadata.getTraceChain()),
                escapeJson(metadata.getBackend()),
                metadata.getElapsedUs(),
                escapeJson(metadata.getResultSummary()),
                escapeJson(metadata.getDebugQuery()));
    }

    @Override
    public String formatAuditLog(List<TraceNode> traceChain, AuditEvent event) {
        return String.format("{\"type\":\"AUDIT_LOG\",\"trace\":%s,\"entity\":\"%s\",\"id\":\"%s\",\"kind\":\"%s\"}",
                formatTraceChain(traceChain),
                escapeJson(event.getEntityType()),
                event.getEntityId() != null ? escapeJson(event.getEntityId().toString()) : "null",
                escapeJson(event.getMutationKind()));
    }
}
