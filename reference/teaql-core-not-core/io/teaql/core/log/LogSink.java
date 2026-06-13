package io.teaql.core.log;

import io.teaql.core.ExecutionMetadata;

/**
 * App-layer log sink. Receives masked execution logs.
 * Application can register custom implementations: display on UI, send elsewhere.
 */
public interface LogSink {

    void onExecutionLog(ExecutionMetadata entry);
}
