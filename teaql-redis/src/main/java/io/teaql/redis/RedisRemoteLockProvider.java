package io.teaql.redis;

import io.teaql.core.spi.RemoteLockProvider;
import io.teaql.core.spi.LockException;
import io.teaql.core.utils.RemoteLock;

public class RedisRemoteLockProvider implements RemoteLockProvider {
    @Override
    public RemoteLock getLock(String name) throws LockException {
        RedisRemoteLock lock = new RedisRemoteLock();
        // TODO: 从配置中注入 JedisPool
        lock.init(null);
        return lock;
    }
}
