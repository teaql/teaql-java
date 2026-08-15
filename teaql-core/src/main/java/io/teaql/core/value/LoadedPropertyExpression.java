package io.teaql.core.value;

import io.teaql.core.BaseEntity;
import java.util.function.Function;

final class LoadedPropertyExpression<T, E extends BaseEntity, V> implements Expression<T, V> {
    private final Expression<T, E> parent;
    private final String property;
    private final Function<E, V> getter;

    LoadedPropertyExpression(Expression<T, E> parent, String property, Function<E, V> getter) {
        this.parent = parent; this.property = property; this.getter = getter;
    }

    @Override
    public V eval(T root) {
        E entity = parent.eval(root);
        if (entity == null) return null;
        if (!entity.isPropertyLoaded(property)) {
            throw new TeaQLNotLoadedException(describeRoot(), $path(), property);
        }
        return getter.apply(entity);
    }

    @Override public T $getRoot() { return parent.$getRoot(); }
    @Override public String $path() {
        return parent.$path().isEmpty() ? property : parent.$path() + "." + property;
    }
    private String describeRoot() {
        Object root = $getRoot();
        if (root instanceof BaseEntity entity) return entity.typeName() + "(id=" + entity.getId() + ")";
        return String.valueOf(root);
    }
}
