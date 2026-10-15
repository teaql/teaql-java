package io.teaql.runtime;

import io.teaql.core.http.HttpRequestService;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class RuntimeHttpObservationTest {
    @Test public void safeLineageFlowsIntoRuntimeTelemetryOperation() {
        var started = new AtomicReference<RuntimeTelemetry.Operation>();
        var completed = new AtomicReference<Map<String, Object>>();
        RuntimeTelemetry telemetry = operation -> {
            started.set(operation);
            return new RuntimeTelemetry.Scope() {
                @Override public void success(Map<String, Object> attributes) { completed.set(attributes); }
                @Override public void failure(Throwable error) { fail("unexpected failure"); }
            };
        };
        var call = HttpRequestService.Call.get("orders", "orders/1?token=PRIVATE_QUERY_CANARY",
                "load status", "render order");
        var scope = new RuntimeHttpObservation(telemetry).start(call.observation());
        scope.finish("success", 1, 7);
        assertEquals("http", started.get().family());
        assertEquals("orders", started.get().name());
        assertTrue(((String) started.get().attributes().get("teaql.http.trace_chain"))
                .contains("render order"));
        assertFalse(started.get().toString().contains("PRIVATE_QUERY_CANARY"));
        assertEquals("success", completed.get().get("teaql.http.outcome"));
        assertEquals(1, completed.get().get("teaql.http.attempts"));
    }
}
