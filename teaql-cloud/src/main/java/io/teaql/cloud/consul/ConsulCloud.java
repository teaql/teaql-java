package io.teaql.cloud.consul;

import com.fasterxml.jackson.databind.JsonNode;
import io.teaql.cloud.CloudClient;
import io.teaql.cloud.CloudException;
import io.teaql.cloud.HttpSupport;
import io.teaql.cloud.ServiceInstance;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConsulCloud implements CloudClient {
    private final HttpSupport http;

    public ConsulCloud(String serverAddress, String token) { this(serverAddress, token, null); }

    public ConsulCloud(String serverAddress, String token, HttpClient client) {
        this.http = new HttpSupport(serverAddress, client,
                token == null || token.isBlank() ? Map.of() : Map.of("X-Consul-Token", token));
    }

    @Override public void register(ServiceInstance instance) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ID", instance.serviceId());
        body.put("Name", instance.serviceId());
        body.put("Address", instance.host());
        body.put("Port", instance.port());
        body.put("Meta", instance.metadata());
        http.json("PUT", "/v1/agent/service/register", body);
    }

    @Override public void deregister(ServiceInstance instance) {
        http.json("PUT", "/v1/agent/service/deregister/" + path(instance.serviceId()), null);
    }

    @Override public List<ServiceInstance> getInstances(String serviceId) {
        String body = http.json("GET", "/v1/health/service/" + path(serviceId) + "?passing=true", null);
        try {
            List<ServiceInstance> result = new ArrayList<>();
            for (JsonNode item : HttpSupport.JSON.readTree(body)) {
                JsonNode service = item.path("Service");
                result.add(new ServiceInstance(service.path("ID").asText(),
                        service.path("Address").asText(), service.path("Port").asInt(), false,
                        HttpSupport.JSON.convertValue(service.path("Meta"), Map.class)));
            }
            return result;
        } catch (Exception e) { throw new CloudException("decode Consul instances", e); }
    }

    @Override public boolean isHealthy() {
        try {
            String leader = http.json("GET", "/v1/status/leader", null);
            return !leader.trim().equals("\"\"");
        } catch (CloudException e) { return false; }
    }

    private static String path(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
