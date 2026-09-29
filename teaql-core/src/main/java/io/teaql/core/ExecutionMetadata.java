package io.teaql.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExecutionMetadata {
    private String backend;
    private DataServiceOperation operation;
    private Instant startedAt;
    private Instant endedAt;
    private Long affectedRows;
    private Integer resultCount;
    private String backendRequestId;
    private String parameterizedQuery;
    private List<Object> parameters = List.of();
    private String debugQuery;
    private List<TraceNode> traceChain;
    private String comment;
    private String purpose;
    private String auditReason;
    private List<SqlParameterLogPolicy> parameterLogPolicies = List.of();
    private List<Boolean> parameterMasked = List.of();
    private boolean generatedSql;
    private String logMode;
    private String sqlOmissionReason;
    private String executionOutcome;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private transient SqlIntentRedactions intentRedactions;

    @FrameworkInternal("SQL diagnostic provenance; excluded from serialized metadata")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public SqlIntentRedactions getIntentRedactions() { return intentRedactions; }
    @FrameworkInternal("SQL diagnostic provenance only")
    public void setIntentRedactions(SqlIntentRedactions value) { intentRedactions = value; }

    /** Statement/cursor outcome, not transaction commit or business acceptance. Null means unknown. */
    public String getExecutionOutcome() { return executionOutcome; }
    public void setExecutionOutcome(String outcome) { executionOutcome = outcome; }

    public List<SqlParameterLogPolicy> getParameterLogPolicies() { return parameterLogPolicies; }
    public void setParameterLogPolicies(List<SqlParameterLogPolicy> policies) {
        parameterLogPolicies = policies == null ? List.of() : List.copyOf(policies);
    }
    public List<Boolean> getParameterMasked() { return parameterMasked; }
    public void setParameterMasked(List<Boolean> masked) {
        parameterMasked = masked == null ? List.of() : List.copyOf(masked);
    }
    /** Set only by a compiler that can attest that inline text contains no user values. */
    public boolean isGeneratedSql() { return generatedSql; }
    public void setGeneratedSql(boolean generated) { generatedSql = generated; }
    public String getLogMode() { return logMode; }
    public void setLogMode(String mode) { logMode = mode; }
    public String getSqlOmissionReason() { return sqlOmissionReason; }
    public void setSqlOmissionReason(String reason) { sqlOmissionReason = reason; }

    public String getBackend() { return backend; }
    public void setBackend(String backend) { this.backend = backend; }

    public DataServiceOperation getOperation() { return operation; }
    public void setOperation(DataServiceOperation operation) { this.operation = operation; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public Long getAffectedRows() { return affectedRows; }
    public void setAffectedRows(Long affectedRows) { this.affectedRows = affectedRows; }

    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

    public String getBackendRequestId() { return backendRequestId; }
    public void setBackendRequestId(String backendRequestId) { this.backendRequestId = backendRequestId; }

    /** Provider-native request text with placeholders, never interpolated bind values. */
    public String getParameterizedQuery() { return parameterizedQuery; }
    public void setParameterizedQuery(String parameterizedQuery) { this.parameterizedQuery = parameterizedQuery; }

    /** Execution bindings before projection; only independently projected values may reach log sinks. */
    public List<Object> getParameters() { return parameters; }
    public void setParameters(List<Object> parameters) {
        this.parameters = parameters == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(parameters));
    }
    public int getParameterCount() { return parameters.size(); }

    public String getDebugQuery() { return debugQuery; }
    public void setDebugQuery(String debugQuery) { this.debugQuery = debugQuery; }

    public List<TraceNode> getTraceChain() { return traceChain; }
    public void setTraceChain(List<TraceNode> traceChain) { this.traceChain = traceChain; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getAuditReason() { return auditReason; }
    public void setAuditReason(String auditReason) { this.auditReason = auditReason; }

    private long elapsedUs;
    private String resultSummary;

    public long getElapsedUs() { return elapsedUs; }
    public void setElapsedUs(long elapsedUs) { this.elapsedUs = elapsedUs; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
}
