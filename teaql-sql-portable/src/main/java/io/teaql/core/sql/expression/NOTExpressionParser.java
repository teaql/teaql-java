package io.teaql.core.sql.expression;

import java.util.List;
import java.util.Map;

import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.NOT;

import io.teaql.core.sql.SQLColumnResolver;
public class NOTExpressionParser implements SQLExpressionParser<NOT> {

    @Override
    public Class<NOT> type() {
        return NOT.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            NOT expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        List<Expression> expressions = expression.getExpressions();
        Expression sub = CollectionUtil.getFirst(expressions);
        if (sub == null) {
            return SearchCriteria.TRUE;
        }
        String subSql =
                ExpressionHelper.toSql(userContext, sub, idTable, parameters, sqlColumnResolver);
        if (SearchCriteria.TRUE.equalsIgnoreCase(subSql)) {
            return SearchCriteria.FALSE;
        }

        if (SearchCriteria.FALSE.equalsIgnoreCase(subSql)) {
            return SearchCriteria.TRUE;
        }
        return StrUtil.format("NOT ({})", subSql);
    }
}
