package io.teaql.dataservice.sql;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.UserContext;
import java.util.Spliterator;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Package-private cursor lifecycle. Does not make Stream safe for concurrent consumers. */
final class SqlDiagnosticStream<T> implements Spliterator<T> {
    private final UserContext context;
    private final Stream<T> source;
    private final ExecutionMetadata metadata;
    private final long start;
    private Spliterator<T> rows;
    private boolean done;
    private long delivered;

    private SqlDiagnosticStream(UserContext context, Stream<T> source, ExecutionMetadata metadata, long start) {
        this.context = context;
        this.source = source;
        this.metadata = metadata;
        this.start = start;
    }

    static <T> Stream<T> wrap(UserContext context, Stream<T> source, ExecutionMetadata metadata, long start) {
        var cursor = new SqlDiagnosticStream<>(context, source, metadata, start);
        try {
            cursor.rows = source.spliterator();
        } catch (RuntimeException | Error failure) {
            cursor.finish(outcome(failure), failure);
            throw failure;
        }
        return StreamSupport.stream(cursor, false).onClose(() -> cursor.finish("cancelled", null));
    }

    @Override public boolean tryAdvance(Consumer<? super T> action) {
        if (done) return false;
        try {
            boolean more = rows.tryAdvance(row -> {
                // Count rows handed to the caller, not prefetched rows or rows successfully processed.
                delivered++;
                action.accept(row);
            });
            if (!more) finish("success", null);
            return more;
        } catch (RuntimeException | Error failure) {
            finish(outcome(failure), failure);
            throw failure;
        }
    }

    private void finish(String outcome, Throwable original) {
        if (done) return;
        done = true;
        Throwable failure = original;
        try {
            source.close();
        } catch (RuntimeException | Error closeFailure) {
            if (failure == null) { failure = closeFailure; outcome = outcome(closeFailure); }
            else if (failure != closeFailure) failure.addSuppressed(closeFailure);
        }
        if (metadata != null) {
            metadata.setElapsedUs((System.nanoTime() - start) / 1000);
            metadata.setExecutionOutcome(outcome);
            metadata.setResultCount(delivered <= Integer.MAX_VALUE ? (int) delivered : null);
            metadata.setResultSummary("Cursor " + outcome + "; delivered " + delivered + " rows");
            try {
                context.recordExecutionMetadata(metadata);
            } catch (RuntimeException | Error sinkFailure) {
                if (failure == null) failure = sinkFailure;
            }
        }
        // The original traversal exception is rethrown by the caller of finish.
        if (original == null) {
            if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
            if (failure instanceof Error fatal) throw fatal;
        }
    }

    private static String outcome(Throwable failure) {
        return failure instanceof CancellationException ? "cancelled" : "failure";
    }

    @Override public Spliterator<T> trySplit() { return null; }
    @Override public long estimateSize() { return Long.MAX_VALUE; }
    // No SIZED: count() must traverse rather than elide resource cleanup and diagnostics.
    @Override public int characteristics() { return ORDERED; }
}
