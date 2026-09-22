package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import io.teaql.core.UserContext;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.IntStream;

/** Default operator log. It deliberately excludes bind values and rendered Debug SQL. */
public class DefaultTextRuntimeLogSink implements RuntimeLogSink {
    protected final PrintStream output;

    public DefaultTextRuntimeLogSink() {
        this(System.err);
    }

    public DefaultTextRuntimeLogSink(PrintStream output) {
        this.output = output;
    }

    @Override
    public boolean requiresSensitiveSqlData() {
        return false;
    }

    @Override
    public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) {
        output.printf(
                "[TeaQL SQL][%s][%dus] %s comment=%s purpose=%s auditReason=%s tracePath=%s%n"
                        + "Parameterized SQL: %s%n",
                metadata.getOperation() == null ? "unknown" : metadata.getOperation().name().toLowerCase(),
                metadata.getElapsedUs(), resultSummary(metadata),
                nullToEmpty(metadata.getComment()), nullToEmpty(metadata.getPurpose()),
                nullToEmpty(metadata.getAuditReason()), formatTrace(metadata.getTraceChain()),
                nullToEmpty(metadata.getParameterizedQuery()));
    }

    protected static String formatTrace(List<TraceNode> nodes) {
        if (nodes == null) return "[]";
        return "[" + IntStream.range(0, nodes.size())
                .mapToObj(index -> index + ":" + nodes.get(index))
                .reduce((left, right) -> left + " -> " + right).orElse("") + "]";
    }

    protected static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    protected static String resultSummary(ExecutionMetadata metadata) {
        if (metadata.getResultCount() != null) {
            return metadata.getResultCount() + " rows returned";
        }
        if (metadata.getAffectedRows() != null) {
            return metadata.getAffectedRows() + " rows affected";
        }
        return nullToEmpty(metadata.getResultSummary());
    }
}
