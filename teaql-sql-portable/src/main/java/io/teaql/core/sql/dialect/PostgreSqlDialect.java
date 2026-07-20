package io.teaql.core.sql.dialect;

import io.teaql.core.SearchRequest;
import io.teaql.core.Slice;
import io.teaql.core.utils.ObjectUtil;
import io.teaql.core.utils.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

public class PostgreSqlDialect extends AbstractSqlDialect {

    @Override
    public String escapeIdentifier(String identifier) {
        if (!needsEscape(identifier)) {
            return identifier;
        }
        return "\"" + identifier + "\"";
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
        // Note: PostgreSQL actually uses standard window function syntax which is very similar,
        // but we might need different pagination wrapping. For now, matching the base format.
        return "SELECT * FROM (SELECT {}, (row_number() over(partition by {}{} {})) as _rank from {} {}) as t where t._rank >= {} and t._rank < {}";
    }

    @Override
    public String buildSubsidiaryInsertSql(String tableName, List<String> tableColumns) {
        String columnsStr = tableColumns.stream().map(this::escapeIdentifier).collect(Collectors.joining(", "));
        String valuesStr = StrUtil.repeatAndJoin("?", tableColumns.size(), ", ");
        String updateSetStr = tableColumns.stream()
                .filter(c -> !c.equalsIgnoreCase("id"))
                .map(c -> escapeIdentifier(c) + " = EXCLUDED." + escapeIdentifier(c))
                .collect(Collectors.joining(", "));

        if (updateSetStr.isEmpty()) {
            return StrUtil.format("INSERT INTO {} ({}) VALUES ({}) ON CONFLICT ({}) DO NOTHING",
                    escapeIdentifier(tableName), columnsStr, valuesStr, escapeIdentifier("id"));
        }
        return StrUtil.format("INSERT INTO {} ({}) VALUES ({}) ON CONFLICT ({}) DO UPDATE SET {}",
                escapeIdentifier(tableName), columnsStr, valuesStr, escapeIdentifier("id"), updateSetStr);
    }

    @Override
    public String mapColumnType(String type) {
        if (type != null && type.contains("<max>")) {
            return type.replace("VARCHAR(<max>)", "TEXT").replace("<max>", "255");
        }
        return type;
    }
}
