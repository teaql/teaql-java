package io.teaql.core.sql.expression;

import java.util.List;
import java.util.Map;

import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.Expression;
import io.teaql.core.PropertyFunction;
import io.teaql.core.RepositoryException;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.OneOperatorCriteria;
import io.teaql.core.criteria.Operator;
import io.teaql.core.sql.SQLRepository;
import io.teaql.core.sql.SQLColumnResolver;
public class OneOperatorExpressionParser implements SQLExpressionParser<OneOperatorCriteria> {
    @Override
    public Class<OneOperatorCriteria> type() {
        return OneOperatorCriteria.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            OneOperatorCriteria criteria,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        List<Expression> expressions = criteria.getExpressions();
        PropertyFunction operator = criteria.getOperator();
        if (!(operator instanceof Operator)) {
            throw new RepositoryException("unsupported operator:" + operator);
        }
        if (CollectionUtil.size(expressions) != 1) {
            throw new RepositoryException(operator + " should have one expression");
        }
        Expression left = expressions.get(0);
        String leftSQL =
                ExpressionHelper.toSql(userContext, left, idTable, parameters, sqlColumnResolver);
        return StrUtil.format("{} {}", leftSQL, getOp((Operator) operator));
    }

    private Object getOp(Operator operator) {
        switch (operator) {
            case IS_NULL:
                return "IS NULL";
            case IS_NOT_NULL:
                return "IS NOT NULL";
            default:
                throw new RepositoryException("unsupported operator:" + operator);
        }
    }
}
