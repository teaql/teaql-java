package io.teaql.runtime;

import java.net.URI;

/** Optional module SPI. Core never exposes SDK/exporter types. */
public interface StandardTelemetryFactory {
    RuntimeTelemetry create(String serviceName, URI endpoint);
}
