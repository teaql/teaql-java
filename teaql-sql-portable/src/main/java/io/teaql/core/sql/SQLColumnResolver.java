package io.teaql.core.sql;

import java.util.List;
import java.util.Map;

import io.teaql.core.SearchRequest;
import io.teaql.core.UserContext;
import io.teaql.core.utils.CollUtil;
import io.teaql.core.sql.expression.SQLExpressionParser;

public interface SQLColumnResolver {

    default SQLColumn getPropertyColumn(String idTable, String property) {
        return CollUtil.getFirst(getPropertyColumns(idTable, property));
    }

    List<SQLColumn> getPropertyColumns(String idTable, String property);

    default Map<Class, SQLExpressionParser> getExpressionParsers() {
        return java.util.Collections.emptyMap();
    }

    default boolean canMixinSubQuery(UserContext userContext, SearchRequest subQuery) {
        return false;
    }

    default String escapeIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty() || "*".equals(identifier)) {
            return identifier;
        }
        if (identifier.startsWith("`") || identifier.startsWith("\"") || identifier.startsWith("[")) {
            return identifier;
        }
        return "`" + identifier + "`";
    }
}
