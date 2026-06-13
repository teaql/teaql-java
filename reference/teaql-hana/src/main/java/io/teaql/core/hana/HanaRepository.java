package io.teaql.core.hana;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.RepositoryException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLRepository;

public class HanaRepository<T extends BaseEntity> extends SQLRepository<T> {

    public HanaRepository(EntityDescriptor entityDescriptor, DataSource dataSource) {
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
                    "select * from table_columns where table_name = '%s' and schema_name = '%s'",
                    table.toUpperCase(), schemaName);
        }
        catch (SQLException pE) {
            throw new RuntimeException(pE);
        }
    }

    @Override
    protected String generateAlterColumnSQL(UserContext ctx, SQLColumn column) {
        return StrUtil.format(
                "ALTER TABLE {} ALTER ({} {})",
                column.getTableName(),
                column.getColumnName(),
                column.getType());
    }

    @Override
    protected String calculateDBType(Map<String, Object> columnInfo) {
        String dataType = ((String) columnInfo.get("DATA_TYPE_NAME")).toLowerCase();
        switch (dataType) {
            case "bigint":
                return "bigint";
            case "tinyint":
            case "boolean":
                return "boolean";
            case "varchar":
            case "character varying":
                return StrUtil.format("varchar({})", columnInfo.get("LENGTH"));
            case "date":
                return "date";
            case "int":
            case "integer":
                return "integer";
            case "decimal":
            case "numeric":
                return StrUtil.format("numeric({},{})", columnInfo.get("LENGTH"), columnInfo.get("SCALE"));
            case "text":
                return "text";
            case "time without time zone":
                return "time";
            case "timestamp":
            case "timestamp without time zone":
                return "timestamp";
            default:
                throw new RepositoryException("unsupported type:" + dataType);
        }
    }
}
