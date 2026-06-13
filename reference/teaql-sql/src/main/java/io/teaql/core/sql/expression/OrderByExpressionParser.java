package io.teaql.core.sql.expression;

import java.util.Map;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.OrderBy;
import io.teaql.core.UserContext;
import io.teaql.core.sql.SQLRepository;
import io.teaql.core.sql.SQLColumnResolver;
public class OrderByExpressionParser implements SQLExpressionParser<OrderBy> {

    @Override
    public Class<OrderBy> type() {
        return OrderBy.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            OrderBy expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        return StrUtil.format(
                "{} {}",
                ExpressionHelper.toSql(
                        userContext, expression.getExpression(), idTable, parameters, sqlColumnResolver),
                expression.getDirection());
    }
}
