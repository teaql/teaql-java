package io.teaql.core.sql.expression;

import java.util.List;
import java.util.Map;

import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.Expression;
import io.teaql.core.PropertyFunction;
import io.teaql.core.RepositoryException;
import io.teaql.core.UserContext;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import io.teaql.core.sql.SQLRepository;
import io.teaql.core.sql.SQLColumnResolver;
public class TwoOperatorExpressionParser implements SQLExpressionParser<TwoOperatorCriteria> {
    @Override
    public Class<TwoOperatorCriteria> type() {
        return TwoOperatorCriteria.class;
    }

    @Override
    public String toSql(
            UserContext userContext,
            TwoOperatorCriteria twoOperatorCriteria,
            String idTable,
            Map<String, Object> parameters,
            SQLColumnResolver sqlColumnResolver) {
        List<Expression> expressions = twoOperatorCriteria.getExpressions();
        PropertyFunction operator = twoOperatorCriteria.getOperator();
        if (!(operator instanceof Operator)) {
            throw new RepositoryException("unsupported operator:" + operator);
        }
        if (CollectionUtil.size(expressions) != 2) {
            throw new RepositoryException(operator + " should have 2 expressions");
        }
        Expression left = twoOperatorCriteria.first();
        Expression right = twoOperatorCriteria.second();
        String leftSQL =
                ExpressionHelper.toSql(userContext, left, idTable, parameters, sqlColumnResolver);
        String rightSQL =
                ExpressionHelper.toSql(userContext, right, idTable, parameters, sqlColumnResolver);
        return StrUtil.format(
                "{} {} {}{}{}",
                leftSQL,
                getOp((Operator) operator),
                getPrefix((Operator) operator),
                rightSQL,
                getSuffix((Operator) operator));
    }

    public Object getSuffix(Operator operator) {
        switch (operator) {
            case IN:
            case NOT_IN:
            case IN_LARGE:
            case NOT_IN_LARGE:
                return ")";
            default:
                return "";
        }
    }

    public Object getPrefix(Operator operator) {
        switch (operator) {
            case IN:
            case NOT_IN:
            case IN_LARGE:
            case NOT_IN_LARGE:
                return "(";
            default:
                return "";
        }
    }

    public String getOp(Operator operator) {
        switch (operator) {
            case EQUAL:
                return "=";
            case NOT_EQUAL:
                return "<>";
            case CONTAIN:
            case BEGIN_WITH:
            case END_WITH:
                return "LIKE";
            case NOT_CONTAIN:
            case NOT_BEGIN_WITH:
            case NOT_END_WITH:
                return "NOT LIKE";
            case GREATER_THAN:
                return ">";
            case GREATER_THAN_OR_EQUAL:
                return ">=";
            case LESS_THAN:
                return "<";
            case LESS_THAN_OR_EQUAL:
                return "<=";
            case IN:
                return "IN";
            case IN_LARGE:
                return "= ANY";
            case NOT_IN:
                return "NOT IN";
            case NOT_IN_LARGE:
                return "<> ALL";
            default:
                throw new RepositoryException("unsupported operator:" + operator);
        }
    }
}
