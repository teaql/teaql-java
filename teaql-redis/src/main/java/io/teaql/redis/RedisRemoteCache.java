package io.teaql.redis;

import io.teaql.core.utils.RemoteCache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

public class RedisRemoteCache<K, V> implements RemoteCache<K, V> {
    private JedisPool jedisPool;
    private String namespace;
    private ObjectMapper objectMapper;
    private Class<V> type;

    public RedisRemoteCache() {
        // Required for SPI ServiceLoader default instantiation
    }

    public void init(JedisPool pool, String namespace, ObjectMapper mapper, Class<V> type) {
        this.jedisPool = pool;
        this.namespace = namespace;
        this.objectMapper = mapper;
        this.type = type;
    }

    private String buildKey(K key) {
        if (namespace == null) {
            return key.toString();
        }
        return namespace + ":" + key.toString();
    }

    @Override
    public void expire(K key, long timeout) {
        if (jedisPool == null) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.pexpire(buildKey(key), timeout);
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, 0); // 0 means no timeout
    }

    @Override
    public void put(K key, V value, long timeout) {
        if (jedisPool == null) return;
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(value);
            if (timeout > 0) {
                jedis.psetex(buildKey(key), timeout, json);
            } else {
                jedis.set(buildKey(key), json);
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis put failed", e);
        }
    }

    @Override
    public V get(K key) {
        if (jedisPool == null) return null;
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(buildKey(key));
            if (json == null) return null;
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null; // Fallback to null on error
        }
    }

    @Override
    public V get(K key, boolean isUpdate) {
        return get(key);
    }

    @Override
    public V get(K key, Supplier<? extends V> supplier) {
        V value = get(key);
        if (value == null && supplier != null) {
            value = supplier.get();
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    @Override
    public void remove(K key) {
        if (jedisPool == null) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(buildKey(key));
        }
    }

    @Override
    public boolean containsKey(K key) {
        if (jedisPool == null) return false;
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(buildKey(key));
        }
    }
}
