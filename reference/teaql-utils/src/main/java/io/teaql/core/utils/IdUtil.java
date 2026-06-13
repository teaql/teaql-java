package io.teaql.core.utils;

import java.util.UUID;

public class IdUtil {

    private static final long START_EPOCH = 1577836800000L;
    private static final long WORKER_ID = 1L;
    private static final long DATACENTER_ID = 1L;
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    public static java.lang.String fastSimpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static synchronized long getSnowflakeNextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp; // simple clock drift handling or wait
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & 4095L;
            if (sequence == 0) {
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START_EPOCH) << 22)
            | (DATACENTER_ID << 17)
            | (WORKER_ID << 12)
            | sequence;
    }

    public static java.lang.String getSnowflakeNextIdStr() {
        return String.valueOf(getSnowflakeNextId());
    }

}
