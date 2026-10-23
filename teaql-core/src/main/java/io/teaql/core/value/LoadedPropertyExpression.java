package io.teaql.core.value;

import io.teaql.core.BaseEntity;
import io.teaql.core.checker.ObjectLocation;
import io.teaql.core.checker.JsonFieldNamingProfile;
import java.util.function.Function;

final class LoadedPropertyExpression<T, E extends BaseEntity, V> implements Expression<T, V> {
    private final Expression<T, E> parent;
    private final String property;
    private final Function<E, V> getter;
    private final String method;

    LoadedPropertyExpression(Expression<T, E> parent, String property, Function<E, V> getter) {
        this(parent, property, "get" + capitalize(JsonFieldNamingProfile.CAMEL_CASE.render(property)), getter);
    }

    LoadedPropertyExpression(Expression<T, E> parent, String property, String method, Function<E, V> getter) {
        this.parent = parent; this.property = property; this.method = method; this.getter = getter;
    }

    @Override
    public V eval(T root) {
        E entity;
        try { entity = parent.eval(root); }
        catch (TeaQLNotLoadedException error) { throw error.withAttempt($trace().operation("eval(<root>)")); }
        if (entity == null) return null;
        if (!entity.isPropertyLoaded(property)) {
            throw new TeaQLNotLoadedException(describeRoot(), $location(), property, $jsonFieldNaming()).withAttempt($trace().operation("eval(<root>)"));
        }
        return getter.apply(entity);
    }

    @Override public T $getRoot() { return parent.$getRoot(); }
    @Override public String $path() {
        return $location().modelPath();
    }
    @Override public ObjectLocation $location() {
        ObjectLocation base = parent.$location();
        return base == null ? ObjectLocation.hashRoot(property) : base.member(property);
    }
    @Override public JsonFieldNamingProfile $jsonFieldNaming() { return parent.$jsonFieldNaming(); }
    @Override public ExpressionTrace $trace() { return parent.$trace().member(property, method); }
    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
    private String describeRoot() {
        Object root = $getRoot();
        if (root instanceof BaseEntity entity) return entity.typeName() + "(id=" + entity.getId() + ")";
        return String.valueOf(root);
    }
}
