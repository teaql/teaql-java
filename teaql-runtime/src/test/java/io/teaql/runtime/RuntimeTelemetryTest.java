package io.teaql.runtime;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class RuntimeTelemetryTest {
    @Test
    public void recordsBalancedSafeLifecycle() {
        List<String> phases = new ArrayList<>();
        List<RuntimeTelemetry.Operation> operations = new ArrayList<>();
        RuntimeTelemetry telemetry = operation -> {
            phases.add("start");
            operations.add(operation);
            return new RuntimeTelemetry.Scope() {
                @Override
                public void success(Map<String, Object> attributes) {
                    phases.add("success");
                }

                @Override
                public void failure(Throwable error) {
                    phases.add("failure");
                }
            };
        };

        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(telemetry,
                new RuntimeTelemetry.Operation("query", "School.list", Map.of(
                        "teaql.entity.type", "School",
                        "teaql.entity.id", 42L)));
        scope.success(Map.of("teaql.result.cardinality", 1));
        scope.failure(new IllegalStateException("late"));

        assertEquals(List.of("start", "success"), phases);
        assertEquals("School", operations.get(0).attributes().get("teaql.entity.type"));
        assertFalse(operations.get(0).attributes().containsKey("teaql.entity.id"));
    }

    @Test
    public void adapterFailuresAreFailOpen() {
        RuntimeTelemetry brokenStart = operation -> {
            throw new IllegalStateException("adapter failed");
        };
        assertSame(RuntimeTelemetry.NoopScope.INSTANCE, RuntimeTelemetry.startSafely(
                brokenStart, new RuntimeTelemetry.Operation("cache", "get", Map.of())));

        RuntimeTelemetry brokenFinish = operation -> new RuntimeTelemetry.Scope() {
            @Override
            public void success(Map<String, Object> attributes) {
                throw new IllegalStateException("export failed");
            }

            @Override
            public void failure(Throwable error) {
                throw new IllegalStateException("export failed");
            }
        };
        RuntimeTelemetry.Scope scope = RuntimeTelemetry.startSafely(brokenFinish,
                new RuntimeTelemetry.Operation("mutation", "School.create", Map.of()));
        scope.success();
        scope.failure(new RuntimeException("ignored after completion"));
    }
}
