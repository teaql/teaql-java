package io.teaql.core.value;

import java.util.function.Function;

import io.teaql.core.BaseEntity;
import io.teaql.core.SmartList;
import io.teaql.core.checker.ObjectLocation;

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
		return get(0);
    }

    public Expression<T, U> get(int index) {
        return new ExpressionAdaptor<T, SmartList<U>, U>(this,
                list -> {
                    if (index < 0 || index > list.size() - 1) {
                        return null;
                    }
                    return list.get(index);
                }) {
            @Override public ObjectLocation $location() {
                ObjectLocation parent = SmartListExpression.this.$location();
                return parent == null ? ObjectLocation.arrayRoot(index) : parent.element(index);
            }
            @Override public String $path() { return $location().modelPath(); }
        };
    }
}
