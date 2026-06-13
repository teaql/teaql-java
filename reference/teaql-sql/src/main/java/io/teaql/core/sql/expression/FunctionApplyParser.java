package io.teaql.core.sql.expression;

import java.util.Map;

import io.teaql.core.utils.StrUtil;

import io.teaql.core.FunctionApply;
import io.teaql.core.PropertyFunction;
import io.teaql.core.RepositoryException;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.SQLRepository;
import io.teaql.core.sql.SQLColumnResolver;
public class FunctionApplyParser implements SQLExpressionParser<FunctionApply> {
    @Override
    public Class<FunctionApply> type() {
        return FunctionApply.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            FunctionApply expression,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        PropertyFunction operator = expression.getOperator();
        if (operator == Operator.SOUNDS_LIKE) {
            return StrUtil.format(
                    "SOUNDEX({})",
                    ExpressionHelper.toSql(
                            userContext, expression.first(), idTable, parameters, sqlColumnResolver));
        }
        throw new RepositoryException("unexpected operator:" + operator);
    }
}
