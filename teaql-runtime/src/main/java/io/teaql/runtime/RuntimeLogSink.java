package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.UserContext;

public interface RuntimeLogSink {
    void writeExecutionLog(UserContext ctx, ExecutionMetadata metadata);

    default void writeAuditEvent(UserContext ctx, RawAuditEvent event) {}
}
