package io.teaql.core.sql.portable;

import io.teaql.core.Entity;
import io.teaql.core.SmartList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Sequential, bounded enhancement. No batch is delivered before its validation succeeds. */
final class BatchedRootStream {
    private BatchedRootStream() {}

    static <T extends Entity> Stream<T> enhance(
            Stream<T> source, int batchSize, Consumer<SmartList<T>> hydrate) {
        if (batchSize < 1) throw new IllegalArgumentException("batch size must be positive");
        var cursor = new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
            private Iterator<T> input;
            private Iterator<T> ready = java.util.Collections.emptyIterator();
            private boolean closed;

            private void close() {
                if (!closed) { closed = true; ready = java.util.Collections.emptyIterator(); source.close(); }
            }

            @Override public Spliterator<T> trySplit() { return null; }

            @Override public boolean tryAdvance(Consumer<? super T> action) {
                if (closed) return false;
                try {
                    if (!ready.hasNext()) {
                        if (input == null) input = source.iterator();
                        if (!input.hasNext()) { close(); return false; }
                        var batch = new SmartList<T>(batchSize);
                        do { batch.add(input.next()); }
                        while (batch.size() < batchSize && input.hasNext());
                        hydrate.accept(batch);
                        ready = batch.iterator();
                    }
                    action.accept(ready.next());
                    return true;
                } catch (RuntimeException | Error error) {
                    try { close(); } catch (RuntimeException | Error cleanup) { error.addSuppressed(cleanup); }
                    throw error;
                }
            }
        };
        return StreamSupport.stream(cursor, false).onClose(cursor::close);
    }
}
