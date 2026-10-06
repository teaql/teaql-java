package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.UserContext;
import java.io.PrintStream;

/**
 * Explicit value-bearing SQL destination for controlled troubleshooting.
 *
 * <p>Unlike the default sink, this output contains bind values and executable
 * Debug SQL. Deployments must apply access, retention, rotation, and deletion
 * controls appropriate for sensitive application data.</p>
 */
public final class SensitiveDiagnosticTextRuntimeLogSink extends DefaultTextRuntimeLogSink {
    public SensitiveDiagnosticTextRuntimeLogSink() {
        super();
    }

    public SensitiveDiagnosticTextRuntimeLogSink(PrintStream output) {
        super(output);
    }

    @Override
    public boolean requiresSensitiveSqlData() {
        return LogPrivacy.plaintextEnabled();
    }

    @Override
    public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) {
        metadata = LogPrivacy.sql(metadata, LogPrivacy.plaintextEnabled());
        output.printf(
                "[TeaQL SQL][%s][%dus] %s outcome=%s batchOutcome=%s comment=%s purpose=%s auditReason=%s tracePath=%s mutationLineage=%s%n"
                        + "SQL: %s%n",
                metadata.getOperation() == null ? "unknown" : metadata.getOperation().name().toLowerCase(),
                metadata.getElapsedUs(), resultSummary(metadata),
                metadata.getExecutionOutcome() == null ? "unknown" : metadata.getExecutionOutcome(),
                metadata.getBatchOutcome() == null ? "not_applicable" : metadata.getBatchOutcome(),
                nullToEmpty(metadata.getComment()), nullToEmpty(metadata.getPurpose()),
                nullToEmpty(metadata.getAuditReason()), formatTrace(metadata.getTraceChain()),
                formatTrace(metadata.getMutationLineage()),
                nullToEmpty(metadata.getDebugQuery()));
    }
}
