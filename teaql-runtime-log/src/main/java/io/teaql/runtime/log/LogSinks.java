package io.teaql.runtime.log;

import io.teaql.core.UserContext;

public final class LogSinks {

    private LogSinks() {
    }

    public static void register(UserContext ctx, CustomLogSink sink) {
        if (ctx == null) {
            return;
        }
        ctx.putAttribute(CustomLogSink.class.getName(), sink);
    }
}
