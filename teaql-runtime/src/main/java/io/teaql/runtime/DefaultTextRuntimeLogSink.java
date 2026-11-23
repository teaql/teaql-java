package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import io.teaql.core.UserContext;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.IntStream;

/** Default operator log, showing safe expanded SQL rather than a separate bind array. */
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
        metadata = LogPrivacy.sql(metadata, false);
        output.printf(
                "[TeaQL SQL][%s][%dus] %s outcome=%s batchOutcome=%s comment=%s purpose=%s auditReason=%s traceChain=%s tracePath=%s mutationLineage=%s%s%n"
                        + "SQL: %s%n",
                metadata.getOperation() == null ? "unknown" : metadata.getOperation().name().toLowerCase(),
                metadata.getElapsedUs(), resultSummary(metadata),
                metadata.getExecutionOutcome() == null ? "unknown" : metadata.getExecutionOutcome(),
                metadata.getBatchOutcome() == null ? "not_applicable" : metadata.getBatchOutcome(),
                nullToEmpty(metadata.getComment()), nullToEmpty(metadata.getPurpose()),
                nullToEmpty(metadata.getAuditReason()), formatTrace(metadata.getRequestTraceChain()), formatTrace(metadata.getTracePath()),
                formatTrace(metadata.getMutationLineage()),
                formatRequestTree(metadata),
                nullToEmpty(metadata.getDebugQuery()));
    }

    @Override
    public void writeMutationGovernanceEvent(
            UserContext context, MutationGovernanceEvent event) {
        if (!event.firstOccurrence()) return;
        output.printf(
                "[TeaQL MUTATION GOVERNANCE][%s] request=%s execution=%s policy=%s approval=%s%n",
                event.warningCode(),
                event.snapshot().requestKey(),
                event.snapshot().executionId(),
                event.snapshot().policy() == null ? "none" : event.snapshot().policy().id(),
                event.snapshot().approvalStatus());
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

    protected static String formatRequestTree(ExecutionMetadata metadata) {
        if (metadata.getRequestTree().isEmpty()) return "";
        return RequestTreeJson.render(metadata.getRequestTree());
    }
    private static final class RequestTreeJson {
        static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
        static String render(java.util.List<io.teaql.core.QueryRequestTraceNode> nodes) {
            try { return " requestTree=" + JSON.writeValueAsString(nodes); }
            catch (com.fasterxml.jackson.core.JsonProcessingException unavailable) { return " requestTree=[unavailable]"; }
        }
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
