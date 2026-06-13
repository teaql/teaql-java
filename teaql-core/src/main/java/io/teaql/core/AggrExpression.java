package io.teaql.core;

public class AggrExpression extends FunctionApply {
    public AggrExpression(AggrFunction operator, Expression expression) {
        super(operator, expression);
    }
}
