package io.teaql.core.sql.expression;

import java.util.List;
import java.util.Map;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.Expression;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.Between;
import io.teaql.core.sql.SQLRepository;
import io.teaql.core.sql.SQLColumnResolver;
public class BetweenParser implements SQLExpressionParser<Between> {
    @Override
    public Class<Between> type() {
        return Between.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            Between expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        List<Expression> expressions = expression.getExpressions();
        Expression property = expressions.get(0);
        Expression lowValue = expressions.get(1);
        Expression highValue = expressions.get(2);
        return StrUtil.format(
                "{} BETWEEN {} AND {}",
                ExpressionHelper.toSql(userContext, property, idTable, parameters, sqlColumnResolver),
                ExpressionHelper.toSql(userContext, lowValue, idTable, parameters, sqlColumnResolver),
                ExpressionHelper.toSql(userContext, highValue, idTable, parameters, sqlColumnResolver));
    }
}
