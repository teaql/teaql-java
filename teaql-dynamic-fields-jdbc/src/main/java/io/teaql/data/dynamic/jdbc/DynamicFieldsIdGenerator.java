package io.teaql.data.dynamic.jdbc;

import io.teaql.dataservice.sql.SqlExecutionAdapter;

import java.util.List;
import java.util.Map;

/**
 * ID generator for dynamic field definitions using teaql_id_space.
 */
public final class DynamicFieldsIdGenerator {

    private static final String TYPE_NAME = "DynamicFieldDef";

    private static final String SQL_UPDATE =
            "UPDATE teaql_id_space SET current_level = current_level + 1 WHERE type_name = ?";

    private static final String SQL_SELECT =
            "SELECT current_level FROM teaql_id_space WHERE type_name = ?";

    private final SqlExecutionAdapter executor;

    public DynamicFieldsIdGenerator(SqlExecutionAdapter executor) {
        this.executor = executor;
    }

    /**
     * Allocates the next ID for DynamicFieldDef.
     * Uses UPDATE-then-SELECT on teaql_id_space for atomic increment.
     */
    public long nextId() {
        int updated = executor.update(SQL_UPDATE, new Object[]{TYPE_NAME});
        if (updated == 0) {
            throw new RuntimeException("teaql_id_space entry for '" + TYPE_NAME + "' not found. " +
                    "Call DynamicFieldsSchema.ensureSchema() first.");
        }
        List<Map<String, Object>> rows = executor.queryForList(SQL_SELECT, new Object[]{TYPE_NAME});
        if (rows.isEmpty()) {
            throw new RuntimeException("Failed to read current_level from teaql_id_space for '" + TYPE_NAME + "'");
        }
        return ((Number) rows.get(0).get("current_level")).longValue();
    }
}
