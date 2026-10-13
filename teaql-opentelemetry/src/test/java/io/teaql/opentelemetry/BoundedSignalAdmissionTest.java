package io.teaql.opentelemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class BoundedSignalAdmissionTest {
    @Test public void actualOwnedFactoryAccountsOverflowAndRecoversThroughHttp() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var workers = Executors.newFixedThreadPool(3);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger received = new AtomicInteger();
        server.setExecutor(workers);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) { exchange.sendResponseHeaders(503, -1); return; }
                received.incrementAndGet(); exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        var losses = new ExportLossAccounting();
        io.teaql.runtime.RuntimeTelemetry telemetry = null;
        try {
            // Same factory assembly as production; only a package-private test
            // capacity differs. No second queue setting is exposed to applications.
            telemetry = new StandardOpenTelemetryFactory().create("owned-overflow-test",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()), losses, 8);
            for (int i = 0; i < 24; i++) telemetry.start(new io.teaql.runtime.RuntimeTelemetry.Operation(
                    "query", "School.list", Map.of())).success();
            assertEquals(16, losses.lost(ExportLossAccounting.Signal.TRACES, ExportLossAccounting.Reason.QUEUE_OVERFLOW));
            assertEquals(16, losses.lost(ExportLossAccounting.Signal.LOGS, ExportLossAccounting.Reason.QUEUE_OVERFLOW));
            release.countDown(); telemetry.flush();
            assertEquals(0, losses.lost(ExportLossAccounting.Signal.TRACES, ExportLossAccounting.Reason.EXPORT_FAILED));
            assertEquals(0, losses.lost(ExportLossAccounting.Signal.LOGS, ExportLossAccounting.Reason.EXPORT_FAILED));
            assertEquals(0, losses.lost(ExportLossAccounting.Signal.METRICS));
            assertTrue(received.get() >= 3);
            telemetry.start(new io.teaql.runtime.RuntimeTelemetry.Operation("query", "School.list", Map.of())).success();
            telemetry.flush();
            assertEquals(16, losses.lost(ExportLossAccounting.Signal.TRACES));
            assertEquals(16, losses.lost(ExportLossAccounting.Signal.LOGS));
        } finally {
            release.countDown(); if (telemetry != null) telemetry.shutdown(); server.stop(0); workers.shutdownNow();
        }
    }

    @Test public void realSdkTraceQueueCountsInFlightAndOverflowThenReusesCapacity() throws Exception {
        var losses = new ExportLossAccounting();
        var admission = new BoundedSignalAdmission(4, losses, ExportLossAccounting.Signal.TRACES);
        var block = new BlockedBatch();
        var delegate = new SpanExporter() {
            public CompletableResultCode export(Collection<SpanData> values) { return block.export(values.size()); }
            public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        };
        var processor = admission.spans(BatchSpanProcessor.builder(losses.spans(delegate, admission))
                .setMaxQueueSize(4).setMaxExportBatchSize(1).setScheduleDelay(Duration.ofMillis(1))
                .setExporterTimeout(Duration.ofSeconds(3)).build());
        try (var provider = SdkTracerProvider.builder().addSpanProcessor(processor).build()) {
            var tracer = provider.get("io.teaql.runtime");
            tracer.spanBuilder("first").startSpan().end();
            assertTrue(block.entered.await(2, TimeUnit.SECONDS));
            for (int i = 0; i < 6; i++) tracer.spanBuilder("next").startSpan().end();
            assertEquals(4, admission.pending());
            assertEquals(3, losses.lost(ExportLossAccounting.Signal.TRACES, ExportLossAccounting.Reason.QUEUE_OVERFLOW));
            assertEquals(0, losses.lost(ExportLossAccounting.Signal.TRACES, ExportLossAccounting.Reason.EXPORT_FAILED));
            block.completion.succeed();
            assertTrue(provider.forceFlush().join(2, TimeUnit.SECONDS).isSuccess());
            assertEquals(0, admission.pending()); assertEquals(4, block.exported.get());
            tracer.spanBuilder("after recovery").startSpan().end();
            assertTrue(provider.forceFlush().join(2, TimeUnit.SECONDS).isSuccess());
            assertEquals(5, block.exported.get()); assertEquals(0, admission.pending());
            assertEquals(3, losses.lost(ExportLossAccounting.Signal.TRACES));
        } finally { block.completion.succeed(); }
    }

    @Test public void realSdkLogQueueCountsInFlightAndOverflowThenReusesCapacity() throws Exception {
        var losses = new ExportLossAccounting();
        var admission = new BoundedSignalAdmission(4, losses, ExportLossAccounting.Signal.LOGS);
        var block = new BlockedBatch();
        var delegate = new LogRecordExporter() {
            public CompletableResultCode export(Collection<LogRecordData> values) { return block.export(values.size()); }
            public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        };
        var processor = admission.logs(BatchLogRecordProcessor.builder(losses.logs(delegate, admission))
                .setMaxQueueSize(4).setMaxExportBatchSize(1).setScheduleDelay(Duration.ofMillis(1))
                .setExporterTimeout(Duration.ofSeconds(3)).build());
        try (var provider = SdkLoggerProvider.builder().addLogRecordProcessor(processor).build()) {
            var logger = provider.get("io.teaql.runtime");
            logger.logRecordBuilder().setBody("first").emit();
            assertTrue(block.entered.await(2, TimeUnit.SECONDS));
            for (int i = 0; i < 6; i++) logger.logRecordBuilder().setBody("next").emit();
            assertEquals(4, admission.pending());
            assertEquals(3, losses.lost(ExportLossAccounting.Signal.LOGS, ExportLossAccounting.Reason.QUEUE_OVERFLOW));
            block.completion.succeed();
            assertTrue(provider.forceFlush().join(2, TimeUnit.SECONDS).isSuccess());
            assertEquals(0, admission.pending()); assertEquals(4, block.exported.get());
            logger.logRecordBuilder().setBody("after recovery").emit();
            assertTrue(provider.forceFlush().join(2, TimeUnit.SECONDS).isSuccess());
            assertEquals(5, block.exported.get()); assertEquals(0, admission.pending());
            assertEquals(3, losses.lost(ExportLossAccounting.Signal.LOGS));
        } finally { block.completion.succeed(); }
    }

    @Test public void concurrentAdmissionNeverExceedsCapacityAndReportsEveryRejectedEntry() throws Exception {
        var losses = new ExportLossAccounting();
        var admission = new BoundedSignalAdmission(8, losses, ExportLossAccounting.Signal.TRACES);
        var pool = Executors.newFixedThreadPool(8);
        try {
            List<java.util.concurrent.Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 32; i++) tasks.add(admission::acquire);
            int accepted = 0;
            for (var result : pool.invokeAll(tasks)) if (result.get()) accepted++;
            assertEquals(8, accepted); assertEquals(8, admission.pending());
            assertEquals(24, losses.lost(ExportLossAccounting.Signal.TRACES, ExportLossAccounting.Reason.QUEUE_OVERFLOW));
            admission.release(8); assertEquals(0, admission.pending());
        } finally { pool.shutdownNow(); }
    }

    @Test public void failedExportReleasesCapacityAndShutdownRejectsWithoutHandoff() {
        var losses = new ExportLossAccounting();
        var admission = new BoundedSignalAdmission(1, losses, ExportLossAccounting.Signal.LOGS);
        assertTrue(admission.acquire());
        var delegate = new LogRecordExporter() {
            public CompletableResultCode export(Collection<LogRecordData> values) { throw new IllegalStateException("PRIVATE_EXPORT_CANARY"); }
            public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        };
        assertFalse(losses.logs(delegate, admission).export(java.util.Arrays.asList((LogRecordData) null)).isSuccess());
        assertEquals(0, admission.pending());
        assertEquals(1, losses.lost(ExportLossAccounting.Signal.LOGS, ExportLossAccounting.Reason.EXPORT_FAILED));
        AtomicInteger handedOff = new AtomicInteger();
        var processor = admission.logs(new LogRecordProcessor() {
            public void onEmit(Context context, ReadWriteLogRecord record) { handedOff.incrementAndGet(); }
        });
        processor.shutdown(); processor.onEmit(Context.root(), null);
        assertEquals(0, handedOff.get()); assertEquals(0, admission.pending());
        assertEquals(1, losses.lost(ExportLossAccounting.Signal.LOGS, ExportLossAccounting.Reason.SHUTDOWN));
    }

    private static final class BlockedBatch {
        final CountDownLatch entered = new CountDownLatch(1);
        final CompletableResultCode completion = new CompletableResultCode();
        final AtomicBoolean first = new AtomicBoolean(true);
        final AtomicInteger exported = new AtomicInteger();
        CompletableResultCode export(int size) {
            exported.addAndGet(size);
            if (first.compareAndSet(true, false)) { entered.countDown(); return completion; }
            return CompletableResultCode.ofSuccess();
        }
    }
}
