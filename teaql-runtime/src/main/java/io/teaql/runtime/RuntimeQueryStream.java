package io.teaql.runtime;

import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Query ownership follows cursor consumption; never leaves a scope active in the caller. */
final class RuntimeQueryStream<T> implements Spliterator<T> {
    private final Stream<T> source;
    private final Spliterator<T> rows;
    private final RuntimeTelemetry.Scope scope;
    private boolean done;
    private long delivered;

    private RuntimeQueryStream(Stream<T> source, RuntimeTelemetry.Scope scope) {
        this.source = source; this.scope = scope; this.rows = source.spliterator();
    }

    static <T> Stream<T> wrap(Stream<T> source, RuntimeTelemetry.Scope scope) {
        try {
            var cursor = new RuntimeQueryStream<>(source, scope);
            var result = StreamSupport.stream(cursor, false).onClose(() -> cursor.finish(true, null));
            scope.detach();
            return result;
        } catch (RuntimeException | Error failure) {
            try { source.close(); } catch (RuntimeException | Error closeFailure) {
                if (failure != closeFailure) failure.addSuppressed(closeFailure);
            }
            scope.failure(failure); throw failure;
        }
    }

    @Override public boolean tryAdvance(Consumer<? super T> action) {
        if (done) return false;
        try (var activation = scope.activate()) {
            boolean more = rows.tryAdvance(value -> { delivered++; action.accept(value); });
            if (!more) finish(false, null);
            return more;
        } catch (RuntimeException | Error failure) {
            finish(false, failure); throw failure;
        }
    }

    private void finish(boolean cancelled, Throwable original) {
        if (done) return;
        done = true;
        Throwable failure = original;
        try (var activation = scope.activate()) { source.close(); }
        catch (RuntimeException | Error closeFailure) {
            if (failure == null) failure = closeFailure;
            else if (failure != closeFailure) failure.addSuppressed(closeFailure);
        }
        if (failure != null) scope.failure(failure);
        else if (cancelled) scope.failure(new CancellationException());
        else scope.success(Map.of("teaql.result.cardinality", delivered));
        if (original == null) {
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
        }
    }

    @Override public Spliterator<T> trySplit() { return null; }
    @Override public long estimateSize() { return Long.MAX_VALUE; }
    @Override public int characteristics() { return ORDERED; }
}
