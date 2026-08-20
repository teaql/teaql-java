package io.teaql.core.sql.expression;

import java.util.Map;

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
        String escapedName = sqlColumnResolver.escapeIdentifier(name);
        if (sql.equals(name) || sql.equals(escapedName)) {
            return sql;
        }
        return io.teaql.core.utils.StrUtil.format("{} AS {}", sql, escapedName);
    }
}
