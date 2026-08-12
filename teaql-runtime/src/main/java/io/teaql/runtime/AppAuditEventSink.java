package io.teaql.runtime;

import io.teaql.core.UserContext;

@FunctionalInterface
public interface AppAuditEventSink {
    void onAuditEvent(UserContext context, SafeAuditEvent event);
}
