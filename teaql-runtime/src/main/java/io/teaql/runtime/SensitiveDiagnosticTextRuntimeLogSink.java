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
        return true;
    }

    @Override
    public void writeExecutionLog(UserContext context, ExecutionMetadata metadata) {
        output.printf(
                "[TeaQL SQL][%s][%dus] %s comment=%s purpose=%s auditReason=%s tracePath=%s%n"
                        + "Parameterized SQL: %s params=%s%nDebug SQL: %s%n",
                metadata.getOperation() == null ? "unknown" : metadata.getOperation().name().toLowerCase(),
                metadata.getElapsedUs(), resultSummary(metadata),
                nullToEmpty(metadata.getComment()), nullToEmpty(metadata.getPurpose()),
                nullToEmpty(metadata.getAuditReason()), formatTrace(metadata.getTraceChain()),
                nullToEmpty(metadata.getParameterizedQuery()), metadata.getParameters(),
                nullToEmpty(metadata.getDebugQuery()));
    }
}
