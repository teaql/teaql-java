package io.teaql.core.repository;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class TaskThreadFactory implements ThreadFactory {

    public TaskThreadFactory(String prefix, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.prefix = prefix;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    private static final AtomicInteger count = new AtomicInteger(0);
    private String prefix;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(prefix + count.incrementAndGet());
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        return thread;
    }
}

public class EnhancerThreadUtil {

    static int QUEUE_SIZE = 1024;
    static int CORE_POOL_SIZE = 8;
    static int MAX_POOL_SIZE = 128;
    static int THREAD_ALIVE_SECONDS = 60;
    static String THREAD_NAME_PREFIX = "TeaQL-Enhancer-";

    public static ThreadPoolExecutor threadPoolExecutor() {

        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        TaskThreadFactory taskThreadFactory = new TaskThreadFactory(THREAD_NAME_PREFIX,
                null);
        return new ThreadPoolExecutor(CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                THREAD_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                queue,
                taskThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }


}
