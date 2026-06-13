package io.teaql.core;

public interface ExecutionLogSink {
    void log(UserContext ctx, ExecutionMetadata metadata);
}
