package io.teaql.core.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.teaql.core.UserContext;

/**
 * Simple local lock service implementation. No Spring dependency.
 * For plain Java environments.
 */
public class SimpleLockService implements LockService {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Lock getLocalLock(UserContext ctx, String lockName) {
        return locks.computeIfAbsent(lockName, k -> new ReentrantLock());
    }

    @Override
    public Lock getDistributeLock(UserContext ctx, String lockName) {
        // Falls back to local lock in non-distributed environments
        return getLocalLock(ctx, lockName);
    }
}
