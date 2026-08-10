package io.teaql.redis;

import io.teaql.core.utils.RemoteLock;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

public class RedisRemoteLock implements RemoteLock {

    private JedisPool jedisPool;

    public RedisRemoteLock() {
        // Required for SPI ServiceLoader
    }

    public void init(JedisPool pool) {
        this.jedisPool = pool;
    }

    @Override
    public boolean tryLock(String key, long timeoutMillis, long expireMillis) {
        if (jedisPool == null) return false;
        long start = System.currentTimeMillis();
        try (Jedis jedis = jedisPool.getResource()) {
            while (true) {
                String result = jedis.set(key, "locked", SetParams.setParams().nx().px(expireMillis));
                if ("OK".equals(result)) {
                    return true;
                }
                if (System.currentTimeMillis() - start > timeoutMillis) {
                    return false;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        if (jedisPool == null) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }
}
