package io.teaql.core.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import io.teaql.utils.reflect.ReflectUtil;

import io.teaql.core.AggrExpression;
import io.teaql.core.AggregationResult;
import io.teaql.core.Aggregations;
import io.teaql.core.BaseEntity;
import io.teaql.core.Expression;
import io.teaql.core.PropertyFunction;
import io.teaql.core.PropertyReference;
import io.teaql.core.SearchRequest;
import io.teaql.core.SimpleNamedExpression;
import io.teaql.core.SmartList;
import io.teaql.core.TQLException;
import io.teaql.core.UserContext;
import io.teaql.core.DefaultUserContext;
import io.teaql.core.memory.filter.CriteriaFilter;
import io.teaql.core.memory.filter.Filter;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.repository.AbstractRepository;

public class MemoryRepository<T extends BaseEntity> extends AbstractRepository<T> {

    private EntityDescriptor entityDescriptor;

    private Filter<T> filter = new CriteriaFilter<>();

    private List<T> dataSet = new ArrayList<>();

    private AtomicLong max = new AtomicLong(1);

    public MemoryRepository(EntityDescriptor pEntityDescriptor) {
        entityDescriptor = pEntityDescriptor;
    }

    @Override
    public EntityDescriptor getEntityDescriptor() {
        return entityDescriptor;
    }

    @Override
    protected void updateInternal(UserContext ctx, Collection<T> items) {
        throw new TQLException("unsupported update");
    }

    @Override
    protected void createInternal(UserContext ctx, Collection<T> items) {
        throw new TQLException("unsupported save");
    }

    @Override
    protected void deleteInternal(UserContext userContext, Collection<T> deleteItems) {
        throw new TQLException("unsupported delete");
    }

    @Override
    protected void recoverInternal(UserContext userContext, Collection<T> recoverItems) {
        throw new TQLException("unsupported recover");
    }

    @Override
    protected SmartList<T> loadInternal(UserContext userContext, SearchRequest<T> request) {
        List<T> dataSet = getDataSet(userContext, request);
        SmartList<T> result = new SmartList<>();
        if (dataSet != null) {
            for (T t : dataSet) {
                if (filter.accept(t, request.getSearchCriteria())) {
                    result.add(copy(t, request));
                }
            }
        }
        return result;
    }

    @Override
    public Long prepareId(UserContext userContext, T entity) {
        if (entity.getId() != null) {
            return entity.getId();
        }

        if (userContext instanceof DefaultUserContext) {
            Long id = ((DefaultUserContext) userContext).generateId(entity);
            if (id != null) {
                return id;
            }
        }

        return max.getAndIncrement();
    }

    private T copy(T entity, SearchRequest<T> request) {
        Class<? extends T> returnType = request.returnType();
        T result = ReflectUtil.newInstance(returnType);
        List<SimpleNamedExpression> projections = request.getProjections();
        for (SimpleNamedExpression projection : projections) {
            String name = projection.name();
            Expression expression = projection.getExpression();
            if (expression instanceof PropertyReference p) {
                Object value = entity.getProperty(p.getPropertyName());
                result.setProperty(name, value);
            }
        }
        return result;
    }

    protected List<T> getDataSet(UserContext userContext, SearchRequest<T> request) {
        return dataSet;
    }

    @Override
    protected AggregationResult doAggregateInternal(
            UserContext userContext, SearchRequest<T> request) {
        Aggregations aggregations = request.getAggregations();
        if (!request.hasSimpleAgg()) {
            return null;
        }

        List<T> dataSet = getDataSet(userContext, request);
        if (dataSet != null) {
            for (T t : dataSet) {
                if (filter.accept(t, request.getSearchCriteria())) {
                }
            }
        }
        List<SimpleNamedExpression> aggregates = aggregations.getAggregates();

        for (SimpleNamedExpression aggregate : aggregates) {
            Expression expression = aggregate.getExpression();
            if (expression instanceof AggrExpression agg) {
                PropertyFunction operator = agg.getOperator();
            }
        }
        return null;
    }
}
