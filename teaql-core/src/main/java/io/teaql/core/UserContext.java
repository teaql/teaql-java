package io.teaql.core;

import java.util.stream.Stream;
import io.teaql.core.utils.OptNullBasicTypeFromObjectGetter;
import java.util.List;
import io.teaql.data.dynamic.DynamicFieldsFacade;

public interface UserContext extends OptNullBasicTypeFromObjectGetter<String> {

    /**
     * Applies trusted, context-owned defaults to a newly generated entity.
     * Business input must not supply tenant, actor, policy, provider, or audit
     * infrastructure values directly.
     */
    default <T extends Entity> T initializeEntity(String entityName, T entity) {
        if (entityName == null || entityName.trim().isEmpty()) {
            throw new IllegalArgumentException("entityName must not be empty");
        }
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        return entity;
    }

    void pushTrace(String comment);

    List<TraceNode> getTraceChain();

    void popTrace();
    void recordExecutionMetadata(ExecutionMetadata metadata);

    // Business-facing API
    <T extends Entity> T executeForOne(ExecutableRequest<T> request);

    <T extends Entity> SmartList<T> executeForList(ExecutableRequest<T> request);

    default <T extends Entity> SmartList<T> executeForPage(
            ExecutableRequest<T> request, int offset, int limit) {
        throw new TeaQLRuntimeException("Exact paged execution is not supported by this context");
    }

    <T extends Entity> Stream<T> executeForStream(ExecutableRequest<T> request);

    <T extends Entity> Stream<T> executeForStream(ExecutableRequest<T> request, int enhanceBatchSize);

    <T extends Entity> AggregationResult aggregation(ExecutableRequest<T> request);

    // Internal framework API (do not use in business logic)
    <T extends Entity> SmartList<T> internalExecuteForList(SearchRequest searchRequest);
    <T extends Entity> T internalExecuteForOne(SearchRequest searchRequest);
    <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest);
    <T extends Entity> Stream<T> internalExecuteForStream(SearchRequest searchRequest, int enhanceBatchSize);
    <T extends Entity> AggregationResult internalAggregation(SearchRequest request);

    default Object extension(String name) {
        return null;
    }

    default <T> T capability(Class<T> capabilityType) {
        return null;
    }

    /**
     * Returns the Dynamic Fields facade for reading/writing dynamic field values.
     * The facade is resolved via {@link #capability(Class)} and must be registered
     * by the runtime before use.
     *
     * @throws TeaQLRuntimeException if DynamicFieldsFacade is not registered
     */
    default DynamicFieldsFacade dynamicFields() {
        DynamicFieldsFacade facade = capability(DynamicFieldsFacade.class);
        if (facade == null) {
            throw new TeaQLRuntimeException("DynamicFieldsFacade not registered. "
                + "Ensure a dynamic fields provider is configured in the runtime.");
        }
        return facade.withContext(this);
    }

    /**
     * Generates a business string ID (like an order number) based on the entity and property descriptors.
     * Delegates to the registered BusinessIdGenerator capability.
     */
    default String generateBusinessId(Entity entity, io.teaql.core.meta.EntityDescriptor entityDesc, io.teaql.core.meta.PropertyDescriptor propertyDesc) {
        BusinessIdGenerator generator = capability(BusinessIdGenerator.class);
        if (generator == null) {
            throw new TeaQLRuntimeException("BusinessIdGenerator capability is not registered in this runtime.");
        }
        return generator.generateBusinessId(this, entity, entityDesc, propertyDesc);
    }

    void saveGraph(Object items);

    void saveGraph(Entity entity);

    void delete(Entity pEntity);

    @Override
    default Object getObj(String key, Object defaultValue) {
        Object val = getAttribute(key);
        return val != null ? val : defaultValue;
    }

    <T> T evaluate(String expression, Object... args);

    // ==========================================
    // Context Attribute (上下文级属性)
    // ==========================================
    default void putAttribute(String key, Object value) {}
    default <T> T getAttribute(String key, Class<T> clazz) { return null; }
    default Object getAttribute(String key) { return null; }

    // ==========================================
    // Remote Cache (分布式级，跨节点共享)
    // ==========================================
    default void putToRemoteCache(String key, Object value) {
        putToRemoteCache(key, value, 0);
    }
    default void putToRemoteCache(String key, Object value, int timeToLiveInSeconds) {
        io.teaql.core.spi.RemoteCacheProvider provider = capability(io.teaql.core.spi.RemoteCacheProvider.class);
        if (provider != null) {
            try {
                io.teaql.core.utils.RemoteCache<String, Object> cache = provider.getCache("default");
                if (cache != null) {
                    if (timeToLiveInSeconds > 0) {
                        cache.put(key, value, timeToLiveInSeconds * 1000L);
                    } else {
                        cache.put(key, value);
                    }
                }
            } catch (Exception e) {}
        }
    }
    default <T> T getFromRemoteCache(String key, Class<T> clazz) {
        io.teaql.core.spi.RemoteCacheProvider provider = capability(io.teaql.core.spi.RemoteCacheProvider.class);
        if (provider != null) {
            try {
                io.teaql.core.utils.RemoteCache<String, Object> cache = provider.getCache("default");
                if (cache != null) {
                    Object val = cache.get(key);
                    if (clazz.isInstance(val)) return clazz.cast(val);
                }
            } catch (Exception e) {}
        }
        return null;
    }
    default void removeFromRemoteCache(String key) {
        io.teaql.core.spi.RemoteCacheProvider provider = capability(io.teaql.core.spi.RemoteCacheProvider.class);
        if (provider != null) {
            try {
                io.teaql.core.utils.RemoteCache<String, Object> cache = provider.getCache("default");
                if (cache != null) cache.remove(key);
            } catch (Exception e) {}
        }
    }

    // ==========================================
    // Remote Lock (分布式锁)
    // ==========================================
    default boolean tryRemoteLock(String key, long timeoutMillis, long expireMillis) {
        io.teaql.core.spi.RemoteLockProvider provider = capability(io.teaql.core.spi.RemoteLockProvider.class);
        if (provider != null) {
            try {
                io.teaql.core.utils.RemoteLock lock = provider.getLock("default");
                if (lock != null) return lock.tryLock(key, timeoutMillis, expireMillis);
            } catch (Exception e) {}
        }
        return true;
    }
    default void unlockRemote(String key) {
        io.teaql.core.spi.RemoteLockProvider provider = capability(io.teaql.core.spi.RemoteLockProvider.class);
        if (provider != null) {
            try {
                io.teaql.core.utils.RemoteLock lock = provider.getLock("default");
                if (lock != null) lock.unlock(key);
            } catch (Exception e) {}
        }
    }

    // ==========================================
    // Local Cache (本地缓存)
    // ==========================================
    default void putToLocalCache(String key, Object value) {}
    default void putToLocalCache(String key, Object value, int timeToLiveInSeconds) {}
    default <T> T getFromLocalCache(String key, Class<T> clazz) { return null; }
    default void removeFromLocalCache(String key) {}

    // ==========================================
    // Local Lock (本地锁)
    // ==========================================
    default boolean tryLocalLock(String key, long timeoutMillis, long expireMillis) { return true; }
    default void unlockLocal(String key) {}
}
