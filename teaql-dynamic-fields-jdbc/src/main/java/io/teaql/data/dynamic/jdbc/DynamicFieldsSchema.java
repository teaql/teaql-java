package io.teaql.data.dynamic.jdbc;

import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DDL constants and schema creation for dynamic fields tables.
 * All table names use the 'teaql_' prefix to indicate internal framework tables.
 */
public final class DynamicFieldsSchema {

    private static final Logger LOG = Logger.getLogger(DynamicFieldsSchema.class.getName());

    private DynamicFieldsSchema() {}

    // ─── Table Names ───────────────────────────────────────────────────

    public static final String TABLE_FIELD_DEF = "teaql_dynamic_field_def";
    public static final String TABLE_FIELD_VALUE = "teaql_dynamic_field_value";

    // ─── DDL ───────────────────────────────────────────────────────────

    public static final String DDL_FIELD_DEF =
            "CREATE TABLE IF NOT EXISTS teaql_dynamic_field_def ("
            + "id BIGINT PRIMARY KEY, "
            + "scope_type VARCHAR(50) NOT NULL, "
            + "scope_id VARCHAR(100) NOT NULL, "
            + "owner_type VARCHAR(100) NOT NULL, "
            + "code VARCHAR(100) NOT NULL, "
            + "name VARCHAR(200), "
            + "description VARCHAR(500), "
            + "data_type VARCHAR(20) NOT NULL, "
            + "logical_type VARCHAR(30), "
            + "required SMALLINT DEFAULT 0, "
            + "visible SMALLINT DEFAULT 1, "
            + "editable SMALLINT DEFAULT 1, "
            + "filterable SMALLINT DEFAULT 0, "
            + "sortable SMALLINT DEFAULT 0, "
            + "searchable SMALLINT DEFAULT 0, "
            + "exportable SMALLINT DEFAULT 0, "
            + "importable SMALLINT DEFAULT 0, "
            + "auditable SMALLINT DEFAULT 1, "
            + "privacy_level VARCHAR(50), "
            + "mask_rule VARCHAR(200), "
            + "default_value VARCHAR(500), "
            + "status VARCHAR(20) NOT NULL, "
            + "display_order INTEGER DEFAULT 0, "
            + "version BIGINT DEFAULT 1, "
            + "created_by VARCHAR(100), "
            + "created_at BIGINT, "
            + "updated_by VARCHAR(100), "
            + "updated_at BIGINT)";

    public static final String IDX_FIELD_DEF_UK =
            "CREATE UNIQUE INDEX IF NOT EXISTS uk_tdfd_scope_owner_code "
            + "ON teaql_dynamic_field_def (scope_type, scope_id, owner_type, code)";

    public static final String DDL_FIELD_VALUE =
            "CREATE TABLE IF NOT EXISTS teaql_dynamic_field_value ("
            + "scope_type VARCHAR(50) NOT NULL, "
            + "scope_id VARCHAR(100) NOT NULL, "
            + "owner_type VARCHAR(100) NOT NULL, "
            + "owner_id BIGINT NOT NULL, "
            + "field_id BIGINT NOT NULL, "
            + "string_value VARCHAR(4000), "
            + "number_value BIGINT, "
            + "bool_value SMALLINT, "
            + "datetime_value BIGINT, "
            + "enum_value VARCHAR(200), "
            + "version BIGINT DEFAULT 1, "
            + "updated_by VARCHAR(100), "
            + "updated_at BIGINT, "
            + "PRIMARY KEY (scope_type, scope_id, owner_type, owner_id, field_id))";

    public static void ensureSchema(SqlExecutionAdapter executor) {
        tryExecute(executor, DDL_FIELD_DEF);
        tryExecute(executor, IDX_FIELD_DEF_UK);
        tryExecute(executor, DDL_FIELD_VALUE);
        LOG.info("Dynamic fields schema ensured.");
    }

    private static void tryExecute(SqlExecutionAdapter executor, String ddl) {
        try {
            executor.execute(ddl);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Schema element may already exist: {0}", e.getMessage());
        }
    }
}
