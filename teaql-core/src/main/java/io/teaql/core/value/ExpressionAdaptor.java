package io.teaql.core.value;

import java.util.function.Function;

public class ExpressionAdaptor<T, E, U> implements Expression<T, U> {
    private Expression expression;
    private Function function;

    public ExpressionAdaptor(Expression<T, E> pExpression, Function<E, U> pFunction) {
        expression = pExpression;
        function = pFunction;
    }

    public ExpressionAdaptor(Expression<T, U> pExpression) {
        expression = pExpression;
    }

    @Override
    public U eval(T pT) {
        Object eval = expression.eval(pT);
        if (eval == null) {
            return null;
        }

        if (function == null) {
            return (U) eval;
        }

        return (U) function.apply(eval);
    }

    @Override
    public T $getRoot() {
        return (T) expression.$getRoot();
    }
}
