package io.teaql.core.sql.internal;

import io.teaql.core.AggrExpression;
import io.teaql.core.AggrFunction;
import io.teaql.core.Expression;
import io.teaql.core.TeaQLRuntimeException;
import io.teaql.core.UserContext;
import io.teaql.core.sql.SqlCompilerDelegate;

/** A bounded sort modifier over a selected result, not another source read. */
public record StatisticalOrderExpression(Expression original, Expression key, boolean gbk) {
    public static StatisticalOrderExpression parse(Expression expression) {
        if (expression == null) throw invalid("Statistical order requires an expression");
        Expression key = selfOperand(expression);
        boolean gbk = key instanceof AggrExpression aggregate && aggregate.getOperator() == AggrFunction.GBK;
        if (gbk) {
            var aggregate = (AggrExpression) key;
            if (aggregate.getExpressions().size() != 1) throw invalid("GBK expects exactly one argument");
            key = selfOperand(aggregate.first());
        }
        return new StatisticalOrderExpression(expression, key, gbk);
    }

    public boolean matches(Expression selected) {
        return original.equals(selected) || key.equals(selfOperand(selected));
    }

    public String sql(SqlCompilerDelegate owner, UserContext context, Expression selected, String sql) {
        return gbk ? owner.gbkOrderSql(context, selected, sql) : owner.statisticalOrderSql(context, selected, sql);
    }

    private static Expression selfOperand(Expression expression) {
        for (int depth=0;depth<64;depth++) {
            if (!(expression instanceof AggrExpression aggregate) || aggregate.getOperator() != AggrFunction.SELF)
                return expression;
            if (aggregate.getExpressions().size() != 1) throw invalid("SELF expects exactly one argument");
            expression = aggregate.first();
        }
        throw invalid("Statistical order exceeds expression depth limit");
    }

    private static TeaQLRuntimeException invalid(String message) { return new TeaQLRuntimeException(message); }
}
