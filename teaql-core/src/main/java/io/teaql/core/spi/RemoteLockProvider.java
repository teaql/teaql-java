package io.teaql.core.spi;

import io.teaql.core.utils.RemoteLock;

public interface RemoteLockProvider {
    /**
     * 获取指定的远程分布式锁实例
     * @param name 锁名称
     * @return 远程分布式锁
     * @throws LockException
     */
    RemoteLock getLock(String name) throws LockException;
}
