package io.teaql.core;

/**
 * Unified ID generation service for all TeaQL components.
 *
 * <p>The primary method is {@link #generateId(UserContext, Entity)} for entity ID allocation.
 * The {@link #nextId(String)} method provides a lower-level, context-free alternative
 * for framework-internal components (e.g., DynamicFieldsProvider) that need IDs
 * without an Entity instance.</p>
 *
 * <p>Implementations include:</p>
 * <ul>
 *   <li>{@code IdSpaceIdGenerator} (teaql-sql-portable) — uses the {@code teaql_id_space} table</li>
 *   <li>Simple lambda / AtomicLong — for unit tests</li>
 *   <li>Snowflake / UUID — for distributed deployments</li>
 * </ul>
 */
public interface InternalIdGenerationService {

    /**
     * Allocates a new unique ID for the given entity.
     *
     * @param context    the user context
     * @param entity the entity that needs an ID
     * @return a new unique ID
     */
    Long generateId(UserContext context, Entity entity);

    /**
     * Allocates a new unique ID for the given type name.
     * Used by framework-internal components that don't have an Entity instance.
     *
     * <p>The default implementation throws {@code UnsupportedOperationException}.
     * Implementations like {@code IdSpaceIdGenerator} override this with a direct,
     * efficient implementation.</p>
     *
     * @param typeName the logical type name, e.g. "Platform", "DynamicFieldDef"
     * @return a new unique ID
     * @throws UnsupportedOperationException if the implementation does not support
     *         type-name-based ID generation
     */
    default long nextId(String typeName) {
        throw new UnsupportedOperationException(
                "This IdGenerationService does not support type-name-based ID generation. "
                + "Use an implementation like IdSpaceIdGenerator.");
    }

    /**
     * Advances the allocation state for {@code typeName} so future generated IDs are greater
     * than {@code floor}. Generated schema bootstrap uses this after reserving model-defined IDs.
     */
    default void ensureFloor(String typeName, long floor) {
        throw new UnsupportedOperationException(
                "This IdGenerationService cannot reserve a fixed-ID floor for " + typeName);
    }
}
