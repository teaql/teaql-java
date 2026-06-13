package io.teaql.core.db2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.RepositoryException;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLRepository;

public class DB2Repository<T extends BaseEntity> extends SQLRepository<T> {
    public DB2Repository(EntityDescriptor entityDescriptor, DataSource dataSource) {
        super(entityDescriptor, dataSource);
    }

    protected String wrapColumnStatementForCreatingTable(UserContext ctx, String table, SQLColumn column) {

        String dbColumn = column.getColumnName() + " " + column.getType();
        if (column.isIdColumn()) {
            dbColumn = dbColumn + " PRIMARY KEY NOT NULL";
        }
        return dbColumn;
    }

    public String getIdSpaceSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ")
                .append(getTqlIdSpaceTable())
                .append(" (\n")
                .append("type_name varchar(100) PRIMARY KEY NOT NULL,\n")
                .append("current_level bigint)\n");
        String createIdSpaceSql = sb.toString();
        return createIdSpaceSql;
    }

    @Override
    protected String findTableColumnsSql(DataSource dataSource, String table) {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            String schemaName = connection.getSchema();
            return String.format(
                    "select * from sysibm.syscolumns WHERE tbcreator = '%s' AND tbname = '%s'",
                    schemaName, table.toUpperCase());
        }
        catch (SQLException pE) {
            throw new RuntimeException(pE);
        }
    }

    protected String getSchemaColumnNameFieldName() {
        return "name";
    }

    @Override
    protected String generateAlterColumnSQL(UserContext ctx, SQLColumn column) {
        return StrUtil.format(
                "ALTER TABLE {} ALTER COLUMN {} SET DATA TYPE {}",
                column.getTableName(),
                column.getColumnName(),
                column.getType());
    }

    protected String calculateDBType(Map<String, Object> columnInfo) {
        String dataType = ((String) columnInfo.get("coltype")).toLowerCase().trim();
        switch (dataType) {
            case "bigint":
                return "bigint";
            case "tinyint":
            case "boolean":
                return "boolean";
            case "varchar":
            case "character varying":
                return StrUtil.format("varchar({})", columnInfo.get("length"));
            case "date":
                return "date";
            case "int":
            case "integer":
                return "integer";
            case "decimal":
            case "numeric":
                return StrUtil.format(
                        "decimal({},{})", columnInfo.get("length"), columnInfo.get("scale"));
            case "text":
                return "text";
            case "clob":
                return "clob";
            case "time without time zone":
                return "time";
            case "timestamp":
            case "timestmp":
            case "timestamp without time zone":
                return "timestamp";
            default:
                throw new RepositoryException("unsupported type:" + dataType);
        }
    }

    protected Map<String, Map<String, Object>> getFields(List<Map<String, Object>> tableInfo) {
        return CollStreamUtil.toIdentityMap(
                tableInfo, m -> String.valueOf(m.get(getSchemaColumnNameFieldName())).toLowerCase());
    }

    @Override
    protected void ensureIndexAndForeignKey(UserContext ctx) {
    }
}
