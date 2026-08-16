package io.teaql.cloud.nacos;

import com.fasterxml.jackson.databind.JsonNode;
import io.teaql.cloud.CloudClient;
import io.teaql.cloud.CloudException;
import io.teaql.cloud.HttpSupport;
import io.teaql.cloud.ServiceInstance;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NacosCloud implements CloudClient {
    private final HttpSupport http;
    private final String namespace;
    private final String group;

    public NacosCloud(String serverAddress, String namespace, String group) {
        this(serverAddress, namespace, group, null);
    }

    public NacosCloud(String serverAddress, String namespace, String group, HttpClient client) {
        this.http = new HttpSupport(serverAddress, client, Map.of());
        this.namespace = namespace == null ? "" : namespace;
        this.group = group == null || group.isBlank() ? "DEFAULT_GROUP" : group;
    }

    private Map<String, String> instanceParameters(ServiceInstance instance) {
        Map<String, String> values = new HashMap<>();
        values.put("serviceName", instance.serviceId());
        values.put("ip", instance.host());
        values.put("port", Integer.toString(instance.port()));
        values.put("namespaceId", namespace);
        values.put("groupName", group);
        values.put("ephemeral", "true");
        try { values.put("metadata", HttpSupport.JSON.writeValueAsString(instance.metadata())); }
        catch (Exception e) { throw new CloudException("encode Nacos metadata", e); }
        return values;
    }

    @Override public void register(ServiceInstance instance) {
        http.request("POST", "/nacos/v1/ns/instance", instanceParameters(instance), true);
    }

    @Override public void deregister(ServiceInstance instance) {
        http.request("DELETE", "/nacos/v1/ns/instance", instanceParameters(instance), true);
    }

    @Override public List<ServiceInstance> getInstances(String serviceId) {
        String body = http.request("GET", "/nacos/v1/ns/instance/list", Map.of(
                "serviceName", serviceId, "namespaceId", namespace,
                "groupName", group, "healthyOnly", "true"), false);
        try {
            List<ServiceInstance> result = new ArrayList<>();
            for (JsonNode host : HttpSupport.JSON.readTree(body).path("hosts")) {
                if (host.path("healthy").asBoolean()) {
                    result.add(new ServiceInstance(serviceId, host.path("ip").asText(),
                            host.path("port").asInt(), false,
                            HttpSupport.JSON.convertValue(host.path("metadata"), Map.class)));
                }
            }
            return result;
        } catch (Exception e) { throw new CloudException("decode Nacos instances", e); }
    }

    public String getConfig(String dataId, String configGroup) {
        return http.request("GET", "/nacos/v1/cs/configs", Map.of(
                "dataId", dataId, "group", configGroup == null ? group : configGroup,
                "tenant", namespace), false);
    }

    @Override public boolean isHealthy() {
        try {
            http.request("GET", "/nacos/v1/console/health/readiness", Map.of(), false);
            return true;
        } catch (CloudException e) { return false; }
    }
}
