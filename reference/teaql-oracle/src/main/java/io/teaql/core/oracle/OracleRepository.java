package io.teaql.core.oracle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import io.teaql.core.utils.LocalDateTimeUtil;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.Entity;
import io.teaql.core.RepositoryException;
import io.teaql.core.SearchRequest;
import io.teaql.core.Slice;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLRepository;

public class OracleRepository<T extends Entity> extends SQLRepository<T> {
    public OracleRepository(EntityDescriptor entityDescriptor, DataSource dataSource) {
        super(entityDescriptor, dataSource);
    }

    @Override
    protected void ensureIndexAndForeignKey(UserContext ctx) {
    }

    @Override
    public String getPartitionSQL() {

        return "SELECT * FROM (SELECT {}, (row_number() over(partition by {}{} {})) rank_value from {} {})  t where t.rank_value >= {} and t.rank_value < {}";
    }

    @Override
    protected String getSqlValue(Object value) {
        if (value instanceof LocalDateTime) {
            return StrUtil.format(
                    "TO_TIMESTAMP('{}', 'yyyy-mm-dd hh24:mi:ss')",
                    LocalDateTimeUtil.formatNormal((LocalDateTime) value));
        }
        if (value instanceof LocalDate) {
            return StrUtil.format(
                    "TO_DATE('{}', 'yyyy-mm-dd')", LocalDateTimeUtil.formatNormal((LocalDate) value));
        }
        return super.getSqlValue(value);
    }

    public String getIdSpaceSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ")
                .append(getTqlIdSpaceTable())
                .append(" (\n")
                .append("type_name varchar(100) PRIMARY KEY,\n")
                .append("current_level number(11))\n");
        String createIdSpaceSql = sb.toString();
        return createIdSpaceSql;
    }

    @Override
    protected String findTableColumnsSql(DataSource dataSource, String table) {
        return String.format(
                "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_PRECISION, DATA_SCALE, DATA_LENGTH FROM USER_TAB_COLUMNS where TABLE_NAME=UPPER('%s')",
                table);
    }

    @Override
    protected void ensure(
            UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        List<Map<String, Object>> lowercaseTableInfo = new ArrayList<>();
        for (Map<String, Object> column : tableInfo) {
            Map<String, Object> lowercaseColumn = new HashMap<>();
            for (Map.Entry<String, Object> field : column.entrySet()) {
                if (field.getValue() != null) {
                    lowercaseColumn.put(
                            field.getKey().toLowerCase(), field.getValue().toString().toLowerCase());
                } else {
                    lowercaseColumn.put(field.getKey().toLowerCase(), null);
                }
            }
            lowercaseTableInfo.add(lowercaseColumn);
        }
        super.ensure(ctx, lowercaseTableInfo, table, columns);
    }

    @Override
    protected String generateAddColumnSQL(UserContext ctx, String preColumnName, SQLColumn column) {
        return StrUtil.format(
                "ALTER TABLE {} ADD ({} {})",
                column.getTableName(),
                column.getColumnName(),
                column.getType());
    }

    @Override
    protected String generateAlterColumnSQL(UserContext ctx, SQLColumn column) {
        return StrUtil.format(
                "ALTER TABLE {} MODIFY ({} {})",
                column.getTableName(),
                column.getColumnName(),
                column.getType());
    }

    @Override
    protected String calculateDBType(Map<String, Object> columnInfo) {
        String dataType = (String) columnInfo.get("data_type");
        switch (dataType) {
            case "number":
                if ("0".equals(columnInfo.get("data_scale"))) {
                    return StrUtil.format("number({})", columnInfo.get("data_precision"));
                }
                return StrUtil.format(
                        "number({},{})", columnInfo.get("data_precision"), columnInfo.get("data_scale"));
            case "varchar2":
            case "varchar":
                return StrUtil.format("varchar({})", columnInfo.get("data_length"));
            case "date":
                return "date";
            case "clob":
                return "clob";
            case "timestamp":
            case "timestamp(6)":
            case "timestamp without time zone":
                return "timestamp";
            default:
                throw new RepositoryException("unsupported type:" + dataType);
        }
    }

    public String prepareLimit(SearchRequest request) {
        Slice slice = request.getSlice();
        if (ObjectUtil.isEmpty(slice)) {
            return null;
        }
        return StrUtil.format(
                "OFFSET {} ROWS FETCH NEXT {} ROWS ONLY", slice.getOffset(), slice.getSize());
    }
}
