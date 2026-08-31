package io.teaql.core.sql.portable;

import io.teaql.core.InternalIdGenerationService;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * {@link InternalIdGenerationService} implementation backed by the {@code teaql_id_space} table.
 *
 * <p>This is the standard ID generation strategy for SQL-based TeaQL deployments.
 * It uses a portable optimistic compare-and-set update to allocate monotonically
 * increasing IDs per type name.</p>
 *
 * <p>Cross-process safety is guaranteed by including the previously read level
 * in the update predicate and accepting the allocation only when exactly one row
 * was changed. A concurrent winner causes this generator to read and retry.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * TeaQLDatabase db = ...;
 * IdSpaceIdGenerator idGen = new IdSpaceIdGenerator(db);
 *
 * TeaQLRuntime runtime = TeaQLRuntime.builder()
 *     .metadata(metaFactory)
 *     .idGenerationService(idGen)
 *     .build();
 * }</pre>
 */
public class IdSpaceIdGenerator implements InternalIdGenerationService {

    private static final Logger LOG = Logger.getLogger(IdSpaceIdGenerator.class.getName());
    private static final int MAX_ALLOCATION_ATTEMPTS = 100;

    private final TeaQLDatabase database;
    private final String idSpaceTable;

    /**
     * Constructs with the default table name {@code teaql_id_space}.
     */
    public IdSpaceIdGenerator(TeaQLDatabase database) {
        this(database, "teaql_id_space");
    }

    /**
     * Constructs with a custom table name (for multi-schema or prefixed deployments).
     */
    public IdSpaceIdGenerator(TeaQLDatabase database, String idSpaceTable) {
        this.database = database;
        this.idSpaceTable = idSpaceTable;
    }

    @Override
    public long nextId(String typeName) {
        for (int attempt = 1; attempt <= MAX_ALLOCATION_ATTEMPTS; attempt++) {
            Long current = readCurrentLevel(typeName);
            if (current == null) {
                try {
                    int inserted = database.executeUpdate(
                            "INSERT INTO " + idSpaceTable
                                    + " (type_name, current_level) VALUES (?, ?)",
                            new Object[]{typeName, 1L});
                    if (inserted == 1) {
                        return 1L;
                    }
                    throw new IllegalStateException(
                            "Expected one inserted ID space row for type " + typeName
                                    + ", inserted " + inserted);
                } catch (RuntimeException insertFailure) {
                    // A competing instance may have inserted the same primary-key row.
                    // Only treat it as contention when that row is now observable.
                    if (readCurrentLevel(typeName) == null) {
                        throw insertFailure;
                    }
                    continue;
                }
            }

            long next = Math.addExact(current, 1L);
            int updated = database.executeUpdate(
                    "UPDATE " + idSpaceTable
                            + " SET current_level = ?"
                            + " WHERE type_name = ? AND current_level = ?",
                    new Object[]{next, typeName, current});
            if (updated == 1) {
                return next;
            }
            if (updated != 0) {
                throw new IllegalStateException(
                        "Expected at most one ID space row for type " + typeName
                                + ", updated " + updated);
            }
            // Another process won the compare-and-set. Re-read and retry.
        }
        throw new IllegalStateException(
                "Unable to allocate ID for type " + typeName + " after "
                        + MAX_ALLOCATION_ATTEMPTS + " optimistic-lock attempts");
    }

    /** Advances an ID space to at least {@code floor} without ever moving it backwards. */
    @Override
    public void ensureFloor(String typeName, long floor) {
        for (int attempt = 1; attempt <= MAX_ALLOCATION_ATTEMPTS; attempt++) {
            Long current = readCurrentLevel(typeName);
            if (current == null) {
                try {
                    int inserted = database.executeUpdate(
                            "INSERT INTO " + idSpaceTable
                                    + " (type_name, current_level) VALUES (?, ?)",
                            new Object[]{typeName, floor});
                    if (inserted == 1) return;
                    throw new IllegalStateException(
                            "Expected one inserted ID space row for type " + typeName
                                    + ", inserted " + inserted);
                } catch (RuntimeException insertFailure) {
                    if (readCurrentLevel(typeName) == null) throw insertFailure;
                    continue;
                }
            }
            if (current >= floor) return;
            int updated = database.executeUpdate(
                    "UPDATE " + idSpaceTable
                            + " SET current_level = ?"
                            + " WHERE type_name = ? AND current_level = ?",
                    new Object[]{floor, typeName, current});
            if (updated == 1) return;
            if (updated != 0) {
                throw new IllegalStateException(
                        "Expected at most one ID space row for type " + typeName
                                + ", updated " + updated);
            }
        }
        throw new IllegalStateException(
                "Unable to advance ID floor for type " + typeName + " to " + floor
                        + " after " + MAX_ALLOCATION_ATTEMPTS + " optimistic-lock attempts");
    }

    private Long readCurrentLevel(String typeName) {
        List<Map<String, Object>> rows = database.query(
                "SELECT current_level FROM " + idSpaceTable + " WHERE type_name = ?",
                new Object[]{typeName});
        if (rows.isEmpty()) {
            return null;
        }
        Object value = rows.get(0).get("current_level");
        if (value == null) {
            throw new IllegalStateException(
                    "ID space current_level must not be null for type " + typeName);
        }
        return value instanceof Number
                ? ((Number) value).longValue()
                : Long.parseLong(String.valueOf(value));
    }

    @Override
    public Long generateId(io.teaql.core.UserContext context, io.teaql.core.Entity entity) {
        return nextId(entity.typeName());
    }

    /**
     * Ensures the {@code teaql_id_space} table exists.
     * Safe to call multiple times.
     */
    public void ensureIdSpaceTable() {
        try {
            database.execute(
                    "CREATE TABLE IF NOT EXISTS " + idSpaceTable
                    + " (type_name VARCHAR(100) NOT NULL PRIMARY KEY, current_level BIGINT)");
        } catch (Exception e) {
            LOG.fine("teaql_id_space table may already exist: " + e.getMessage());
        }
    }
}
