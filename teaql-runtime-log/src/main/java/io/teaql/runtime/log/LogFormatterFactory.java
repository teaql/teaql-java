package io.teaql.runtime.log;

import io.teaql.runtime.config.TeaQLEnv;

public class LogFormatterFactory {
    private static final LogFormatter instance;

    static {
        String format = TeaQLEnv.get("TEAQL_LOG_FORMAT", "human");
        if ("json".equalsIgnoreCase(format) || "debug".equalsIgnoreCase(format)) {
            instance = new JsonReaderFormatter();
        } else {
            instance = new HumanReaderFormatter();
        }
    }

    public static LogFormatter getFormatter() {
        return instance;
    }
}
