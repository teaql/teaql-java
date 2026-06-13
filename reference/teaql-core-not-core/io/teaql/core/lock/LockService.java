package io.teaql.core.lock;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;

import io.teaql.core.utils.ThreadUtil;

import io.teaql.core.UserContext;

public interface LockService {
    Executor taskExecutor = ThreadUtil.newExecutorByBlockingCoefficient(0.5f);

    Lock getLocalLock(UserContext ctx, String key);

    Lock getDistributeLock(UserContext ctx, String key);
}
