package io.teaql.core.spi;

/**
 * Provider interface for remote lock implementations.
 */

public interface RemoteLockProvider {
    /**
     * Get the remote distributed lock instance for the specified name.
     * @param name the lock name
     * @return the remote lock instance
     * @throws LockException if lock retrieval fails
     */
    RemoteLock getLock(String name) throws LockException;
}
