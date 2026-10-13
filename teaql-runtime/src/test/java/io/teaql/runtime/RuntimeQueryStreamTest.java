package io.teaql.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

public class RuntimeQueryStreamTest {
    private static final class Scope implements RuntimeTelemetry.Scope {
        final List<String> events = new ArrayList<>();
        int activations;
        Map<String, Object> completion;
        Throwable failure;
        public void success(Map<String, Object> values) { events.add("success"); completion = values; }
        public void failure(Throwable error) { events.add("failure"); failure = error; }
        public void detach() { events.add("detach"); }
        public RuntimeTelemetry.PropagationScope activate() {
            activations++; return () -> activations--;
        }
    }

    @Test public void exhaustedCursorHasOneCompletionDeliveredCountAndNoLeakedActivation() {
        var scope = new Scope(); var closed = new AtomicInteger();
        try (var stream = RuntimeQueryStream.wrap(Stream.of(1, 2, 3).onClose(closed::incrementAndGet), scope)) {
            assertEquals(List.of("detach"), scope.events);
            assertEquals(3, stream.count());
        }
        assertEquals(List.of("detach", "success"), scope.events);
        assertEquals(3L, scope.completion.get("teaql.result.cardinality"));
        assertEquals(1, closed.get()); assertEquals(0, scope.activations);
    }

    @Test public void shortCircuitAndUnconsumedCloseAreCancellationNotSuccess() {
        for (boolean consume : List.of(false, true)) {
            var scope = new Scope(); var closed = new AtomicInteger();
            var stream = RuntimeQueryStream.wrap(Stream.of(1, 2).onClose(closed::incrementAndGet), scope);
            if (consume) assertEquals(1, stream.findFirst().get().intValue());
            stream.close(); stream.close();
            assertEquals(List.of("detach", "failure"), scope.events);
            assertEquals("cancelled", RuntimeTelemetry.errorCategory(scope.failure));
            assertEquals(1, closed.get()); assertEquals(0, scope.activations);
        }
    }

    @Test public void originalTraversalFailurePreservedAndCloseFailureSuppressedOnce() {
        var original = new IllegalStateException("private traversal canary");
        var closeFailure = new IllegalArgumentException("private close canary");
        var scope = new Scope();
        try (var stream = RuntimeQueryStream.wrap(Stream.of(1).onClose(() -> { throw closeFailure; }), scope)) {
            var observed = assertThrows(IllegalStateException.class, () -> stream.forEach(value -> { throw original; }));
            assertSame(original, observed); assertArrayEquals(new Throwable[]{closeFailure}, observed.getSuppressed());
        }
        assertSame(original, scope.failure); assertEquals(0, scope.activations);
        assertEquals(List.of("detach", "failure"), scope.events);
    }

    @Test public void closeFailureWithoutTraversalIsFailureNotCancellation() {
        var failure = new IllegalStateException("private close canary");
        var scope = new Scope();
        var stream = RuntimeQueryStream.wrap(Stream.of(1).onClose(() -> { throw failure; }), scope);
        assertSame(failure, assertThrows(IllegalStateException.class, stream::close));
        assertSame(failure, scope.failure); assertEquals(0, scope.activations);
    }
}
