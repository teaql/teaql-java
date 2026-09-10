package io.teaql.core.spi;

import java.util.concurrent.locks.Lock;

public interface LocalLockProvider {
    /**
     * Get the local lock instance for the specified name.
     * @param name the lock name
     * @return the local lock instance
     * @throws LockException if lock retrieval fails
     */
    Lock getLock(String name) throws LockException;
}
