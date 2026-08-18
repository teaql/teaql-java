package io.teaql.runtime.log;

import io.teaql.core.UserContext;

public final class LogSinks {

    private LogSinks() {
    }

    public static void register(UserContext context, CustomLogSink sink) {
        if (context == null) {
            return;
        }
        context.putAttribute(CustomLogSink.class.getName(), sink);
    }
}
