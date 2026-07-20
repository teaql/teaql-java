package io.teaql.businessid.jdbc;

import io.teaql.core.BusinessIdGenerator;
import io.teaql.core.Entity;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.sql.portable.TeaQLDatabase;
import io.teaql.core.utils.StrUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of BusinessIdGenerator.
 * Uses a dedicated sequence table {@code teaql_biz_sequence} to generate unique IDs.
 */
public class JdbcBusinessIdGenerator implements BusinessIdGenerator {

    private static final Logger LOG = Logger.getLogger(JdbcBusinessIdGenerator.class.getName());
    
    private final TeaQLDatabase database;
    private final String sequenceTable;

    public JdbcBusinessIdGenerator(TeaQLDatabase database) {
        this(database, "teaql_biz_sequence");
    }

    public JdbcBusinessIdGenerator(TeaQLDatabase database, String sequenceTable) {
        this.database = database;
        this.sequenceTable = sequenceTable;
        ensureSequenceTable();
    }

    private void ensureSequenceTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS " + sequenceTable + " ("
                + "sequence_key VARCHAR(100) PRIMARY KEY, "
                + "current_value BIGINT NOT NULL)";
        try {
            database.execute(ddl);
        } catch (Exception e) {
            LOG.log(Level.FINE, "teaql_biz_sequence table may already exist: " + e.getMessage());
        }
    }

    @Override
    public String generateBusinessId(UserContext ctx, Entity entity, EntityDescriptor entityDesc, PropertyDescriptor propertyDesc) {
        String rule = propertyDesc.getAdditionalInfo().get("business_id_rule");
        if (StrUtil.isEmpty(rule)) {
            throw new IllegalArgumentException("No business_id_rule defined in metadata for " + entityDesc.getType() + "." + propertyDesc.getName());
        }

        String[] parts = rule.split(",");
        String prefix = parts[0].trim();
        int length = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 6;

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequenceKey = prefix + ":" + dateStr;

        long seq = nextSequence(sequenceKey);

        return String.format("%s%s%0" + length + "d", prefix, dateStr, seq);
    }

    private long nextSequence(String sequenceKey) {
        AtomicLong result = new AtomicLong(-1);

        database.executeInTransaction(() -> {
            Number dbCurrent = null;
            try {
                List<Map<String, Object>> rows = database.query(
                        "SELECT current_value FROM " + sequenceTable + " WHERE sequence_key = ?",
                        new Object[]{sequenceKey}
                );
                if (rows != null && !rows.isEmpty()) {
                    dbCurrent = (Number) rows.get(0).get("current_value");
                }
            } catch (Exception e) {
                // Table might not exist or other error, fallback will attempt to fix
                ensureSequenceTable();
            }

            if (dbCurrent == null) {
                try {
                    database.executeUpdate(
                            "INSERT INTO " + sequenceTable + " (sequence_key, current_value) VALUES (?, 1)",
                            new Object[]{sequenceKey}
                    );
                    result.set(1);
                } catch (Exception e) {
                    // Concurrent insert collision, retry as update
                    int updated = database.executeUpdate(
                            "UPDATE " + sequenceTable + " SET current_value = current_value + 1 WHERE sequence_key = ?",
                            new Object[]{sequenceKey}
                    );
                    if (updated == 0) {
                        throw new TeaQLRuntimeException("Failed to initialize or update sequence: " + sequenceKey, e);
                    }
                    List<Map<String, Object>> rows = database.query(
                            "SELECT current_value FROM " + sequenceTable + " WHERE sequence_key = ?",
                            new Object[]{sequenceKey}
                    );
                    result.set(((Number) rows.get(0).get("current_value")).longValue());
                }
                return;
            }
            database.executeUpdate(
                    "UPDATE " + sequenceTable + " SET current_value = current_value + 1 WHERE sequence_key = ?",
                    new Object[]{sequenceKey}
            );
            result.set(dbCurrent.longValue() + 1);
        });

        if (result.get() == -1) {
            throw new TeaQLRuntimeException("Failed to read sequence value for key: " + sequenceKey);
        }

        return result.get();
    }
}
