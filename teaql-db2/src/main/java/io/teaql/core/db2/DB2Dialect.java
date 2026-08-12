package io.teaql.core.db2;

import io.teaql.core.SearchRequest;
import io.teaql.core.Slice;
import io.teaql.core.sql.dialect.AbstractSqlDialect;
import io.teaql.core.utils.StrUtil;
import java.util.List;
import java.util.stream.Collectors;

public final class DB2Dialect extends AbstractSqlDialect {
    @Override
    public String prepareLimit(SearchRequest<?> request) {
        Slice slice = request.getSlice();
        if (slice == null) return null;
        return StrUtil.format("OFFSET {} ROWS FETCH NEXT {} ROWS ONLY", slice.getOffset(), slice.getSize());
    }

    @Override
    public String prepareParameterizedLimit(String limitPlaceholder, String offsetPlaceholder) {
        return "OFFSET " + offsetPlaceholder + " ROWS FETCH NEXT " + limitPlaceholder + " ROWS ONLY";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return needsEscape(identifier) ? "\"" + identifier.replace("\"", "\"\"") + "\"" : identifier;
    }

    @Override
    public String getPartitionSQL() {
        return "SELECT * FROM (SELECT {}, (row_number() over(partition by {}{} {})) as rank_ from {} {}) as t where t.rank_ >= {} and t.rank_ < {}";
    }

    @Override
    public String buildSubsidiaryInsertSql(String tableName, List<String> columns) {
        String names = columns.stream().map(this::escapeIdentifier).collect(Collectors.joining(", "));
        String values = StrUtil.repeatAndJoin("?", columns.size(), ", ");
        return StrUtil.format("INSERT INTO {} ({}) VALUES ({})", escapeIdentifier(tableName), names, values);
    }

    @Override
    public String mapColumnType(String type) {
        return "LARGE_TEXT".equalsIgnoreCase(type) ? "CLOB" : type;
    }
}
