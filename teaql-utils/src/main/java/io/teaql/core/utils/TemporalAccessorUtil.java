package io.teaql.core.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class TemporalAccessorUtil {

    public static long toEpochMilli(TemporalAccessor p0) {
        if (p0 == null) {
            throw new NullPointerException("TemporalAccessor cannot be null");
        }
        if (p0 instanceof Instant) {
            return ((Instant) p0).toEpochMilli();
        }
        if (p0 instanceof ZonedDateTime) {
            return ((ZonedDateTime) p0).toInstant().toEpochMilli();
        }
        if (p0 instanceof LocalDateTime) {
            return ((LocalDateTime) p0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (p0 instanceof LocalDate) {
            return ((LocalDate) p0).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        try {
            return Instant.from(p0).toEpochMilli();
        } catch (Exception e) {
            if (p0.isSupported(ChronoField.INSTANT_SECONDS)) {
                long sec = p0.getLong(ChronoField.INSTANT_SECONDS);
                long milli = p0.isSupported(ChronoField.MILLI_OF_SECOND) ? p0.get(ChronoField.MILLI_OF_SECOND) : 0;
                return sec * 1000L + milli;
            }
            throw new IllegalArgumentException("Cannot convert to epoch milli: " + p0, e);
        }
    }

}
