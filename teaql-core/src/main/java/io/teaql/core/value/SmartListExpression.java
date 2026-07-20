package io.teaql.core.value;

import java.util.function.Function;

import io.teaql.core.BaseEntity;
import io.teaql.core.SmartList;

public class SmartListExpression<T, E, U extends BaseEntity>
        extends ExpressionAdaptor<T, E, SmartList<U>> {
    public SmartListExpression(Expression<T, E> pExpression, Function<E, SmartList<U>> pFunction) {
        super(pExpression, pFunction);
    }

    public SmartListExpression(Expression<T, SmartList<U>> pExpression) {
        super(pExpression);
    }

    public Expression<T, Integer> size() {
        return apply(list -> list.size());
    }

    public Expression<T, U> first() {
        return apply(list -> list.get(0));
    }

    public Expression<T, U> get(int index) {
        return apply(
                list -> {
                    if (index < 0 || index > list.size() - 1) {
                        return null;
                    }
                    return list.get(index);
                });
    }
}
