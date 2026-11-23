package io.teaql.core.value;

import io.teaql.core.checker.ObjectLocation;
import io.teaql.core.checker.JsonFieldNamingProfile;
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
        Object eval;
        try { eval = expression.eval(pT); }
        catch (TeaQLNotLoadedException error) { throw error.withAttempt($trace().operation("eval(<root>)")); }
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

    @Override
    public String $path() { return expression.$path(); }

    @Override public ObjectLocation $location() { return expression.$location(); }
    @Override public JsonFieldNamingProfile $jsonFieldNaming() { return expression.$jsonFieldNaming(); }
    @Override public ExpressionTrace $trace() { return expression.$trace(); }
}
