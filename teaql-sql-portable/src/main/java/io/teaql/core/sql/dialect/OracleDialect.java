package io.teaql.core.sql.dialect;

import io.teaql.core.SearchRequest;
import io.teaql.core.Slice;
import io.teaql.core.utils.StrUtil;

public class OracleDialect extends AbstractSqlDialect {
    @Override
    public String prepareLimit(SearchRequest<?> request) {
        Slice slice = request.getSlice();
        if (slice == null) return null;
        return StrUtil.format("OFFSET {} ROWS FETCH NEXT {} ROWS ONLY", slice.getOffset(), slice.getSize());
    }

    @Override
    public String escapeIdentifier(String identifier) {
        if (!needsEscape(identifier)) {
            return identifier;
        }
        return "\"" + identifier + "\"";
    }

    @Override
    public String getPartitionSQL() {
        return "SELECT * FROM (SELECT {}, (row_number() over(partition by {}{} {})) as row_num from {} {}) t where t.row_num >= {} and t.row_num < {}";
    }

    @Override
    public String buildSubsidiaryInsertSql(String tableName, java.util.List<String> tableColumns) {
        throw new UnsupportedOperationException("Subsidiary insert not implemented for Oracle yet");
    }

    @Override
    public String mapColumnType(String type) {
        if ("LARGE_TEXT".equalsIgnoreCase(type)) {
            return "CLOB";
        }
        return type;
    }
}
