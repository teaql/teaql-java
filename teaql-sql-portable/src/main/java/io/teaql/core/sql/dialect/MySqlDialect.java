package io.teaql.core.sql.dialect;

import io.teaql.core.SearchRequest;
import io.teaql.core.Slice;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

public class MySqlDialect extends AbstractSqlDialect {

    @Override
    public String escapeIdentifier(String identifier) {
        if (!needsEscape(identifier)) {
            return identifier;
        }
        return "`" + identifier + "`";
    }

    @Override
    public String prepareLimit(SearchRequest<?> request) {
        Slice slice = request.getSlice();
        if (ObjectUtil.isEmpty(slice)) {
            return null;
        }
        return StrUtil.format("LIMIT {} OFFSET {}", slice.getSize(), slice.getOffset());
    }

    @Override
    public String getPartitionSQL() {
        return "SELECT * FROM (SELECT {}, (row_number() over(partition by {}{} {})) as _rank from {} {}) as t where t._rank >= {} and t._rank < {}";
    }

    @Override
    public String buildSubsidiaryInsertSql(String tableName, List<String> tableColumns) {
        return StrUtil.format(
                "REPLACE INTO {} SET {}",
                escapeIdentifier(tableName),
                tableColumns.stream().map(c -> escapeIdentifier(c) + " = ?").collect(Collectors.joining(" , ")));
    }

    @Override
    public String mapColumnType(String type) {
        if (type != null && type.contains("<max>")) {
            return type.replace("VARCHAR(<max>)", "LONGTEXT").replace("<max>", "65535");
        }
        return type;
    }
}
