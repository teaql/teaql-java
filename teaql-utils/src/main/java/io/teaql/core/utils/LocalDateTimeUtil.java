package io.teaql.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SQL_DATETIME_FORMATTER = new java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 1, 9, true)
            .toFormatter(java.util.Locale.ROOT);

    /** JDBC-style text without creating an instant or consulting a time zone. */
    public static String formatSql(LocalDateTime value) {
        return value == null ? null : SQL_DATETIME_FORMATTER.format(value);
    }

    public static java.lang.String formatNormal(java.time.LocalDate p0) {
        if (p0 == null) {
            return null;
        }
        return p0.format(DATE_FORMATTER);
    }

    public static java.lang.String formatNormal(java.time.LocalDateTime p0) {
        if (p0 == null) {
            return null;
        }
        return p0.format(DATETIME_FORMATTER);
    }

}
