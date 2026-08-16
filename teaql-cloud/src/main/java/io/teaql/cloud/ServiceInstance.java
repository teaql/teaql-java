package io.teaql.cloud;

import java.util.Map;

public record ServiceInstance(
        String serviceId, String host, int port, boolean secure, Map<String, String> metadata) {
    public ServiceInstance(String serviceId, String host, int port) {
        this(serviceId, host, port, false, Map.of());
    }
}
