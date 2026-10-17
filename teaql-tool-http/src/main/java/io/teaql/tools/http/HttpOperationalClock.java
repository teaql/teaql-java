package io.teaql.tools.http;

import java.util.concurrent.locks.LockSupport;

/** Monotonic transport time, independent of UserContext business time. */
public interface HttpOperationalClock {
    long nanoTime();
    void pauseNanos(long nanos);

    HttpOperationalClock SYSTEM = new HttpOperationalClock() {
        @Override public long nanoTime() { return System.nanoTime(); }
        @Override public void pauseNanos(long nanos) { LockSupport.parkNanos(nanos); }
    };
}
