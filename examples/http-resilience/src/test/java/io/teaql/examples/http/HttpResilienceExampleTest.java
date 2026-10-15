package io.teaql.examples.http;

import com.sun.net.httpserver.HttpServer;
import io.teaql.core.http.HttpRequestService;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.tools.http.HttpServiceProfile;
import io.teaql.tools.http.JdkHttpRequestService;
import io.teaql.tools.http.impl.JdkHttpTool;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpResilienceExampleTest {
    @Test public void contextAndAgentToolUseOneControlledService() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/orders/1", exchange -> {
            requests.incrementAndGet();
            byte[] body = "{\"status\":\"submitted\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var profile = HttpServiceProfile.builder("orders",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/")).build();
            var service = new JdkHttpRequestService(List.of(profile));
            var context = new DefaultUserContext(null);
            context.putAttribute(HttpRequestService.class.getName(), service);

            var response = context.httpRequests().execute(context,
                    HttpRequestService.Call.get("orders", "orders/1",
                            "load order status", "show the order status"));
            assertEquals(200, response.status());
            assertEquals("submitted", ((java.util.Map<?, ?>) response.jsonValue()).get("status"));
            assertEquals(1, response.attempts());

            String viaTool = new JdkHttpTool(context).get("orders", "orders/1")
                    .comment("load order status for agent")
                    .purpose("explain the order status")
                    .execute();
            assertTrue(viaTool.contains("submitted"));
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }
}
