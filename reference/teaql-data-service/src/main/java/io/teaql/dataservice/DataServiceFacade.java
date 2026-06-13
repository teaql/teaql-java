package io.teaql.coreservice;

import io.teaql.core.AggregationResult;
import io.teaql.core.Entity;
import io.teaql.core.Repository;
import io.teaql.core.SearchRequest;
import io.teaql.core.SmartList;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;

import java.util.Collection;
import java.util.stream.Stream;

public class DataServiceFacade<T extends Entity> implements Repository<T> {

    private final EntityDescriptor entityDescriptor;
    private final DataServiceRegistry registry;

    public DataServiceFacade(EntityDescriptor entityDescriptor, DataServiceRegistry registry) {
        this.entityDescriptor = entityDescriptor;
        this.registry = registry;
    }

    @Override
    public EntityDescriptor getEntityDescriptor() {
        return entityDescriptor;
    }

    private String getTargetDataService() {
        String ds = entityDescriptor.getDataService();
        return ds != null ? ds : "sql";
    }

    @Override
    public T executeForOne(UserContext userContext, SearchRequest<T> request) {
        QueryExecutor executor = registry.resolveQueryExecutor(getTargetDataService());
        if (executor != null && executor.capabilities().isQuery()) {
            // Future: map SearchRequest to QueryRequest and invoke executor
            // QueryResult result = executor.query(userContext, adapt(request));
            throw new UnsupportedOperationException("DataService executor routing not fully implemented yet");
        }
        throw new IllegalStateException("No query executor found for data service: " + getTargetDataService());
    }

    @Override
    public SmartList<T> executeForList(UserContext userContext, SearchRequest<T> request) {
        QueryExecutor executor = registry.resolveQueryExecutor(getTargetDataService());
        if (executor != null && executor.capabilities().isQuery()) {
            throw new UnsupportedOperationException("DataService executor routing not fully implemented yet");
        }
        throw new IllegalStateException("No query executor found for data service: " + getTargetDataService());
    }

    @Override
    public Stream<T> executeForStream(UserContext userContext, SearchRequest<T> request) {
        QueryExecutor executor = registry.resolveQueryExecutor(getTargetDataService());
        if (executor != null && executor.capabilities().isStreamingQuery()) {
            throw new UnsupportedOperationException("DataService executor streaming not fully implemented yet");
        }
        throw new IllegalStateException("No streaming query executor found for data service: " + getTargetDataService());
    }

    @Override
    public Stream<T> executeForStream(UserContext userContext, SearchRequest<T> request, int enhanceBatch) {
        return executeForStream(userContext, request);
    }

    @Override
    public AggregationResult aggregation(UserContext userContext, SearchRequest<T> request) {
        QueryExecutor executor = registry.resolveQueryExecutor(getTargetDataService());
        if (executor != null && executor.capabilities().isAggregation()) {
            throw new UnsupportedOperationException("DataService executor aggregation not fully implemented yet");
        }
        throw new IllegalStateException("No aggregation executor found for data service: " + getTargetDataService());
    }

    @Override
    public Collection<T> save(UserContext userContext, Collection<T> entities) {
        MutationExecutor executor = registry.resolveMutationExecutor(getTargetDataService());
        if (executor != null && executor.capabilities().isMutation()) {
            throw new UnsupportedOperationException("DataService executor mutation not fully implemented yet");
        }
        throw new IllegalStateException("No mutation executor found for data service: " + getTargetDataService());
    }

    @Override
    public void delete(UserContext userContext, Collection<T> entities) {
        MutationExecutor executor = registry.resolveMutationExecutor(getTargetDataService());
        if (executor != null && executor.capabilities().isMutation()) {
            throw new UnsupportedOperationException("DataService executor mutation not fully implemented yet");
        }
        throw new IllegalStateException("No mutation executor found for data service: " + getTargetDataService());
    }

    @Override
    public void delete(UserContext userContext, T entity) {
        delete(userContext, java.util.Collections.singletonList(entity));
    }

    @Override
    public void recover(UserContext userContext, Collection<T> entities) {
        MutationExecutor executor = registry.resolveMutationExecutor(getTargetDataService());
        if (executor != null && executor.capabilities().isMutation()) {
            throw new UnsupportedOperationException("DataService executor mutation not fully implemented yet");
        }
        throw new IllegalStateException("No mutation executor found for data service: " + getTargetDataService());
    }
}
