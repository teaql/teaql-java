package io.teaql.autoconfigure.lock;

import io.teaql.core.UserContext;
import io.teaql.core.lock.LockService;

import java.util.concurrent.locks.Lock;

import org.redisson.api.RedissonClient;

import io.teaql.core.UserContext;

public class RedisLockService extends LocalLockService {
    RedissonClient redissonClient;

    public RedisLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Lock getDistributeLock(UserContext ctx, String key) {
        return redissonClient.getLock(key);
    }
}
