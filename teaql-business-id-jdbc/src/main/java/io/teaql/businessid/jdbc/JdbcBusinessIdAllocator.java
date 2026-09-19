package io.teaql.businessid.jdbc;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.*;
import io.teaql.core.sql.portable.TeaQLDatabase;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Portable optimistic Business ID allocator backed by a TeaQLDatabase. */
public final class JdbcBusinessIdAllocator implements BusinessIdAllocator {
    public static final String DEFAULT_TABLE = "teaql_business_id_space";
    private static final int MAX_ATTEMPTS = 100;

    private final TeaQLDatabase database;
    private final String table;

    public JdbcBusinessIdAllocator(TeaQLDatabase database) {
        this(database, DEFAULT_TABLE);
    }

    public JdbcBusinessIdAllocator(TeaQLDatabase database, String table) {
        this.database = java.util.Objects.requireNonNull(database, "database");
        if (table == null || !table.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid Business ID table name: " + table);
        }
        this.table = table;
    }

    /** Explicit development/test schema operation; construction never executes DDL. */
    public void ensureSchema(UserContext context) {
        database.execute(context, "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "scope_key VARCHAR(512) PRIMARY KEY, "
                + "current_value BIGINT NOT NULL, "
                + "version BIGINT NOT NULL, "
                + "updated_at BIGINT NOT NULL)");
    }

    @Override
    public BusinessIdAllocation allocate(BusinessIdPlan plan) {
        String scopeKey = plan.scope().canonicalKey();
        long updatedAt = plan.businessDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        RuntimeException lastConflict = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            AtomicReference<BusinessIdAllocation> result = new AtomicReference<>();
            try {
                database.executeInTransaction(() -> {
                    List<Map<String, Object>> rows = database.query(
                            "SELECT current_value, version FROM " + table
                                    + " WHERE scope_key = ?",
                            new Object[]{scopeKey});
                    if (rows == null || rows.isEmpty()) {
                        database.executeUpdate(
                                "INSERT INTO " + table
                                        + " (scope_key, current_value, version, updated_at)"
                                        + " VALUES (?, 1, 1, ?)",
                                new Object[]{scopeKey, updatedAt});
                        result.set(new BusinessIdAllocation(plan.scope(), 1));
                        return;
                    }
                    Map<String, Object> row = rows.get(0);
                    long current = number(row, "current_value");
                    long version = number(row, "version");
                    long next = current + 1;
                    if (next > plan.maximumSequence()) {
                        throw new BusinessIdException(
                                BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                                "Business ID range exhausted for " + scopeKey);
                    }
                    int updated = database.executeUpdate(
                            "UPDATE " + table
                                    + " SET current_value = ?, version = version + 1, updated_at = ?"
                                    + " WHERE scope_key = ? AND version = ?",
                            new Object[]{next, updatedAt, scopeKey, version});
                    if (updated == 1) {
                        result.set(new BusinessIdAllocation(plan.scope(), next));
                    }
                });
                if (result.get() != null) {
                    return result.get();
                }
            } catch (BusinessIdException error) {
                throw error;
            } catch (RuntimeException conflict) {
                lastConflict = conflict;
            }
            java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
        }
        throw new BusinessIdException(
                BusinessIdErrorCode.BUSINESS_ID_ALLOCATION_RETRY_EXHAUSTED,
                "Business ID allocation did not converge for " + scopeKey,
                lastConflict);
    }

    private long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            value = row.get(key.toUpperCase(java.util.Locale.ROOT));
        }
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Missing numeric column " + key);
        }
        return number.longValue();
    }
}
