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
                .map(t -> (CharSequence)("{\"kind\":\"" + t.getKind()
                        + "\",\"name\":\"" + escapeJson(t.getName())
                        + "\",\"value\":\"" + escapeJson(t.getComment()) + "\"}"))
                .collect(Collectors.joining(",")) + "]";
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    @Override
    public String formatExecutionLog(io.teaql.core.ExecutionMetadata metadata) {
        return String.format("{\"type\":\"EXEC_LOG\",\"tracePath\":%s,\"backend\":\"%s\",\"operation\":\"%s\",\"comment\":\"%s\",\"purpose\":\"%s\",\"auditReason\":\"%s\",\"elapsedUs\":%d,\"resultCount\":%s,\"affectedRows\":%s,\"summary\":\"%s\",\"parameterizedSQL\":\"%s\",\"parameters\":\"%s\",\"debugSQL\":\"%s\"}",
                formatTraceChain(metadata.getTraceChain()),
                escapeJson(metadata.getBackend()),
                metadata.getOperation(),
                escapeJson(metadata.getComment()),
                escapeJson(metadata.getPurpose()),
                escapeJson(metadata.getAuditReason()),
                metadata.getElapsedUs(),
                metadata.getResultCount(),
                metadata.getAffectedRows(),
                escapeJson(metadata.getResultSummary()),
                escapeJson(metadata.getParameterizedQuery()),
                escapeJson(String.valueOf(metadata.getParameters())),
                escapeJson(metadata.getDebugQuery()));
    }

    @Override
    public String formatAuditLog(List<TraceNode> traceChain, AuditEvent event) {
        return String.format("{\"type\":\"AUDIT_LOG\",\"trace\":%s,\"entity\":\"%s\",\"id\":\"%s\",\"kind\":\"%s\"}",
                formatTraceChain(traceChain),
                escapeJson(event.getEntityType()),
                formatEntityId(event),
                escapeJson(event.getMutationKind()));
    }

    protected String formatEntityId(AuditEvent event) {
        return event.getEntityId() != null ? escapeJson(event.getEntityId().toString()) : "null";
    }
}
