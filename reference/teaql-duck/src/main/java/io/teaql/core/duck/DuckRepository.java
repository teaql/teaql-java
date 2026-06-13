package io.teaql.core.duck;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.Entity;
import io.teaql.core.RepositoryException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.sql.SQLRepository;

public class DuckRepository<T extends Entity> extends SQLRepository<T> {
    public DuckRepository(EntityDescriptor entityDescriptor, DataSource dataSource) {
        super(entityDescriptor, dataSource);
    }

    @Override
    protected String findTableColumnsSql(DataSource dataSource, String table) {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            String schemaName = connection.getSchema();
            return String.format(
                    "select * from information_schema.columns where table_name = '%s' and table_schema = '%s'",
                    table, schemaName);
        }
        catch (SQLException pE) {
            throw new RuntimeException(pE);
        }
    }

    @Override
    protected void ensureIndexAndForeignKey(UserContext ctx) {

    }

    @Override
    protected String calculateDBType(Map<String, Object> columnInfo) {
        String dataType = (String) columnInfo.get("data_type");
        int index = dataType.indexOf("(");
        if (index != -1) {
            dataType = dataType.substring(0, index).trim().toLowerCase();
        }
        else {
            dataType = dataType.toLowerCase();
        }
        return switch (dataType) {
            case "bigint" -> "bigint";
            case "tinyint", "boolean" -> "boolean";
            case "varchar", "character varying" -> "varchar";
            case "date" -> "date";
            case "int", "integer" -> "integer";
            case "decimal", "numeric" -> StrUtil.format(
                    "numeric({},{})", columnInfo.get("numeric_precision"), columnInfo.get("numeric_scale"));
            case "text" -> "text";
            case "time without time zone" -> "time";
            case "timestamp", "timestamp without time zone" -> "timestamp";
            default -> throw new RepositoryException("unsupported type:" + dataType);
        };
    }

    protected boolean isTypeMatch(String dbType, String type) {
        if (dbType.equalsIgnoreCase(type)) {
            return true;
        }
        return dbType.equalsIgnoreCase("varchar") && type.toLowerCase().startsWith("varchar(");
    }
}
