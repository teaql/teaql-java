package io.teaql.runtime.memory;

import io.teaql.core.*;
import io.teaql.runtime.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryDataService implements DataServiceExecutor, QueryExecutor, MutationExecutor {

    private final String name;
    private final DataServiceCapabilities capabilities;
    private final Map<String, TypeStorage> database = new ConcurrentHashMap<>();
    private final CriteriaFilter<Entity> filter = new CriteriaFilter<>();
    private final int maxEntriesPerType;

    private static class TypeStorage {
        final ReadWriteLock lock = new ReentrantReadWriteLock();
        final Map<Long, Entity> data;

        TypeStorage(int maxEntries) {
            this.data = new LinkedHashMap<Long, Entity>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Entity> eldest) {
                    return size() > maxEntries;
                }
            };
        }
    }

    public MemoryDataService(String name) {
        this(name, 10000); // Default capacity of 10000 per type
    }

    public MemoryDataService(String name, int maxEntriesPerType) {
        this.name = name;
        this.maxEntriesPerType = maxEntriesPerType;
        this.capabilities = new DataServiceCapabilities();
        this.capabilities.setQuery(true);
        this.capabilities.setMutation(true);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public DataServiceCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public QueryResult query(UserContext ctx, QueryRequest request) {
        if (!(request instanceof DefaultQueryRequest)) {
            throw new TeaQLRuntimeException("Unsupported QueryRequest in MemoryDataService");
        }
        SearchRequest<?> searchRequest = ((DefaultQueryRequest) request).getSearchRequest();
        String typeName = searchRequest.getTypeName();
        TypeStorage storage = database.computeIfAbsent(typeName, k -> new TypeStorage(maxEntriesPerType));

        SmartList<Entity> result = new SmartList<>();
        storage.lock.readLock().lock();
        try {
            for (Entity entity : storage.data.values()) {
                if (filter.accept(entity, searchRequest.getSearchCriteria())) {
                    result.add(entity);
                }
            }
        } finally {
            storage.lock.readLock().unlock();
        }
        return new DefaultQueryResult(result);
    }

    @Override
    public MutationResult mutate(UserContext ctx, MutationRequest request) {
        if (!(request instanceof DefaultMutationRequest)) {
            throw new TeaQLRuntimeException("Unsupported MutationRequest in MemoryDataService");
        }
        DefaultMutationRequest mutation = (DefaultMutationRequest) request;
        Entity entity = mutation.getEntity();
        String typeName = entity.typeName();
        TypeStorage storage = database.computeIfAbsent(typeName, k -> new TypeStorage(maxEntriesPerType));

        storage.lock.writeLock().lock();
        try {
            if (mutation.getAction() == DefaultMutationRequest.Action.SAVE) {
                if (entity.getId() == null) {
                    throw new TeaQLRuntimeException("Entity ID must be allocated before save");
                }
                storage.data.put(entity.getId(), entity);
                ((BaseEntity) entity).internalSet("version", entity.getVersion() == null ? 1L : entity.getVersion() + 1);
                if (entity instanceof BaseEntity) {
                    ((BaseEntity) entity).gotoNextStatus(EntityAction.PERSIST);
                }
            } else if (mutation.getAction() == DefaultMutationRequest.Action.DELETE) {
                storage.data.remove(entity.getId());
                if (entity instanceof BaseEntity) {
                    ((BaseEntity) entity).gotoNextStatus(EntityAction.PERSIST);
                }
            }
        } finally {
            storage.lock.writeLock().unlock();
        }

        return new MutationResult() {};
    }

    public void clear() {
        for (TypeStorage storage : database.values()) {
            storage.lock.writeLock().lock();
            try {
                storage.data.clear();
            } finally {
                storage.lock.writeLock().unlock();
            }
        }
        database.clear();
    }
}
