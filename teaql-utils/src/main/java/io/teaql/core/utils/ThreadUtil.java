package io.teaql.core.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {

    public static ThreadPoolExecutor newExecutorByBlockingCoefficient(float p0) {
        if (p0 < 0.0F || p0 >= 1.0F) {
            throw new IllegalArgumentException("Blocking coefficient must be between [0.0, 1.0)");
        }
        int poolSize = (int) ((float) Runtime.getRuntime().availableProcessors() / (1.0F - p0));
        if (poolSize <= 0) {
            poolSize = 1;
        }
        return new ThreadPoolExecutor(
            poolSize, poolSize,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>()
        );
    }

}
