package io.teaql.core.sql.portable;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.SqlParameterLogPolicy;
import java.util.List;

/** Immutable compiler-owned parameter provenance, separate from execution values. */
public record SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated, String diagnosticSql,
        io.teaql.core.SqlIntentRedactions intentRedactions, io.teaql.core.SqlExecutionTrace executionTrace) {
    public static final SqlLogBindings UNKNOWN = new SqlLogBindings(List.of(), false);

    public SqlLogBindings { policies = List.copyOf(policies); }
    public SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated) { this(policies, generated, null); }
    public SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated, String diagnosticSql) {
        this(policies, generated, diagnosticSql, null);
    }
    public SqlLogBindings(List<SqlParameterLogPolicy> policies, boolean generated, String diagnosticSql,
            io.teaql.core.SqlIntentRedactions intentRedactions) {
        this(policies, generated, diagnosticSql, intentRedactions, null);
    }
    public SqlLogBindings withTrace(io.teaql.core.SqlExecutionTrace trace) {
        return new SqlLogBindings(policies, generated, diagnosticSql, intentRedactions, trace);
    }

    public void applyTo(ExecutionMetadata metadata) {
        metadata.setParameterLogPolicies(policies);
        metadata.setGeneratedSql(generated);
        metadata.setIntentRedactions(intentRedactions);
        if (diagnosticSql != null) metadata.setParameterizedQuery(diagnosticSql);
        if (executionTrace != null) executionTrace.applyTo(metadata);
    }
}
