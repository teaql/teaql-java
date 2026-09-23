package io.teaql.businessid.jdbc;

import io.teaql.core.BusinessIdGenerator;
import io.teaql.core.Entity;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessIdErrorCode;
import io.teaql.core.businessid.BusinessIdException;
import io.teaql.core.businessid.BusinessIdSchemaContributor;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.core.utils.StrUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Legacy JDBC business ID format, retained for existing callers. Prefer the
 * context-owned BusinessIdService and JdbcBusinessIdAllocator for new models.
 * Schema creation is explicit through context.ensureSchema().
 */
@Deprecated
public class JdbcBusinessIdGenerator implements BusinessIdGenerator, BusinessIdSchemaContributor {

    private static final int MAX_ATTEMPTS = 100;
    
    private final TeaQLDatabase database;
    private final String sequenceTable;

    public JdbcBusinessIdGenerator(TeaQLDatabase database) {
        this(database, "teaql_biz_sequence");
    }

    public JdbcBusinessIdGenerator(TeaQLDatabase database, String sequenceTable) {
        this.database = java.util.Objects.requireNonNull(database, "database");
        if (sequenceTable == null || !sequenceTable.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid Business ID table name: " + sequenceTable);
        }
        this.sequenceTable = sequenceTable;
    }

    @Override
    public void ensureSchema(UserContext context) {
        String ddl = "CREATE TABLE IF NOT EXISTS " + sequenceTable + " ("
                + "sequence_key VARCHAR(100) PRIMARY KEY, "
                + "current_value BIGINT NOT NULL)";
        database.execute(java.util.Objects.requireNonNull(context, "context"), ddl);
    }

    @Override
    public String generateBusinessId(UserContext context, Entity entity, EntityDescriptor entityDesc, PropertyDescriptor propertyDesc) {
        String rule = propertyDesc.getAdditionalInfo().get("business_id_rule");
        if (StrUtil.isEmpty(rule)) {
            throw new IllegalArgumentException("No business_id_rule defined in metadata for " + entityDesc.getType() + "." + propertyDesc.getName());
        }

        String[] parts = rule.split(",");
        String prefix = parts[0].trim();
        int length = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 6;

        String dateStr = java.util.Objects.requireNonNull(context, "context")
                .businessDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        String sequenceKey = prefix + ":" + dateStr;

        if (length < 1 || length > 18) {
            throw new IllegalArgumentException("Legacy Business ID digits must be between 1 and 18");
        }
        long maximum = 1;
        for (int i = 0; i < length; i++) maximum = Math.multiplyExact(maximum, 10);
        long seq = nextSequence(context, sequenceKey, maximum - 1);

        return String.format(java.util.Locale.ROOT,
                "%s%s%0" + length + "d", prefix, dateStr, seq);
    }

    private long nextSequence(UserContext context, String sequenceKey, long maximum) {
        RuntimeException lastConflict = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            List<Map<String, Object>> rows;
            try {
                rows = database.query(context,
                        "SELECT current_value FROM " + sequenceTable + " WHERE sequence_key = ?",
                        new Object[]{sequenceKey});
            } catch (RuntimeException failure) {
                throw new TeaQLRuntimeException(
                        "Cannot read legacy Business ID table " + sequenceTable
                                + "; call context.ensureSchema() before generating IDs",
                        failure);
            }
            if (rows == null) {
                throw new TeaQLRuntimeException("Legacy Business ID query returned no result set");
            }
            if (rows.isEmpty()) {
                RuntimeException conflict = null;
                try {
                    int inserted = database.executeUpdate(context,
                            "INSERT INTO " + sequenceTable
                                    + " (sequence_key, current_value) VALUES (?, 1)",
                            new Object[]{sequenceKey});
                    if (inserted == 1) return 1;
                    conflict = new TeaQLRuntimeException(
                            "Expected one inserted Business ID row, got " + inserted);
                } catch (RuntimeException insertionFailure) {
                    conflict = insertionFailure;
                }
                lastConflict = conflict;
                // A collision is retried only after a fresh exact-key read proves a row exists.
                try {
                    List<Map<String, Object>> observed = database.query(context,
                            "SELECT current_value FROM " + sequenceTable
                                    + " WHERE sequence_key = ?",
                            new Object[]{sequenceKey});
                    if (observed == null || observed.isEmpty()) {
                        throw new TeaQLRuntimeException(
                                "Business ID insert failed without a matching row for "
                                        + sequenceKey,
                                conflict);
                    }
                } catch (TeaQLRuntimeException failure) {
                    throw failure;
                } catch (RuntimeException readFailure) {
                    readFailure.addSuppressed(conflict);
                    throw new TeaQLRuntimeException(
                            "Cannot verify Business ID insert for " + sequenceKey,
                            readFailure);
                }
            } else {
                Object value = rows.get(0).get("current_value");
                if (value == null) value = rows.get(0).get("CURRENT_VALUE");
                if (!(value instanceof Number)) {
                    throw new TeaQLRuntimeException(
                            "Invalid legacy Business ID current_value for " + sequenceKey);
                }
                long current = ((Number) value).longValue();
                if (current >= maximum) {
                    throw new BusinessIdException(
                            BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                            "Business ID range exhausted for " + sequenceKey);
                }
                int updated = database.executeUpdate(context,
                        "UPDATE " + sequenceTable
                                + " SET current_value = ?"
                                + " WHERE sequence_key = ? AND current_value = ?",
                        new Object[]{current + 1, sequenceKey, current});
                if (updated == 1) return current + 1;
                if (updated != 0) {
                    throw new TeaQLRuntimeException(
                            "Expected one updated Business ID row, got " + updated);
                }
            }
            java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
        }
        throw new BusinessIdException(
                BusinessIdErrorCode.BUSINESS_ID_ALLOCATION_RETRY_EXHAUSTED,
                "Legacy Business ID allocation did not converge for " + sequenceKey,
                lastConflict);
    }
}
