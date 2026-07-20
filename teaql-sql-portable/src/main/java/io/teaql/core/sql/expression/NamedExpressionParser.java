package io.teaql.core.sql.expression;

import java.util.Map;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.Expression;
import io.teaql.core.SimpleNamedExpression;
import io.teaql.core.UserContext;

import io.teaql.core.sql.SQLColumnResolver;
public class NamedExpressionParser implements SQLExpressionParser<SimpleNamedExpression> {
    @Override
    public Class<SimpleNamedExpression> type() {
        return SimpleNamedExpression.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            SimpleNamedExpression expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        Expression inner = expression.getExpression();
        String sql = ExpressionHelper.toSql(userContext, inner, idTable, parameters, sqlColumnResolver);
        String name = expression.name();
        if (!name.toLowerCase().equals(name)) {
            name = StrUtil.wrap(name, "\"");
        }
        if (sql.equals(name)) {
            return sql;
        }
        return StrUtil.format("{} AS {}", sql, name);
    }
}
