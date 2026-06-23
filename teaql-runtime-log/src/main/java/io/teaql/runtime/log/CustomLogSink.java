package io.teaql.runtime.log;

public interface CustomLogSink {
    void onLog(String formattedLogContent);
}
