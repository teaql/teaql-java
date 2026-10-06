package io.teaql.core.sql.portable;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.SqlParameterLogPolicy;
import java.util.List;

/** Immutable compiler-owned parameter provenance, separate from execution values. */
public record SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated, String diagnosticSql,
        io.teaql.core.SqlIntentRedactions intentRedactions, io.teaql.core.SqlExecutionTrace executionTrace,
        List<io.teaql.core.SqlExecutionTrace> batchTraces) {
    public static final SqlLogBindings UNKNOWN = new SqlLogBindings(List.of(), false);

    public SqlLogBindings { policies = List.copyOf(policies); batchTraces = List.copyOf(batchTraces); }
    public SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated) { this(policies, generated, null); }
    public SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated, String diagnosticSql) {
        this(policies, generated, diagnosticSql, null);
    }
    public SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated, String diagnosticSql,
            io.teaql.core.SqlIntentRedactions intentRedactions) {
        this(policies, generated, diagnosticSql, intentRedactions, null);
    }
    public SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated, String diagnosticSql,
            io.teaql.core.SqlIntentRedactions intentRedactions, io.teaql.core.SqlExecutionTrace executionTrace) {
        this(policies, generated, diagnosticSql, intentRedactions, executionTrace, List.of());
    }
    public SqlLogBindings withTrace(io.teaql.core.SqlExecutionTrace trace) {
        return new SqlLogBindings(policies, generated, diagnosticSql, intentRedactions, trace);
    }

    public SqlLogBindings withBatchTraces(List<io.teaql.core.SqlExecutionTrace> traces) {
        return new SqlLogBindings(policies, generated, diagnosticSql, intentRedactions, null, traces);
    }

    public void validateBatchSize(int rowCount) {
        if (!batchTraces.isEmpty() && batchTraces.size() != rowCount) {
            throw new IllegalArgumentException("SQL batch trace count must match physical row count");
        }
    }

    public SqlLogBindings forBatchRow(int index) {
        return batchTraces.isEmpty() ? this
                : new SqlLogBindings(policies, generated, diagnosticSql, intentRedactions, batchTraces.get(index));
    }

    public boolean collectsStatements() {
        return executionTrace != null && executionTrace.statementObserver() != null
                || batchTraces.stream().anyMatch(trace -> trace.statementObserver() != null);
    }

    public void recordStatement(ExecutionMetadata metadata) {
        if (executionTrace != null) executionTrace.recordStatement(metadata);
    }

    public void applyTo(ExecutionMetadata metadata) {
        metadata.setParameterLogPolicies(policies);
        metadata.setGeneratedSql(generated);
        metadata.setIntentRedactions(intentRedactions);
        if (diagnosticSql != null) metadata.setParameterizedQuery(diagnosticSql);
        if (executionTrace != null) executionTrace.applyTo(metadata);
    }
}
