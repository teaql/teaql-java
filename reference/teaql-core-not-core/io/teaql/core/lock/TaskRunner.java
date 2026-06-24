package io.teaql.core.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import io.teaql.core.utils.ListUtil;
import io.teaql.core.utils.StreamUtil;
import io.teaql.core.utils.ThreadUtil;
import io.teaql.core.utils.ArrayUtil;
import io.teaql.core.utils.ObjUtil;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.Entity;

public class TaskRunner {

    public ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
    public Executor executor = ThreadUtil.newExecutorByBlockingCoefficient(0.5f);

    public void execute(Runnable runnable, Entity... entities) {
        List<String> list =
                StreamUtil.of(entities)
                        .filter(entity -> entity != null)
                        .map(entity -> entity.typeName() + entity.getId())
                        .collect(Collectors.toList());
        String[] keys = ArrayUtil.toArray(list, String.class);
        execute(runnable, keys);
    }

    public void execute(Runnable runnable, String... keys) {
        try {
            lock(5000, keys);
            runnable.run();
        }
        finally {
            unlock(keys);
        }
    }

    public void singleTaskRun(String taskName, Runnable runnable) {
        boolean canRun = false;
        try {
            canRun = tryLock(taskName);
            if (!canRun) {
                throw new RuntimeException(StrUtil.format("Task {} is already running.", taskName));
            }
            runnable.run();
        }
        finally {
            if (canRun) {
                unlock(taskName);
            }
        }
    }

    public void trySingleTaskRun(String taskName, Runnable runnable) {
        boolean canRun = false;
        try {
            canRun = tryLock(taskName);
            if (!canRun) {
                System.getLogger(TaskRunner.class.getName())
                        .log(System.Logger.Level.INFO, "Task {0} is already running.", taskName);
                return;
            }
            runnable.run();
        }
        finally {
            if (canRun) {
                unlock(taskName);
            }
        }
    }

    public void singleTaskRunASync(String taskName, Runnable runnable) {
        executor.execute(
                () -> {
                    trySingleTaskRun(taskName, runnable);
                });
    }

    public <V> V call(Callable<V> callable, String... keys) {
        try {
            lock(5000, keys);
            return callable.call();
        }
        catch (Exception pE) {
            throw new RuntimeException(pE);
        }
        finally {
            unlock(keys);
        }
    }

    private void lock(long timeout, String... keys) {
        keys = ArrayUtil.removeNull(keys);
        if (ObjUtil.isEmpty(keys)) {
            return;
        }
        List<String> list = ListUtil.list(false, keys);
        Collections.sort(list);
        List<Lock> acquiredLocks = new ArrayList<>();
        boolean release = false;
        try {
            for (String key : list) {
                Lock lock = ensureLockForKey(key);
                try {
                    if (!lock.tryLock(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        release = true;
                        throw new RuntimeException("error while acquire lock:" + key);
                    }
                    acquiredLocks.add(lock);
                }
                catch (InterruptedException pE) {
                    release = true;
                    throw new RuntimeException(pE);
                }
            }

        }
        finally {
            if (release) {
                for (Lock acquiredLock : acquiredLocks) {
                    acquiredLock.unlock();
                }
            }
        }
    }

    private boolean tryLock(String... keys) {
        keys = ArrayUtil.removeNull(keys);
        if (ObjUtil.isEmpty(keys)) {
            return true;
        }
        List<String> list = ListUtil.list(false, keys);
        Collections.sort(list);
        List<Lock> acquiredLocks = new ArrayList<>();
        boolean release = false;
        try {
            for (String key : list) {
                Lock lock = ensureLockForKey(key);
                if (!lock.tryLock()) {
                    release = true;
                    break;
                }
                acquiredLocks.add(lock);
            }
            return !release;
        }
        finally {
            if (release) {
                for (Lock acquiredLock : acquiredLocks) {
                    acquiredLock.unlock();
                }
            }
        }
    }

    private void unlock(String... keys) {
        keys = ArrayUtil.removeNull(keys);
        if (ObjUtil.isEmpty(keys)) {
            return;
        }
        List<String> list = ListUtil.list(false, keys);
        Collections.sort(list);
        Collections.reverse(list);
        for (String key : list) {
            Lock lock = ensureLockForKey(key);
            lock.unlock();
        }
    }

    private Lock ensureLockForKey(String key) {
        ReentrantLock lock = new ReentrantLock();
        Lock l = locks.putIfAbsent(key, lock);
        if (l != null) {
            return l;
        }
        return lock;
    }
}
