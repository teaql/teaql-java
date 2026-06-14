package io.teaql.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
