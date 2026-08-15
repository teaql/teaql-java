package io.teaql.core.value;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import java.util.function.Function;

public interface BaseEntityExpression<T, U extends BaseEntity> extends Expression<T, U> {
    default Expression<T, Long> getId() {
		return loaded(BaseEntity.ID_PROPERTY, BaseEntity::getId);
    }

    default Expression<T, Long> getVersion() {
		return loaded(BaseEntity.VERSION_PROPERTY, BaseEntity::getVersion);
    }

    default <V> Expression<T, V> loaded(String property, Function<U, V> getter) {
		return new LoadedPropertyExpression<>(this, property, getter);
	}

    default Expression<T, U> save(UserContext userContext) {
        return apply(entity -> {
            entity.auditAs("save via expression").save(userContext);
            return entity;
        });
    }

    default Expression<T, U> updateId(Long id) {
        return apply(
                entity -> {
                    entity.__internalSet("id", id);
                    return entity;
                });
    }
}
