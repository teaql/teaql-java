package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.UserContext;

public interface RuntimeLogSink {
    void writeExecutionLog(UserContext context, ExecutionMetadata metadata);

    default void writeAuditEvent(UserContext context, RawAuditEvent event) {}
}
