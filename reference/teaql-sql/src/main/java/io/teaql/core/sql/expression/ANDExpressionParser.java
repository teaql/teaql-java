package io.teaql.core.sql.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.AND;
import io.teaql.core.sql.SQLRepository;
import io.teaql.core.sql.SQLColumnResolver;
public class ANDExpressionParser implements SQLExpressionParser<AND> {

    @Override
    public Class<AND> type() {
        return AND.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            AND expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        List<Expression> expressions = expression.getExpressions();
        List<String> subs = new ArrayList<>();
        for (Expression sub : expressions) {
            String sql = ExpressionHelper.toSql(userContext, sub, idTable, parameters, sqlColumnResolver);
            if (SearchCriteria.FALSE.equalsIgnoreCase(sql)) {
                return SearchCriteria.FALSE;
            }
            if (SearchCriteria.TRUE.equalsIgnoreCase(sql)) {
                continue;
            }
            subs.add(sql);
        }
        return subs.stream().map(sub -> "(" + sub + ")").collect(Collectors.joining(" AND "));
    }
}
