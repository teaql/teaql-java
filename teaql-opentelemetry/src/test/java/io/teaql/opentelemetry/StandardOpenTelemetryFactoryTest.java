package io.teaql.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.teaql.runtime.RuntimeTelemetry;
import java.net.URI;
import java.net.ServerSocket;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class StandardOpenTelemetryFactoryTest {
    @Test public void ownedFactoryRecordsAllAndDoesNotReplaceGlobalSdk() throws Exception {
        var global = GlobalOpenTelemetry.get();
        int closedPort;
        try (var socket = new ServerSocket(0)) { closedPort = socket.getLocalPort(); }
        RuntimeTelemetry telemetry = new StandardOpenTelemetryFactory().create("standard-factory-test",
                URI.create("http://127.0.0.1:" + closedPort));
        long start = System.nanoTime();
        try {
            RuntimeTelemetry.Scope scope = telemetry.start(new RuntimeTelemetry.Operation("query", "School.list",
                    Map.of("teaql.entity.type", "School")));
            assertTrue(Span.current().getSpanContext().isValid());
            assertTrue(Span.current().getSpanContext().isSampled());
            scope.success(Map.of("teaql.result.cardinality", 1));
            assertSame(global, GlobalOpenTelemetry.get());
            telemetry.flush(); // Unavailable destination must not become a business exception.
        } finally { telemetry.shutdown(); telemetry.shutdown(); }
        assertSame(global, GlobalOpenTelemetry.get());
        assertTrue("Owned lifecycle must be bounded", System.nanoTime() - start < 20_000_000_000L);
    }
}
