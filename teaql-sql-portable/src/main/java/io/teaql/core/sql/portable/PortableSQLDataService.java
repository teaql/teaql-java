package io.teaql.core.sql.portable;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.runtime.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PortableSQLDataService implements DataServiceExecutor, QueryExecutor, MutationExecutor, TransactionExecutor {

    private final String name;
    private final DataServiceCapabilities capabilities;
    private final TeaQLDatabase database;
    private final EntityMetaFactory metadata;
    private final Map<String, PortableSQLRepository<?>> repositories = new ConcurrentHashMap<>();
    private final PortableSQLRepository.PortableSQLRepositoryResolver resolver = this::getRepository;

    public PortableSQLDataService(String name, TeaQLDatabase database, EntityMetaFactory metadata) {
        this.name = name;
        this.database = database;
        this.metadata = metadata;
        this.capabilities = new DataServiceCapabilities();
        this.capabilities.setQuery(true);
        this.capabilities.setMutation(true);
        this.capabilities.setTransaction(true);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public DataServiceCapabilities capabilities() {
        return capabilities;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> PortableSQLRepository<T> getRepository(String typeName) {
        return (PortableSQLRepository<T>) repositories.computeIfAbsent(typeName, t -> {
            EntityDescriptor descriptor = metadata.resolveEntityDescriptor(t);
            if (descriptor == null) {
                throw new TeaQLRuntimeException("Entity descriptor not found for type: " + t);
            }
            return new PortableSQLRepository<>(descriptor, database, resolver);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public QueryResult query(UserContext ctx, QueryRequest request) {
        if (!(request instanceof DefaultQueryRequest)) {
            throw new TeaQLRuntimeException("Unsupported QueryRequest in PortableSQLDataService");
        }
        SearchRequest<?> searchRequest = ((DefaultQueryRequest) request).getSearchRequest();
        String typeName = searchRequest.getTypeName();
        PortableSQLRepository<?> repository = getRepository(typeName);
        SmartList<?> result = repository.loadInternal(ctx, (SearchRequest) searchRequest);
        return new DefaultQueryResult((SmartList<Entity>) result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MutationResult mutate(UserContext ctx, MutationRequest request) {
        if (!(request instanceof DefaultMutationRequest)) {
            throw new TeaQLRuntimeException("Unsupported MutationRequest in PortableSQLDataService");
        }
        DefaultMutationRequest mutation = (DefaultMutationRequest) request;
        Entity entity = mutation.getEntity();
        String typeName = entity.typeName();
        PortableSQLRepository repository = getRepository(typeName);

        if (mutation.getAction() == DefaultMutationRequest.Action.SAVE) {
            if (entity.getId() == null) {
                Long newId = repository.prepareId(ctx, entity);
                entity.setId(newId);
            }
            if (entity.newItem()) {
                entity.setVersion(1L);
                repository.createInternal(ctx, Collections.singletonList(entity));
            } else if (entity.updateItem()) {
                repository.updateInternal(ctx, Collections.singletonList(entity));
                entity.setVersion(entity.getVersion() + 1);
            } else if (entity.recoverItem()) {
                repository.recoverInternal(ctx, Collections.singletonList(entity));
                entity.setVersion(-entity.getVersion() + 1);
            }
            if (entity instanceof BaseEntity) {
                ((BaseEntity) entity).gotoNextStatus(EntityAction.PERSIST);
            }
        } else if (mutation.getAction() == DefaultMutationRequest.Action.DELETE) {
            repository.deleteInternal(ctx, Collections.singletonList(entity));
            entity.setVersion(-(entity.getVersion() + 1));
            if (entity instanceof BaseEntity) {
                ((BaseEntity) entity).gotoNextStatus(EntityAction.PERSIST);
            }
        }

        return new MutationResult() {};
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeInTransaction(UserContext ctx, TransactionCallback<T> action) {
        final Object[] resultHolder = new Object[1];
        final Exception[] exceptionHolder = new Exception[1];
        database.executeInTransaction(() -> {
            try {
                resultHolder[0] = action.doInTransaction();
            } catch (Exception e) {
                exceptionHolder[0] = e;
                throw new TeaQLRuntimeException("Transaction failed", e);
            }
        });
        if (exceptionHolder[0] != null) {
            if (exceptionHolder[0] instanceof RuntimeException) {
                throw (RuntimeException) exceptionHolder[0];
            }
            throw new TeaQLRuntimeException("Transaction failed", exceptionHolder[0]);
        }
        return (T) resultHolder[0];
    }

    public void ensureSchema(UserContext ctx, String typeName) {
        PortableSQLRepository<?> repository = getRepository(typeName);
        repository.ensureSchema(ctx);
    }
}
