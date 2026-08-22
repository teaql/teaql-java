package io.teaql.core;

/** Provider-neutral positional row used by generated, strongly typed mappers. */
public interface DataRow {
    Object get(int columnIndex);

    <V> V get(int columnIndex, Class<V> type);
}
