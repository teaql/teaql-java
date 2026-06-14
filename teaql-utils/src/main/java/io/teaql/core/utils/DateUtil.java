package io.teaql.core.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static java.time.LocalDateTime toLocalDateTime(java.util.Calendar p0) {
        if (p0 == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(p0.getTimeInMillis()), p0.getTimeZone().toZoneId());
    }

    public static java.time.LocalDateTime toLocalDateTime(java.time.Instant p0) {
        if (p0 == null) {
            return null;
        }
        return LocalDateTime.ofInstant(p0, ZoneId.systemDefault());
    }

    public static java.time.LocalDateTime toLocalDateTime(java.util.Date p0) {
        if (p0 == null) {
            return null;
        }
        return LocalDateTime.ofInstant(p0.toInstant(), ZoneId.systemDefault());
    }

}
