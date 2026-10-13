package io.teaql.core;

/** Statement-owned telemetry identity; never derived from log order or an active span at completion. */
public record SqlExecutionCorrelation(String traceId, String spanId) {}
