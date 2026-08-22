package io.teaql.core;

/** Generated row mapper. Column indexes are one-based and follow the compiled SELECT list. */
@FunctionalInterface
public interface CompiledRowMapper<T extends Entity> {
    T map(DataRow row);
}
