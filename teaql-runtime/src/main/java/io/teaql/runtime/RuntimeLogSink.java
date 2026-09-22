package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.UserContext;

public interface RuntimeLogSink {
    void writeExecutionLog(UserContext context, ExecutionMetadata metadata);

    /**
     * Whether the provider should construct bind-value payloads and rendered
     * diagnostic SQL. Existing custom sinks retain these fields by default.
     */
    default boolean requiresSensitiveSqlData() {
        return true;
    }

    default void writeAuditEvent(UserContext context, RawAuditEvent event) {}
}
