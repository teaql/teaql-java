package io.teaql.core.value;

public class ValueExpression<T> implements Expression<T, T> {

    private final T value;

    public ValueExpression(T value) {
        this.value = value;
    }

    @Override
    public T eval(T pT) {
        return pT;
    }

    @Override
    public T $getRoot() {
        return value;
    }
}
