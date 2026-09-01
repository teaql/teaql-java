package io.teaql.runtime.log;

import io.teaql.core.TraceNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class HumanReaderFormatter implements LogFormatter {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private String formatTraceChain(List<TraceNode> traceChain) {
        if (traceChain == null || traceChain.isEmpty()) {
            return "";
        }
        return java.util.stream.IntStream.range(0, traceChain.size())
                .mapToObj(i -> {
                    TraceNode t = traceChain.get(i);
                    return i + ":" + t.getKind() + ":" + t.getName() + "=" + t.getComment();
                })
                .collect(Collectors.joining(" -> "));
    }

    @Override
    public String formatExecutionLog(io.teaql.core.ExecutionMetadata metadata) {
        String ts = LocalDateTime.now().format(TS_FORMATTER);
        String traceStr = formatTraceChain(metadata.getTraceChain());
        String traceDisplay = traceStr.isEmpty() ? "" : " - [" + traceStr + "]";
        
        String parameterized = metadata.getParameterizedQuery() == null ? "" : metadata.getParameterizedQuery().replace('\n', ' ');
        String debug = metadata.getDebugQuery() == null ? "" : metadata.getDebugQuery().replace('\n', ' ');
        return String.format("[%s]-[%5dµs]-[DEBUG]-ExecutionLog%s - [%s] comment=%s purpose=%s auditReason=%s\n          Parameterized SQL: %s params=%s\n          Debug SQL: %s",
                ts, metadata.getElapsedUs(), traceDisplay, metadata.getResultSummary(),
                metadata.getComment(), metadata.getPurpose(), metadata.getAuditReason(),
                parameterized, metadata.getParameters(), debug);
    }

    @Override
    public String formatAuditLog(List<TraceNode> traceChain, AuditEvent event) {
        String ts = LocalDateTime.now().format(TS_FORMATTER);
        String traceStr = formatTraceChain(traceChain);
        String traceDisplay = traceStr.isEmpty() ? "" : " (Trace: " + traceStr + ")";
        
        String fieldsPart = "";
        if (event.getChanges() != null && !event.getChanges().isEmpty()) {
            fieldsPart = " {" + event.getChanges().stream()
                    .filter(c -> !c.getField().startsWith("_"))
                    .map(c -> c.getField() + ": " + (c.getNewValue() == null ? "null" : c.getNewValue().toString()))
                    .collect(Collectors.joining(", ")) + "}";
        }
        
        String entityIdStr = event.getEntityId() != null ? event.getEntityId().toString() : "Unknown";
        return String.format("[%s]-[AUDIT]-Entity [%s:%s] %s%s%s",
                ts, event.getEntityType(), entityIdStr, event.getMutationKind(), traceDisplay, fieldsPart);
    }
}
