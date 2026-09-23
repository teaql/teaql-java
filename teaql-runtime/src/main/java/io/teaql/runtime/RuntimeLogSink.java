package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.UserContext;

public interface RuntimeLogSink {
    void writeExecutionLog(UserContext context, ExecutionMetadata metadata);

    /**
     * Whether the provider should construct bind-value payloads and rendered
     * diagnostic SQL. Sinks must explicitly opt in to receiving value-bearing
     * metadata; ordinary custom sinks receive only parameterized SQL.
     */
    default boolean requiresSensitiveSqlData() {
        return false;
    }

    default void writeAuditEvent(UserContext context, RawAuditEvent event) {}
}
