package io.teaql.core.log;

import io.teaql.core.UserContext;

/**
 * Audit event sink interface. Pluggable implementation.
 * Design aligned with teaql-rs RawAuditEventSink.
 *
 * Implementation examples:
 *   - Database storage
 *   - Message queue
 *   - File writer
 *   - Console output
 */
public interface AuditEventSink {

    void onEvent(UserContext ctx, AuditEvent event);
}
