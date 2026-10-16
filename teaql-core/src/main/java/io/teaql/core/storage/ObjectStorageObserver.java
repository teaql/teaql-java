package io.teaql.core.storage;

import io.teaql.core.UserContext;
import java.util.concurrent.atomic.AtomicBoolean;

/** Receives safe operation metadata only, never Key, body, custom metadata or URL. */
public interface ObjectStorageObserver {
    Scope start(UserContext context, String operation, String comment);
    interface Scope { void finish(String outcome); }

    ObjectStorageObserver LOGGING = (context, operation, comment) -> {
        long started = System.nanoTime();
        AtomicBoolean finished = new AtomicBoolean();
        // Capture the request's lineage before a returned stream outlives the call.
        var trace = java.util.List.copyOf(context.getTraceChain());
        String safeComment = comment.replaceAll("[\\p{Cntrl}]", " ");
        return outcome -> {
            if (finished.compareAndSet(false, true) && context.isExecutionLoggingEnabled()) {
                System.getLogger("io.teaql.object-storage").log(System.Logger.Level.INFO,
                        "ObjectStorage operation={0} comment={1} outcome={2} elapsedMs={3} traceDepth={4}",
                        operation, safeComment, outcome, (System.nanoTime() - started) / 1_000_000, trace.size());
            }
        };
    };
}
