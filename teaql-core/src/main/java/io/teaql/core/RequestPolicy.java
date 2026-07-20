package io.teaql.core;

/**
 * Request policy interface, called before each operation.
 * Can modify queries/commands or reject execution.
 *
 * Design aligned with teaql-rs RequestPolicy trait.
 */
public interface RequestPolicy {

    /**
     * Policy check before query execution.
     * Can reject if comment or purpose is missing.
     */
    default void enforceSelect(UserContext ctx, SearchRequest<?> query) {
    }

    /**
     * Policy check before insert.
     */
    default void enforceInsert(UserContext ctx, Entity entity) {
    }

    /**
     * Policy check before update.
     */
    default void enforceUpdate(UserContext ctx, Entity entity) {
    }

    /**
     * Policy check before delete.
     */
    default void enforceDelete(UserContext ctx, Entity entity) {
    }

    /**
     * Policy check before recover.
     */
    default void enforceRecover(UserContext ctx, Entity entity) {
    }
}
