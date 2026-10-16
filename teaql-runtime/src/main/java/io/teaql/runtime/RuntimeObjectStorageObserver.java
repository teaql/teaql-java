package io.teaql.runtime;

import io.teaql.core.UserContext;
import io.teaql.core.storage.ObjectStorageObserver;
import java.util.Map;

/** Bridges the core storage lifecycle to the runtime's normal telemetry path. */
final class RuntimeObjectStorageObserver implements ObjectStorageObserver {
    private final RuntimeTelemetry telemetry;
    RuntimeObjectStorageObserver(RuntimeTelemetry telemetry) { this.telemetry = telemetry; }
    @Override public Scope start(UserContext context, String operation, String comment) {
        Scope log = ObjectStorageObserver.LOGGING.start(context, operation, comment);
        var scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("object_storage", operation, Map.of()));
        scope.detach(); // A download response may outlive its initiating call.
        return outcome -> {
            log.finish(outcome);
            if (outcome.equals("success")) scope.success();
            else scope.failure(new IllegalStateException("object_storage_" + outcome));
        };
    }
}
