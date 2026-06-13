package io.teaql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.teaql.core.utils.CollUtil;
import io.teaql.core.utils.ListUtil;
import io.teaql.core.utils.ObjectUtil;

public class FunctionApply implements Expression {
    PropertyFunction operator;
    List<Expression> expressions;

    public FunctionApply(PropertyFunction operator, Expression... expressions) {
        if (ObjectUtil.isEmpty(expressions)) {
            throw new TeaQLRuntimeException("FunctionApply expressions cannot be empty");
        }
        this.operator = operator;
        this.expressions = new ArrayList<>(ListUtil.of(expressions));
    }

    @Override
    public List<String> properties(UserContext ctx) {
        List<String> ret = new ArrayList<>();

        for (Expression expression : expressions) {
            List<String> properties = expression.properties(ctx);
            if (properties != null) {
                ret.addAll(properties);
            }
        }
        return ret;
    }

    public PropertyFunction getOperator() {
        return operator;
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public Expression first() {
        return CollUtil.getFirst(expressions);
    }

    public Expression second() {
        return CollUtil.get(expressions, 1);
    }

    public Expression third() {
        return CollUtil.get(expressions, 2);
    }

    public Expression last() {
        return CollUtil.get(expressions, -1);
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof FunctionApply that)) return false;
        return Objects.equals(getOperator(), that.getOperator())
                && Objects.equals(getExpressions(), that.getExpressions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOperator(), getExpressions());
    }
}
