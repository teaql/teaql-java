package io.teaql.cloud;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.teaql.cloud.consul.ConsulCloud;
import io.teaql.cloud.nacos.NacosCloud;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CloudClientsTest {
    private HttpServer server;
    private String baseUrl;
    private final Map<String, Integer> requests = new HashMap<>();

    @Before public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After public void stopServer() { server.stop(0); }

    @Test public void nacosExecutesRegistrationDiscoveryConfigAndHealth() {
        NacosCloud cloud = new NacosCloud(baseUrl, "tenant-a", "APP");
        ServiceInstance instance = new ServiceInstance("orders", "10.0.0.7", 8080);
        cloud.register(instance);
        cloud.deregister(instance);
        assertEquals("10.0.0.7", cloud.getInstances("orders").get(0).host());
        assertEquals("feature=true", cloud.getConfig("orders.yaml", "APP"));
        assertTrue(cloud.isHealthy());
        assertEquals(Integer.valueOf(1), requests.get("POST /nacos/v1/ns/instance"));
        assertEquals(Integer.valueOf(1), requests.get("DELETE /nacos/v1/ns/instance"));
    }

    @Test public void consulExecutesRegistrationDiscoveryAndHealth() {
        ConsulCloud cloud = new ConsulCloud(baseUrl, "token");
        ServiceInstance instance = new ServiceInstance("orders", "10.0.0.8", 8081);
        cloud.register(instance);
        cloud.deregister(instance);
        assertEquals("orders-1", cloud.getInstances("orders").get(0).serviceId());
        assertTrue(cloud.isHealthy());
        assertEquals(Integer.valueOf(1), requests.get("PUT /v1/agent/service/register"));
    }

    private void handle(HttpExchange exchange) throws IOException {
        String key = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
        requests.merge(key, 1, Integer::sum);
        String path = exchange.getRequestURI().getPath();
        String response;
        int status = 200;
        switch (path) {
            case "/nacos/v1/ns/instance" -> {
                String encoded = exchange.getRequestMethod().equals("DELETE")
                        ? exchange.getRequestURI().getRawQuery()
                        : new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> values = form(encoded);
                if (!"orders".equals(values.get("serviceName")) || !"10.0.0.7".equals(values.get("ip"))) status = 400;
                response = "ok";
            }
            case "/nacos/v1/ns/instance/list" -> response = "{\"hosts\":[{\"ip\":\"10.0.0.7\",\"port\":8080,\"healthy\":true,\"metadata\":{}}]}";
            case "/nacos/v1/cs/configs" -> response = "feature=true";
            case "/nacos/v1/console/health/readiness" -> response = "{\"status\":\"UP\"}";
            case "/v1/agent/service/register", "/v1/agent/service/deregister/orders" -> response = "";
            case "/v1/health/service/orders" -> response = "[{\"Service\":{\"ID\":\"orders-1\",\"Address\":\"10.0.0.8\",\"Port\":8081,\"Meta\":{}}}]";
            case "/v1/status/leader" -> response = "\"127.0.0.1:8300\"";
            default -> { status = 404; response = "not found"; }
        }
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static Map<String, String> form(String encoded) {
        Map<String, String> values = new HashMap<>();
        if (encoded == null || encoded.isBlank()) return values;
        for (String pair : encoded.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8));
        }
        return values;
    }
}
