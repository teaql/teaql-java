package io.teaql.core;

import java.util.Collection;
import java.util.stream.Stream;

import io.teaql.core.utils.ListUtil;
import io.teaql.core.utils.IdUtil;
import io.teaql.core.utils.ObjectUtil;

import io.teaql.core.meta.EntityDescriptor;

public interface Repository<T extends Entity> {

    EntityDescriptor getEntityDescriptor();

    Collection<T> save(UserContext userContext, Collection<T> entities);

    SmartList<T> executeForList(UserContext userContext, SearchRequest<T> request);

    default Stream<T> executeForStream(UserContext userContext, SearchRequest<T> request) {
        return executeForStream(userContext, request, 1000);
    }

    Stream<T> executeForStream(
            UserContext userContext, SearchRequest<T> request, int enhanceBatchSize);

    AggregationResult aggregation(UserContext userContext, SearchRequest<T> request);

    default Long prepareId(UserContext userContext, T entity) {
        if (entity.getId() != null) {
            return entity.getId();
        }

        if (userContext instanceof DefaultUserContext) {
            Long id = ((DefaultUserContext) userContext).generateId(entity);
            if (id != null) {
                return id;
            }
        }

        return IdUtil.getSnowflakeNextId();
    }

    default Entity save(UserContext userContext, T entity) {
        if (entity == null) {
            return null;
        }
        save(userContext, ListUtil.of(entity));
        return entity;
    }

    default void delete(UserContext userContext, T entity) {
        if (ObjectUtil.isEmpty(entity)) {
            return;
        }
        delete(userContext, ListUtil.of(entity));
    }

    default void delete(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isNotEmpty(entities)) {
            for (T entity : entities) {
                entity.markAsDeleted();
            }
        }
        save(userContext, entities);
    }

    default void recover(UserContext userContext, T entity) {
        if (ObjectUtil.isEmpty(entity)) {
            return;
        }
        recover(userContext, ListUtil.of(entity));
    }

    default void recover(UserContext userContext, Collection<T> entities) {
        if (ObjectUtil.isNotEmpty(entities)) {
            for (T entity : entities) {
                entity.markAsRecover();
            }
        }
        save(userContext, entities);
    }

    default T executeForOne(UserContext userContext, SearchRequest<T> request) {
        if (request instanceof BaseRequest) {
            ((BaseRequest<T>) request).top(1);
        }
        return executeForList(userContext, request).first();
    }
}
