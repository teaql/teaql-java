package io.teaql.opentelemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounds queued plus exporting records before the SDK's equal-size queue. */
final class BoundedSignalAdmission {
    private final int capacity;
    private final ExportLossAccounting losses;
    private final ExportLossAccounting.Signal signal;
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();

    BoundedSignalAdmission(int capacity, ExportLossAccounting losses, ExportLossAccounting.Signal signal) {
        if (capacity < 1 || signal == ExportLossAccounting.Signal.METRICS) throw new IllegalArgumentException("Invalid event queue");
        this.capacity = capacity; this.losses = losses; this.signal = signal;
    }

    int pending() { return pending.get(); }

    boolean acquire() {
        while (!closed.get()) {
            int current = pending.get();
            if (current >= capacity) {
                losses.lost(signal, ExportLossAccounting.Reason.QUEUE_OVERFLOW, 1);
                return false;
            }
            if (pending.compareAndSet(current, current + 1)) return true;
        }
        losses.lost(signal, ExportLossAccounting.Reason.SHUTDOWN, 1);
        return false;
    }

    void release(int size) { pending.addAndGet(-size); }

    private void rejected() {
        release(1);
        losses.lost(signal, ExportLossAccounting.Reason.EXPORT_FAILED, 1);
    }

    SpanProcessor spans(SpanProcessor delegate) {
        return new SpanProcessor() {
            @Override public void onStart(Context parent, ReadWriteSpan span) { delegate.onStart(parent, span); }
            @Override public boolean isStartRequired() { return delegate.isStartRequired(); }
            @Override public boolean isEndRequired() { return true; }
            @Override public void onEnd(ReadableSpan span) {
                if (!span.getSpanContext().isSampled()) return;
                // Serialize only the non-blocking queue handoff with shutdown.
                // Never hold this monitor while waiting for export completion.
                synchronized (BoundedSignalAdmission.this) {
                    if (!acquire()) return;
                    try { delegate.onEnd(span); } catch (Throwable ignored) { rejected(); }
                }
            }
            @Override public CompletableResultCode forceFlush() { return delegate.forceFlush(); }
            @Override public CompletableResultCode shutdown() {
                synchronized (BoundedSignalAdmission.this) { closed.set(true); return delegate.shutdown(); }
            }
        };
    }

    LogRecordProcessor logs(LogRecordProcessor delegate) {
        return new LogRecordProcessor() {
            @Override public void onEmit(Context context, ReadWriteLogRecord record) {
                synchronized (BoundedSignalAdmission.this) {
                    if (!acquire()) return;
                    try { delegate.onEmit(context, record); } catch (Throwable ignored) { rejected(); }
                }
            }
            @Override public CompletableResultCode forceFlush() { return delegate.forceFlush(); }
            @Override public CompletableResultCode shutdown() {
                synchronized (BoundedSignalAdmission.this) { closed.set(true); return delegate.shutdown(); }
            }
        };
    }
}
