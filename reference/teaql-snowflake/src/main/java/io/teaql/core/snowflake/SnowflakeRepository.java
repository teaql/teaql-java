package io.teaql.core.snowflake;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.Entity;
import io.teaql.core.RepositoryException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLRepository;

public class SnowflakeRepository<T extends Entity> extends SQLRepository<T> {
    public SnowflakeRepository(EntityDescriptor entityDescriptor, DataSource dataSource) {
        super(entityDescriptor, dataSource);
    }

    @Override
    protected void ensureIndexAndForeignKey(UserContext ctx) {
    }

    @Override
    protected String findTableColumnsSql(DataSource dataSource, String table) {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            String schemaName = connection.getSchema();
            return String.format(
                    "select * from information_schema.columns where table_name = '%s' and table_schema = '%s' and table_catalog = '%s'",
                    table.toUpperCase(), schemaName, databaseName);
        }
        catch (SQLException pE) {
            throw new RuntimeException(pE);
        }
    }



    @Override
    protected String getSQLForUpdateWhenPrepareId() {

        return "SELECT current_level from {} WHERE type_name = '{}'";
    }

    @Override
    protected String calculateDBType(Map<String, Object> columnInfo) {
        String dataType = ((String) columnInfo.get("data_type")).toLowerCase();
        switch (dataType) {
            case "bigint":
                return "bigint";
            case "tinyint":
            case "boolean":
                return "boolean";
            case "varchar":
            case "character varying":
                return StrUtil.format("varchar({})", columnInfo.get("character_maximum_length"));
            case "date":
                return "date";
            case "int":
            case "integer":
                return "integer";
            case "number":
                if (!"0".equals(columnInfo.get("numeric_scale"))) {
                    return StrUtil.format(
                            "numeric({},{})",
                            columnInfo.get("numeric_precision"),
                            columnInfo.get("numeric_scale"));
                }
                return "number";
            case "decimal":
            case "numeric":
                return StrUtil.format(
                        "numeric({},{})", columnInfo.get("numeric_precision"), columnInfo.get("numeric_scale"));
            case "text":
                Object maxLenObj = columnInfo.get("character_maximum_length");
                if (maxLenObj != null) {
                    String maxLen = maxLenObj.toString();
                    if (!"16777216".equals(maxLen)) {
                        return StrUtil.format("varchar({})", maxLen);
                    }
                }
                return "text";
            case "time without time zone":
                return "time";
            case "timestamp":
            case "timestamp_ntz":
            case "timestamp without time zone":
                return "timestamp";
            default:
                throw new RepositoryException("unsupported type:" + dataType);
        }
    }

    @Override
    protected void ensure(
            UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        List<Map<String, Object>> normalizedTableInfo = new ArrayList<>();
        for (Map<String, Object> column : tableInfo) {
            Map<String, Object> normalized = new HashMap<>();
            for (Map.Entry<String, Object> field : column.entrySet()) {
                if (field.getValue() != null) {
                    normalized.put(field.getKey().toLowerCase(), field.getValue().toString().toLowerCase());
                } else {
                    normalized.put(field.getKey().toLowerCase(), null);
                }
            }
            normalizedTableInfo.add(normalized);
        }
        super.ensure(ctx, normalizedTableInfo, table, columns);
    }
}
