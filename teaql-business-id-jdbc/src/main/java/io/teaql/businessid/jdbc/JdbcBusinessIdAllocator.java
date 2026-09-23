package io.teaql.businessid.jdbc;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.*;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.core.sql.dialect.PostgreSqlDialect;
import io.teaql.core.sql.dialect.SqlDialect;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.Optional;

/** Portable optimistic Business ID allocator backed by a TeaQLDatabase. */
public final class JdbcBusinessIdAllocator
        implements BusinessIdAllocator, BusinessIdSchemaContributor {
    public static final String DEFAULT_TABLE = "teaql_business_id_space";
    private static final int MAX_ATTEMPTS = 100;

    private final TeaQLDatabase database;
    private final String table;
    private final SqlDialect dialect;

    public JdbcBusinessIdAllocator(TeaQLDatabase database) {
        this(database, DEFAULT_TABLE);
    }

    public JdbcBusinessIdAllocator(TeaQLDatabase database, String table) {
        this(database, table, new PostgreSqlDialect());
    }

    public JdbcBusinessIdAllocator(TeaQLDatabase database, String table, SqlDialect dialect) {
        this.database = java.util.Objects.requireNonNull(database, "database");
        if (table == null || !table.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid Business ID table name: " + table);
        }
        this.table = table;
        this.dialect = java.util.Objects.requireNonNull(dialect, "dialect");
    }

    /** Explicit development/test schema operation; construction never executes DDL. */
    @Override
    public void ensureSchema(UserContext context) {
        List<Map<String, Object>> existing = inspectColumns();
        if (!existing.isEmpty()) {
            requireColumns(existing);
            return;
        }
        try {
            database.execute(context, "CREATE TABLE " + table + " ("
                    + "scope_key " + dialect.mapColumnType("VARCHAR(512)") + " PRIMARY KEY, "
                    + "current_value " + dialect.mapColumnType("BIGINT") + " NOT NULL, "
                    + "version " + dialect.mapColumnType("BIGINT") + " NOT NULL, "
                    + "updated_at " + dialect.mapColumnType("BIGINT") + " NOT NULL)");
        } catch (RuntimeException createFailure) {
            // Another instance may have created the table after inspection.
            // Only accept that race when the installed table passes the same
            // validation as a table that was already present at startup.
            List<Map<String, Object>> concurrent;
            try {
                concurrent = inspectColumns();
            } catch (RuntimeException inspectFailure) {
                createFailure.addSuppressed(inspectFailure);
                throw new IllegalStateException(
                        "Cannot create or inspect Business ID table " + table,
                        createFailure);
            }
            if (!concurrent.isEmpty()) {
                requireColumns(concurrent);
                return;
            }
            throw new IllegalStateException(
                    "Cannot create Business ID table " + table, createFailure);
        }
        requireColumns(inspectColumns());
    }

    private List<Map<String, Object>> inspectColumns() {
        try {
            List<Map<String, Object>> columns = database.getTableColumns(table);
            if (columns == null) {
                throw new IllegalStateException("Column inspection returned null for " + table);
            }
            return columns;
        } catch (RuntimeException failure) {
            throw new IllegalStateException(
                    "Cannot inspect Business ID table " + table
                            + "; the database adapter must provide column metadata",
                    failure);
        }
    }

    private void requireColumns(List<Map<String, Object>> columns) {
        Map<String, Map<String, Object>> found = new HashMap<>();
        for (Map<String, Object> column : columns) {
            Object name = value(column, "column_name");
            if (name != null) {
                found.put(String.valueOf(name).toLowerCase(Locale.ROOT), column);
            }
        }
        Set<String> missing = new java.util.TreeSet<>(Set.of(
                "scope_key", "current_value", "version", "updated_at"));
        missing.removeAll(found.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Business ID table " + table + " is missing required columns " + missing);
        }

        requireTextScope(found.get("scope_key"));
        for (String name : List.of("current_value", "version", "updated_at")) {
            requireIntegralNotNull(name, found.get(name));
        }

        Optional<List<String>> primaryKey;
        try {
            primaryKey = database.getTablePrimaryKeyColumns(table);
        } catch (RuntimeException failure) {
            throw new IllegalStateException(
                    "Cannot inspect primary key of Business ID table " + table, failure);
        }
        if (primaryKey != null && primaryKey.isPresent()
                && scopeKeyOnly(primaryKey.get())) {
            return;
        }
        Optional<List<List<String>>> uniqueKeys;
        try {
            uniqueKeys = database.getTableUniqueKeys(table);
        } catch (RuntimeException failure) {
            throw new IllegalStateException(
                    "Cannot inspect unique keys of Business ID table " + table, failure);
        }
        if (uniqueKeys != null && uniqueKeys.isPresent()
                && uniqueKeys.get().stream().anyMatch(this::scopeKeyOnly)) {
            return;
        }
        throw new IllegalStateException(
                "Business ID table " + table
                        + " requires a single-column scope_key primary or unique key"
                        + "; inspected primary=" + primaryKey + ", unique=" + uniqueKeys);
    }

    private boolean scopeKeyOnly(List<String> names) {
        return names.size() == 1 && "scope_key".equalsIgnoreCase(names.get(0));
    }

    private void requireTextScope(Map<String, Object> column) {
        int type = metadataNumber(column, "data_type", "scope_key");
        int size = metadataNumber(column, "column_size", "scope_key");
        boolean unbounded = type == Types.LONGVARCHAR || type == Types.LONGNVARCHAR
                || type == Types.CLOB || type == Types.NCLOB;
        boolean text = unbounded || type == Types.VARCHAR || type == Types.NVARCHAR
                || type == Types.CHAR || type == Types.NCHAR;
        if (!text || (!unbounded && size < 512)) {
            throw new IllegalStateException(
                    "Business ID table " + table
                            + ".scope_key must be text with capacity at least 512; found "
                            + value(column, "type_name") + "(" + size + ")");
        }
    }

    private void requireIntegralNotNull(String name, Map<String, Object> column) {
        int type = metadataNumber(column, "data_type", name);
        int nullable = metadataNumber(column, "nullable", name);
        int precision = metadataNumber(column, "column_size", name);
        int scale = metadataNumber(column, "decimal_digits", name);
        String declaredType = String.valueOf(value(column, "type_name"))
                .toUpperCase(Locale.ROOT);
        boolean longInteger = type == Types.BIGINT
                || declaredType.equals("BIGINT") || declaredType.equals("INT8")
                || ((type == Types.NUMERIC || type == Types.DECIMAL)
                        && precision >= 19 && scale == 0);
        if (!longInteger) {
            throw new IllegalStateException(
                    "Business ID table " + table + "." + name
                            + " must store signed 64-bit integral values; found "
                            + declaredType + "(" + precision + "," + scale + ")");
        }
        if (nullable != DatabaseMetaData.columnNoNulls) {
            throw new IllegalStateException(
                    "Business ID table " + table + "." + name + " must be NOT NULL");
        }
    }

    private int metadataNumber(Map<String, Object> column, String key, String name) {
        Object result = value(column, key);
        if (result instanceof Number number) return number.intValue();
        throw new IllegalStateException(
                "Business ID table " + table + "." + name
                        + " is missing required " + key + " metadata");
    }

    private Object value(Map<String, Object> column, String key) {
        for (Map.Entry<String, Object> entry : column.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) return entry.getValue();
        }
        return null;
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
            List<Map<String, Object>> rows;
            try {
                rows = database.query(
                        "SELECT current_value, version FROM " + table
                                + " WHERE scope_key = ?",
                        new Object[]{scopeKey});
            } catch (RuntimeException failure) {
                throw new IllegalStateException(
                        "Cannot read Business ID table " + table
                                + "; verify database access and call context.ensureSchema()",
                        failure);
            }
            if (rows == null) {
                throw new IllegalStateException(
                        "Business ID database returned a null result set for " + scopeKey);
            }
            if (rows.isEmpty()) {
                Integer inserted = null;
                try {
                    inserted = database.executeUpdate(
                            "INSERT INTO " + table
                                    + " (scope_key, current_value, version, updated_at)"
                                    + " VALUES (?, 1, 1, ?)",
                            new Object[]{scopeKey, updatedAt});
                } catch (RuntimeException insertionFailure) {
                    lastConflict = insertionFailure;
                    // A competing instance may have inserted this key. Do not retry an
                    // unrelated write failure unless that exact row is now observable.
                    List<Map<String, Object>> observed;
                    try {
                        observed = database.query(
                                "SELECT current_value, version FROM " + table
                                        + " WHERE scope_key = ?",
                                new Object[]{scopeKey});
                    } catch (RuntimeException readFailure) {
                        readFailure.addSuppressed(insertionFailure);
                        throw new IllegalStateException(
                                "Cannot verify Business ID insert for " + scopeKey,
                                readFailure);
                    }
                    if (observed == null || observed.isEmpty()) {
                        throw new IllegalStateException(
                                "Business ID insert failed without a matching row for "
                                        + scopeKey,
                                insertionFailure);
                    }
                }
                if (inserted != null) {
                    if (inserted == 1) {
                        return new BusinessIdAllocation(plan.scope(), 1);
                    }
                    throw new IllegalStateException(
                            "Expected one inserted Business ID row for " + scopeKey
                                    + ", inserted " + inserted);
                }
            } else {
                Map<String, Object> row = rows.get(0);
                long current = number(row, "current_value");
                long version = number(row, "version");
                if (current < 1 || version < 1) {
                    throw new IllegalStateException(
                            "Invalid Business ID sequence row for " + scopeKey);
                }
                if (current >= plan.maximumSequence()) {
                    throw new BusinessIdException(
                            BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                            "Business ID range exhausted for " + scopeKey);
                }
                long next = current + 1;
                int updated = database.executeUpdate(
                        "UPDATE " + table
                                + " SET current_value = ?, version = version + 1, updated_at = ?"
                                + " WHERE scope_key = ? AND version = ? AND current_value = ?",
                        new Object[]{next, updatedAt, scopeKey, version, current});
                if (updated == 1) {
                    return new BusinessIdAllocation(plan.scope(), next);
                }
                if (updated != 0) {
                    throw new IllegalStateException(
                            "Expected at most one updated Business ID row for " + scopeKey
                                    + ", updated " + updated);
                }
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
