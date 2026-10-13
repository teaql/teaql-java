package io.teaql.opentelemetry;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsPartialSuccess;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsPartialSuccess;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.ExportTracePartialSuccess;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.common.export.MessageWriter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.teaql.runtime.RuntimeTelemetry;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.junit.Test;
import static org.junit.Assert.*;

public class OtlpResponseAccountingTest {
    private static final String SECRET = "PARTIAL_RESPONSE_CANARY_9243a11_password";

    @Test public void officialSignalRepliesPreserveLongCountsAndPointUnits() {
        var response = new OtlpResponseAccounting();
        assertTrue(response.receive(ExportLossAccounting.Signal.TRACES, reply(ExportLossAccounting.Signal.TRACES, 3_000_000_000L)));
        assertTrue(response.receive(ExportLossAccounting.Signal.LOGS, reply(ExportLossAccounting.Signal.LOGS, 2)));
        assertTrue(response.receive(ExportLossAccounting.Signal.METRICS, reply(ExportLossAccounting.Signal.METRICS, 5)));
        assertEquals(3_000_000_000L, response.rejected(ExportLossAccounting.Signal.TRACES));
        assertEquals(2, response.rejected(ExportLossAccounting.Signal.LOGS));
        assertEquals(5, response.rejected(ExportLossAccounting.Signal.METRICS));
        var reader = InMemoryMetricReader.create();
        try (var metrics = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            response.bind(metrics.get("io.teaql.runtime"));
            var counter = reader.collectAllMetrics().stream().filter(value -> value.getName().equals("teaql.telemetry.export.rejected")).findFirst().orElseThrow();
            var point = counter.getLongSumData().getPoints().stream().filter(value -> "metrics".equals(
                    value.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("teaql.telemetry.signal")))).findFirst().orElseThrow();
            assertEquals(5, point.getValue());
            assertEquals("data_point", point.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("teaql.telemetry.rejected.unit")));
            assertFalse(counter.toString().contains(SECRET));
        }
    }

    @Test public void warningHasNoRejectionAndEmptyReplyIsFullSuccess() {
        var response = new OtlpResponseAccounting();
        assertTrue(response.receive(ExportLossAccounting.Signal.TRACES, new byte[0]));
        assertTrue(response.receive(ExportLossAccounting.Signal.TRACES, null));
        assertTrue(response.receive(ExportLossAccounting.Signal.TRACES,
                ExportTraceServiceResponse.newBuilder().setPartialSuccess(ExportTracePartialSuccess.getDefaultInstance()).build().toByteArray()));
        assertTrue(response.receive(ExportLossAccounting.Signal.LOGS,
                ExportLogsServiceResponse.newBuilder().setPartialSuccess(ExportLogsPartialSuccess.getDefaultInstance()).build().toByteArray()));
        assertTrue(response.receive(ExportLossAccounting.Signal.METRICS,
                ExportMetricsServiceResponse.newBuilder().setPartialSuccess(ExportMetricsPartialSuccess.getDefaultInstance()).build().toByteArray()));
        assertEquals(0, response.responses(ExportLossAccounting.Signal.TRACES, OtlpResponseAccounting.State.WARNING));
        assertTrue(response.receive(ExportLossAccounting.Signal.TRACES, reply(ExportLossAccounting.Signal.TRACES, 0)));
        assertEquals(0, response.rejected(ExportLossAccounting.Signal.TRACES));
        assertEquals(1, response.responses(ExportLossAccounting.Signal.TRACES, OtlpResponseAccounting.State.WARNING));
    }

    @Test public void malformedNegativeOversizedAndDeepRepliesAreRejectedWithoutTextFallback() {
        var response = new OtlpResponseAccounting();
        assertFalse(response.receive(ExportLossAccounting.Signal.LOGS, new byte[]{10, 20, 8}));
        assertFalse(response.receive(ExportLossAccounting.Signal.LOGS, reply(ExportLossAccounting.Signal.LOGS, -1)));
        assertFalse(response.receive(ExportLossAccounting.Signal.LOGS, new byte[OtlpResponseAccounting.MAX_RESPONSE_BYTES + 1]));
        byte[] nestedGroups = new byte[64];
        java.util.Arrays.fill(nestedGroups, 0, 32, (byte) 11); java.util.Arrays.fill(nestedGroups, 32, 64, (byte) 12);
        assertFalse(response.receive(ExportLossAccounting.Signal.LOGS, nestedGroups));
        assertEquals(4, response.responses(ExportLossAccounting.Signal.LOGS, OtlpResponseAccounting.State.INVALID_RESPONSE));
        assertEquals(0, response.rejected(ExportLossAccounting.Signal.LOGS));
    }

    @Test public void privateSenderAccountsOnePartialReplyDespiteDuplicateCallbacks() {
        var response = new OtlpResponseAccounting(); AtomicInteger callbacks = new AtomicInteger();
        HttpSender nativeSender = new HttpSender() {
            public void send(MessageWriter message, Consumer<HttpResponse> next, Consumer<Throwable> error) {
                var value = http(200, reply(ExportLossAccounting.Signal.TRACES, 1)); next.accept(value); next.accept(value);
                error.accept(new IllegalStateException(SECRET));
            }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        };
        OwnedHttpComponents.protect(nativeSender, response, ExportLossAccounting.Signal.TRACES)
                .send(null, safe -> {
                    callbacks.incrementAndGet(); assertEquals(200, safe.getStatusCode());
                    assertEquals(0, safe.getResponseBody().length); assertFalse(safe.getStatusMessage().contains(SECRET));
                }, error -> fail("valid partial response is not a transport error"));
        assertEquals(1, callbacks.get()); assertEquals(1, response.rejected(ExportLossAccounting.Signal.TRACES));
    }

    @Test public void invalidAcknowledgementCompletesOnceAsSafeFailureAndReleasesAdmission() {
        var losses = new ExportLossAccounting();
        var admission = new BoundedSignalAdmission(1, losses, ExportLossAccounting.Signal.TRACES);
        assertTrue(admission.acquire());
        var result = new CompletableResultCode();
        HttpSender nativeSender = new HttpSender() {
            public void send(MessageWriter message, Consumer<HttpResponse> next, Consumer<Throwable> error) {
                var invalid = http(200, SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                next.accept(invalid); next.accept(invalid);
            }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        };
        var sender = OwnedHttpComponents.protect(nativeSender, losses.responses(), ExportLossAccounting.Signal.TRACES);
        var exporter = losses.spans(new io.opentelemetry.sdk.trace.export.SpanExporter() {
            public CompletableResultCode export(java.util.Collection<io.opentelemetry.sdk.trace.data.SpanData> values) {
                sender.send(null, response -> fail("malformed response cannot be full success"), failure -> {
                    assertFalse(failure.getMessage().contains(SECRET)); assertNull(failure.getCause()); result.fail();
                });
                return result;
            }
            public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        }, admission);
        assertFalse(exporter.export(java.util.Arrays.asList((io.opentelemetry.sdk.trace.data.SpanData) null)).isSuccess());
        assertEquals(0, admission.pending());
        assertEquals(1, losses.lost(ExportLossAccounting.Signal.TRACES, ExportLossAccounting.Reason.EXPORT_FAILED));
        assertEquals(1, losses.responses().responses(ExportLossAccounting.Signal.TRACES, OtlpResponseAccounting.State.INVALID_RESPONSE));
    }

    @Test public void ownedSenderConfigurationCapsResponsesWithoutChangingSourceConfiguration() {
        var source = new io.opentelemetry.sdk.common.export.HttpSenderConfig() {
            public URI getEndpoint() { return URI.create("http://127.0.0.1:4318/v1/traces"); }
            public String getContentType() { return "application/x-protobuf"; }
            public io.opentelemetry.sdk.common.export.Compressor getCompressor() { return null; }
            public java.time.Duration getTimeout() { return java.time.Duration.ofSeconds(3); }
            public java.time.Duration getConnectTimeout() { return getTimeout(); }
            public java.util.function.Supplier<Map<String, java.util.List<String>>> getHeadersSupplier() { return Map::of; }
            public io.opentelemetry.sdk.common.export.ProxyOptions getProxyOptions() { return null; }
            public io.opentelemetry.sdk.common.export.RetryPolicy getRetryPolicy() { return null; }
            public javax.net.ssl.SSLContext getSslContext() { return null; }
            public javax.net.ssl.X509TrustManager getTrustManager() { return null; }
            public java.util.concurrent.ExecutorService getExecutorService() { return null; }
            public long getMaxResponseBodySize() { return 8 * 1024 * 1024; }
        };
        var bound = OwnedHttpComponents.bounded(source);
        assertEquals(OtlpResponseAccounting.MAX_RESPONSE_BYTES, bound.getMaxResponseBodySize());
        assertEquals(8 * 1024 * 1024, source.getMaxResponseBodySize());
        assertEquals(source.getEndpoint(), bound.getEndpoint()); assertEquals(source.getTimeout(), bound.getTimeout());
        assertNull(bound.getRetryPolicy());
    }

    @Test public void actualFactoryPartialResponsesDoNotRetryOrTurnIntoWholeBatchFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/", exchange -> {
            requests.incrementAndGet(); exchange.getRequestBody().readAllBytes();
            var signal = exchange.getRequestURI().getPath().endsWith("traces") ? ExportLossAccounting.Signal.TRACES
                    : exchange.getRequestURI().getPath().endsWith("logs") ? ExportLossAccounting.Signal.LOGS : ExportLossAccounting.Signal.METRICS;
            byte[] body = reply(signal, 1); exchange.getResponseHeaders().add("Content-Type", "application/x-protobuf");
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start(); StringWriter output = new StringWriter();
        var root = java.util.logging.Logger.getLogger("");
        Handler handler = new Handler() {
            public synchronized void publish(LogRecord record) {
                output.append(record.getMessage()).append('\n');
                if (record.getThrown() != null) record.getThrown().printStackTrace(new PrintWriter(output));
            }
            public void flush() {} public void close() {}
        };
        root.addHandler(handler); RuntimeTelemetry telemetry = null; var losses = new ExportLossAccounting();
        try {
            telemetry = new StandardOpenTelemetryFactory().create("partial-response-test",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()), losses);
            telemetry.start(new RuntimeTelemetry.Operation("query", "School.list", Map.of())).success();
            telemetry.flush();
            assertEquals(3, requests.get());
            for (var signal : ExportLossAccounting.Signal.values()) {
                assertEquals(1, losses.responses().rejected(signal));
                assertEquals(1, losses.responses().responses(signal, OtlpResponseAccounting.State.PARTIAL_SUCCESS));
                assertEquals(0, losses.lost(signal));
            }
        } finally {
            if (telemetry != null) telemetry.shutdown(); root.removeHandler(handler); server.stop(0);
        }
        assertFalse(output.toString(), output.toString().contains(SECRET));
        assertTrue(output.toString().contains("state=partial_success"));
    }

    private static HttpResponse http(int status, byte[] body) {
        return new HttpResponse() {
            public int getStatusCode() { return status; }
            public String getStatusMessage() { throw new AssertionError("remote reason must remain unread"); }
            public byte[] getResponseBody() { return body; }
        };
    }

    private static byte[] reply(ExportLossAccounting.Signal signal, long count) {
        return switch (signal) {
            case TRACES -> ExportTraceServiceResponse.newBuilder().setPartialSuccess(
                    ExportTracePartialSuccess.newBuilder().setRejectedSpans(count).setErrorMessage(SECRET)).build().toByteArray();
            case LOGS -> ExportLogsServiceResponse.newBuilder().setPartialSuccess(
                    ExportLogsPartialSuccess.newBuilder().setRejectedLogRecords(count).setErrorMessage(SECRET)).build().toByteArray();
            case METRICS -> ExportMetricsServiceResponse.newBuilder().setPartialSuccess(
                    ExportMetricsPartialSuccess.newBuilder().setRejectedDataPoints(count).setErrorMessage(SECRET)).build().toByteArray();
        };
    }
}
