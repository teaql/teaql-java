package io.teaql.core.value;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;

public interface BaseEntityExpression<T, U extends BaseEntity> extends Expression<T, U> {
    default Expression<T, Long> getId() {
        return apply(BaseEntity::getId);
    }

    default Expression<T, Long> getVersion() {
        return apply(BaseEntity::getVersion);
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
                    entity.internalSet("id", id);
                    return entity;
                });
    }
}
