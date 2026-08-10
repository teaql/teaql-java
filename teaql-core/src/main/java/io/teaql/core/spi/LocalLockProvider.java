package io.teaql.core.spi;

import java.util.concurrent.locks.Lock;

public interface LocalLockProvider {
    /**
     * 获取指定的本地锁实例
     * @param name 锁名称
     * @return 本地锁实例
     * @throws LockException
     */
    Lock getLock(String name) throws LockException;
}
