package io.teaql.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticLog {
    private static final Logger log = LoggerFactory.getLogger(StaticLog.class);

    public static void info(String format, Object... arguments) {
        log.info(format, arguments);
    }
}
