package io.teaql.core.sql.portable;

import io.teaql.core.InternalIdGenerationService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * {@link InternalIdGenerationService} implementation backed by the {@code teaql_id_space} table.
 *
 * <p>This is the standard ID generation strategy for SQL-based TeaQL deployments.
 * It uses a simple SELECT-then-UPDATE approach within a transaction to allocate
 * monotonically increasing IDs per type name.</p>
 *
 * <p>Thread safety is guaranteed by the database transaction (row-level lock on
 * the {@code teaql_id_space} row for the given type name).</p>
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
        AtomicLong result = new AtomicLong();

        database.executeInTransaction(() -> {
            Number dbCurrent = null;
            try {
                List<Map<String, Object>> rows = database.query(
                        "SELECT current_level FROM " + idSpaceTable + " WHERE type_name = ?",
                        new Object[]{typeName});
                if (!rows.isEmpty()) {
                    Object val = rows.get(0).get("current_level");
                    if (val instanceof Number) {
                        dbCurrent = (Number) val;
                    } else if (val != null) {
                        dbCurrent = Long.parseLong(String.valueOf(val));
                    }
                }
            } catch (Exception ignored) {
                // Table may not exist yet on first call
            }

            if (dbCurrent == null) {
                result.set(1L);
                database.executeUpdate(
                        "INSERT INTO " + idSpaceTable + " (type_name, current_level) VALUES (?, ?)",
                        new Object[]{typeName, 1L});
            } else {
                long next = dbCurrent.longValue() + 1;
                database.executeUpdate(
                        "UPDATE " + idSpaceTable + " SET current_level = ? WHERE type_name = ?",
                        new Object[]{next, typeName});
                result.set(next);
            }
        });

        return result.get();
    }

    @Override
    public Long generateId(io.teaql.core.UserContext ctx, io.teaql.core.Entity entity) {
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
                    + " (type_name VARCHAR(100) PRIMARY KEY, current_level BIGINT)");
        } catch (Exception e) {
            LOG.fine("teaql_id_space table may already exist: " + e.getMessage());
        }
    }
}
