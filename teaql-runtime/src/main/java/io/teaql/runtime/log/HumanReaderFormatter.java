package io.teaql.runtime.log;

import io.teaql.core.log.TraceNode;
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
        return traceChain.stream()
                .map(t -> (CharSequence) t.getComment())
                .collect(Collectors.joining(" -> "));
    }

    @Override
    public String formatExecutionLog(io.teaql.core.ExecutionMetadata metadata) {
        String ts = LocalDateTime.now().format(TS_FORMATTER);
        String traceStr = formatTraceChain(metadata.getTraceChain());
        String traceDisplay = traceStr.isEmpty() ? "" : " - [" + traceStr + "]";
        
        String cleanQuery = metadata.getDebugQuery() == null ? "" : metadata.getDebugQuery().replace('\n', ' ');
        return String.format("[%s]-[%5dµs]-[DEBUG]-ExecutionLog%s - [%s]\n          %s",
                ts, metadata.getElapsedUs(), traceDisplay, metadata.getResultSummary(), cleanQuery);
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
