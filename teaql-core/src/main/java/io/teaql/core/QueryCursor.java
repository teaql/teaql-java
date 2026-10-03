package io.teaql.core;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A single-use stream and its invocation-owned physical evidence. Opening a
 * cursor is not completion: statements may be empty until exhaustion or close.
 * Always close, including after short-circuit terminal operations. This does not
 * make a Stream safe for parallel/concurrent consumption.
 */
public final class QueryCursor<T extends Entity> implements AutoCloseable {
    private final Stream<T> stream;
    private final Supplier<List<ExecutionMetadata>> evidence;

    public QueryCursor(Stream<T> stream, Supplier<List<ExecutionMetadata>> evidence) {
        this.stream = Objects.requireNonNull(stream, "stream");
        this.evidence = Objects.requireNonNull(evidence, "evidence");
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Stream<T> stream() { return stream; }

    /** Immutable list snapshot; mutable metadata contains trusted raw bindings, not wire data. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<ExecutionMetadata> statements() { return List.copyOf(evidence.get()); }

    @Override public void close() { stream.close(); }
}
