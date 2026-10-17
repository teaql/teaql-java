package io.teaql.core.http;

import java.util.Map;

/** Optional runtime-owned carrier injector; a business trace ID is never used as a W3C header. */
@FunctionalInterface
public interface HttpPropagation {
    void inject(Map<String, String> carrier);
}
