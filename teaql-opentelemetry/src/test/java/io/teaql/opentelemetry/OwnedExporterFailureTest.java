package io.teaql.opentelemetry;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.common.export.MessageWriter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.teaql.runtime.RuntimeTelemetry;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.junit.Test;
import static org.junit.Assert.*;

public class OwnedExporterFailureTest {
    private static final String SECRET = "EXPORTER_CANARY_8b013d1b_password";

    @Test public void privateTransportNeverReadsResponseBodyOrReasonAndPreservesStatus() {
        AtomicInteger callbacks = new AtomicInteger();
        var sender = OwnedHttpComponents.protect(sender((response, error) -> {
            response.accept(new HttpResponse() {
                public int getStatusCode() { return 503; }
                public String getStatusMessage() { throw new AssertionError(SECRET); }
                public byte[] getResponseBody() { throw new AssertionError(SECRET); }
            });
            error.accept(new IllegalStateException(SECRET)); // Duplicate terminal callback is ignored.
        }));
        sender.send(null, response -> {
            callbacks.incrementAndGet(); assertEquals(503, response.getStatusCode());
            assertEquals(0, response.getResponseBody().length);
            assertFalse(response.getStatusMessage().contains(SECRET));
        }, error -> fail("already completed"));
        assertEquals(1, callbacks.get());
    }

    @Test public void privateTransportDropsExceptionMessageCauseSuppressedAndStack() {
        var original = new IllegalStateException(SECRET, new IllegalArgumentException(SECRET));
        original.addSuppressed(new RuntimeException(SECRET));
        AtomicReference<Throwable> failure = new AtomicReference<>();
        OwnedHttpComponents.protect(sender((response, error) -> error.accept(original)))
                .send(null, response -> fail("unexpected response"), failure::set);
        assertNotNull(failure.get()); assertNull(failure.get().getCause());
        assertEquals(0, failure.get().getSuppressed().length); assertEquals(0, failure.get().getStackTrace().length);
        assertFalse(failure.get().getMessage().contains(SECRET));
    }

    @Test public void failedBatchesCountExactlyOnceAndSuccessfulExportDoesNotIncreaseLoss() {
        var losses = new ExportLossAccounting();
        CompletableResultCode completion = new CompletableResultCode();
        SpanExporter exporter = losses.spans(new SpanExporter() {
            public CompletableResultCode export(Collection<io.opentelemetry.sdk.trace.data.SpanData> values) { return completion; }
            public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        });
        // Null fixture entries are not marshaled; only batch cardinality matters here.
        exporter.export(java.util.Arrays.asList(null, null, null));
        completion.fail(); completion.fail();
        assertEquals(3, losses.lost(ExportLossAccounting.Signal.TRACES));
        losses.spans(SpanExporter.noop()).export(List.of());
        assertEquals(3, losses.lost(ExportLossAccounting.Signal.TRACES));
        var reader = InMemoryMetricReader.create();
        try (var metrics = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            losses.bind(metrics.get("io.teaql.runtime"));
            var counter = reader.collectAllMetrics().stream().filter(value -> value.getName().equals("teaql.telemetry.export.lost")).findFirst().orElseThrow();
            assertEquals(3, counter.getLongSumData().getPoints().stream().mapToLong(point -> point.getValue()).sum());
            assertFalse(counter.toString().contains(SECRET));
        }
    }

    @Test public void realThreeSignalFailuresAreMaskedCountedAndDoNotReplaceCustomerSdk() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/", exchange -> {
            requests.incrementAndGet(); exchange.getRequestBody().readAllBytes();
            byte[] body = SECRET.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        StringWriter output = new StringWriter();
        Handler handler = new Handler() {
            public synchronized void publish(LogRecord record) {
                output.append(record.getMessage()).append('\n');
                if (record.getThrown() != null) record.getThrown().printStackTrace(new PrintWriter(output));
            }
            public void flush() {} public void close() {}
        };
        var root = java.util.logging.Logger.getLogger("");
        var handlers = root.getHandlers(); var level = root.getLevel(); var global = GlobalOpenTelemetry.get();
        root.addHandler(handler); // Observe only; never suppress/change customer log configuration.
        var losses = new ExportLossAccounting();
        RuntimeTelemetry telemetry = null;
        try {
            telemetry = new StandardOpenTelemetryFactory().create("safe-failure-test",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()), losses);
            var operation = telemetry.start(new RuntimeTelemetry.Operation("query", "School.list", Map.of()));
            operation.success(Map.of("teaql.result.cardinality", 1));
            telemetry.flush();
            assertTrue(losses.lost(ExportLossAccounting.Signal.TRACES) > 0);
            assertTrue(losses.lost(ExportLossAccounting.Signal.LOGS) > 0);
            assertTrue(losses.lost(ExportLossAccounting.Signal.METRICS) > 0);
            assertEquals("one request per signal, no implicit exporter retries", 3, requests.get());
            assertSame(global, GlobalOpenTelemetry.get());
        } finally {
            if (telemetry != null) { telemetry.shutdown(); telemetry.shutdown(); }
            root.removeHandler(handler); server.stop(0);
        }
        assertFalse(output.toString(), output.toString().contains(SECRET));
        assertTrue(output.toString().contains("remote response masked"));
        assertTrue(output.toString().contains("export entries lost"));
        assertArrayEquals(handlers, root.getHandlers()); assertEquals(level, root.getLevel());
    }

    private static HttpSender sender(java.util.function.BiConsumer<Consumer<HttpResponse>, Consumer<Throwable>> complete) {
        return new HttpSender() {
            public void send(MessageWriter message, Consumer<HttpResponse> response, Consumer<Throwable> error) { complete.accept(response, error); }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        };
    }
}
