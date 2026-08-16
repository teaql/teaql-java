package io.teaql.core.spi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/** Process-local keyed locks with timeout, lease expiry, and owner-safe release. */
public final class DefaultLocalLockProvider implements LocalLockProvider {
    public static final DefaultLocalLockProvider INSTANCE = new DefaultLocalLockProvider();

    private final Map<String, LeaseLock> locks = new ConcurrentHashMap<>();

    private DefaultLocalLockProvider() {}

    @Override
    public Lock getLock(String name) {
        return locks.computeIfAbsent(name, ignored -> new LeaseLock());
    }

    @Override
    public boolean tryLock(String namespace, String key, long timeoutMillis, long expireMillis) {
        return ((LeaseLock) getLock(namespace + ":" + key))
                .tryLease(Thread.currentThread(), timeoutMillis, expireMillis);
    }

    @Override
    public boolean tryLock(String namespace, String key, Object owner,
            long timeoutMillis, long expireMillis) {
        return ((LeaseLock) getLock(namespace + ":" + key))
                .tryLease(owner, timeoutMillis, expireMillis);
    }

    @Override
    public void unlock(String namespace, String key) {
        ((LeaseLock) getLock(namespace + ":" + key)).unlockLease(Thread.currentThread());
    }

    @Override
    public void unlock(String namespace, String key, Object owner) {
        ((LeaseLock) getLock(namespace + ":" + key)).unlockLease(owner);
    }

    private static final class LeaseLock implements Lock {
        private Object owner;
        private long expiresAt;

        synchronized boolean tryLease(Object requester, long timeoutMillis, long expireMillis) {
            long deadline = System.currentTimeMillis() + Math.max(timeoutMillis, 0);
            while (true) {
                long now = System.currentTimeMillis();
                if (owner == null || (expiresAt > 0 && now >= expiresAt) || owner == requester) {
                    owner = requester;
                    expiresAt = expireMillis > 0 ? now + expireMillis : 0;
                    return true;
                }
                long remaining = deadline - now;
                if (remaining <= 0) return false;
                long leaseRemaining = expiresAt > 0 ? expiresAt - now : remaining;
                try {
                    wait(Math.max(1, Math.min(remaining, leaseRemaining)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        @Override public void lock() { while (!tryLease(Thread.currentThread(), Long.MAX_VALUE / 2, 0)) { /* interrupted */ } }
        @Override public void lockInterruptibly() throws InterruptedException {
            if (!tryLock(Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS)) throw new InterruptedException();
        }
        @Override public boolean tryLock() { return tryLease(Thread.currentThread(), 0, 0); }
        @Override public boolean tryLock(long time, TimeUnit unit) { return tryLease(Thread.currentThread(), unit.toMillis(time), 0); }
        synchronized void unlockLease(Object requester) {
            if (owner == requester) {
                owner = null;
                expiresAt = 0;
                notifyAll();
            }
        }
        @Override public void unlock() { unlockLease(Thread.currentThread()); }
        @Override public Condition newCondition() { throw new UnsupportedOperationException(); }
    }
}
