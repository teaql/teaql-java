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

    /** Structured bind values for trusted runtime sinks. Application-safe projections should expose only the count. */
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

    private long elapsedUs;
    private String resultSummary;

    public long getElapsedUs() { return elapsedUs; }
    public void setElapsedUs(long elapsedUs) { this.elapsedUs = elapsedUs; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
}
