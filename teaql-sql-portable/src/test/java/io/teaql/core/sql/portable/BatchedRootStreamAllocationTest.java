package io.teaql.core.sql.portable;

import io.teaql.core.BaseEntity;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Empty cursor allocation must not scale with the configured batch limit. */
public class BatchedRootStreamAllocationTest {
    private static final int REPEATS = 1_000;
    private static long measure(Object bean, java.lang.reflect.Method allocated, int limit) throws Exception {
        long thread = Thread.currentThread().getId();
        long before = (Long)allocated.invoke(bean, thread);
        for (int i=0; i<REPEATS; i++) {
            try (var stream = BatchedRootStream.enhance(Stream.<BaseEntity>empty(), limit,
                    rows -> fail("empty cursor must not reach enhancement"))) {
                assertEquals(0, stream.count());
            }
        }
        return (Long)allocated.invoke(bean, thread) - before;
    }
    @Test public void emptyAllocationDoesNotGrowWithBatchLimit() throws Exception {
        // JVM-only observation: do not add a management-module dependency to the runtime SPI.
        var bean = Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null);
        var api = Class.forName("com.sun.management.ThreadMXBean");
        assertEquals(true, api.getMethod("isThreadAllocatedMemorySupported").invoke(bean));
        api.getMethod("setThreadAllocatedMemoryEnabled",boolean.class).invoke(bean,true);
        var allocated = api.getMethod("getThreadAllocatedBytes",long.class);
        for(int i=0; i<8; i++) measure(bean, allocated, 4);
        for(int round=0; round<3; round++) {
            long small = measure(bean, allocated, 4), large = measure(bean, allocated, 8_192);
            System.out.printf("EMPTY_STREAM,%d,%d,%d,%d%n", round, REPEATS, small, large);
            assertTrue("empty stream must not allocate a batch-sized backing array",large <= small + 256L * REPEATS);
        }
    }
}
