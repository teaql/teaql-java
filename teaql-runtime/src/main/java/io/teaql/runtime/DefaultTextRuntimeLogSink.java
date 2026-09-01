package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import io.teaql.core.UserContext;
import java.util.List;
import java.util.stream.IntStream;

/** Default value-bearing diagnostic SQL destination. */
public final class DefaultTextRuntimeLogSink implements RuntimeLogSink {
    @Override
    public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) {
        System.err.printf(
                "[TeaQL SQL][%s][%dus] %s comment=%s purpose=%s auditReason=%s tracePath=%s%n"
                        + "Parameterized SQL: %s params=%s%nDebug SQL: %s%n",
                metadata.getOperation() == null ? "unknown" : metadata.getOperation().name().toLowerCase(),
                metadata.getElapsedUs(), resultSummary(metadata),
                nullToEmpty(metadata.getComment()), nullToEmpty(metadata.getPurpose()),
                nullToEmpty(metadata.getAuditReason()), formatTrace(metadata.getTraceChain()),
                nullToEmpty(metadata.getParameterizedQuery()), metadata.getParameters(),
                nullToEmpty(metadata.getDebugQuery()));
    }

    private static String formatTrace(List<TraceNode> nodes) {
        if (nodes == null) return "[]";
        return "[" + IntStream.range(0, nodes.size())
                .mapToObj(index -> index + ":" + nodes.get(index))
                .reduce((left, right) -> left + " -> " + right).orElse("") + "]";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String resultSummary(ExecutionMetadata metadata) {
        if (metadata.getResultCount() != null) {
            return metadata.getResultCount() + " rows returned";
        }
        if (metadata.getAffectedRows() != null) {
            return metadata.getAffectedRows() + " rows affected";
        }
        return nullToEmpty(metadata.getResultSummary());
    }
}
