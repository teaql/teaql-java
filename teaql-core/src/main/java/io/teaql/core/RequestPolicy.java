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
    default void enforceSelect(UserContext context, SearchRequest<?> query) {
    }

    /**
     * Policy check before insert.
     */
    default void enforceInsert(UserContext context, Entity entity) {
    }

    /**
     * Policy check before update.
     */
    default void enforceUpdate(UserContext context, Entity entity) {
    }

    /**
     * Policy check before delete.
     */
    default void enforceDelete(UserContext context, Entity entity) {
    }

    /**
     * Policy check before recover.
     */
    default void enforceRecover(UserContext context, Entity entity) {
    }
}
