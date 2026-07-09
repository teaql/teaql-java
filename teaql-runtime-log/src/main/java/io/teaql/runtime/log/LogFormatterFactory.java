package io.teaql.runtime.log;

import io.teaql.runtime.config.TeaQLEnv;

public class LogFormatterFactory {
    private static final LogFormatter instance;

    static {
        String format = TeaQLEnv.get("TEAQL_LOG_FORMAT", "human");
        instance = createFormatter(format);
    }

    static LogFormatter createFormatter(String format) {
        return ("json".equalsIgnoreCase(format) || "debug".equalsIgnoreCase(format))
                ? new JsonReaderFormatter()
                : new HumanReaderFormatter();
    }

    public static LogFormatter getFormatter() {
        return instance;
    }
}
