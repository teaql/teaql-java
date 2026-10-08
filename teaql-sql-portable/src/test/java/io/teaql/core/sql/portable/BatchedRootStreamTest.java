package io.teaql.core.sql.portable;

import io.teaql.core.BaseEntity;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

public class BatchedRootStreamTest {
    @Test public void boundsReadAheadAndClosesEarlyWithoutLoadingTheNextBatch() {
        var reads = new AtomicInteger(); var batches = new AtomicInteger(); var closes = new AtomicInteger();
        var source = IntStream.range(0, 10).mapToObj(i -> { reads.incrementAndGet(); return new BaseEntity(); })
                .onClose(closes::incrementAndGet);
        try (var stream = BatchedRootStream.enhance(source, 3, rows -> {
            assertEquals(3, rows.size()); batches.incrementAndGet();
        })) {
            assertEquals(0, reads.get());
            assertEquals(1, stream.limit(1).count());
            assertEquals(3, reads.get()); assertEquals(1, batches.get());
        }
        assertEquals(1, closes.get());
    }

    @Test public void emptyStreamDoesNotCallHydrationAndCloses() {
        var closes = new AtomicInteger();
        try (var stream = BatchedRootStream.enhance(Stream.<BaseEntity>empty().onClose(closes::incrementAndGet),
                3, rows -> fail("empty streams must not access dynamic provider"))) {
            assertEquals(0, stream.count());
        }
        assertEquals(1, closes.get());
    }

    @Test public void failedBatchDeliversNothingClosesAndCannotResume() {
        var delivered = new AtomicInteger(); var closes = new AtomicInteger();
        var failure = new IllegalStateException("controlled invalid batch");
        try (var stream = BatchedRootStream.enhance(Stream.of(new BaseEntity(), new BaseEntity())
                .onClose(closes::incrementAndGet), 2, rows -> { throw failure; })) {
            var cursor = stream.spliterator();
            try { cursor.tryAdvance(row -> delivered.incrementAndGet()); fail("must reject"); }
            catch (IllegalStateException error) { assertSame(failure, error); }
            assertEquals(0, delivered.get()); assertEquals(1, closes.get());
            assertFalse(cursor.tryAdvance(row -> delivered.incrementAndGet()));
        }
        assertEquals(1, closes.get());
    }

    @Test public void preservesOrderingAndLoadsTheTailOnce() {
        var batches = new java.util.ArrayList<Integer>();
        var entities = List.of(new BaseEntity(), new BaseEntity(), new BaseEntity());
        try (var stream = BatchedRootStream.enhance(entities.stream(), 2, rows -> batches.add(rows.size()))) {
            assertEquals(entities, stream.toList());
        }
        assertEquals(List.of(2, 1), batches);
    }
}
