package io.teaql.core;

/** Reviews a query before it is executed. */
public interface QueryPolicy {
    default void enforceSelect(UserContext context, SearchRequest<?> query) {}
}
