package io.teaql.examples.http;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import com.sun.net.httpserver.HttpServer;
import io.teaql.core.TraceKind;
import io.teaql.core.TraceNode;
import io.teaql.core.http.HttpRequestService;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.opentelemetry.StandardOpenTelemetryFactory;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.tools.http.HttpServiceProfile;
import io.teaql.tools.http.JdkHttpRequestService;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/** Explicit live probe: the conformance runner must verify OpenObserve readback. */
public class HttpOpenObserveProbeTest {
    @Test public void exportsRequestOwnedHttpTraceToControlledCollector() throws Exception {
        assumeTrue("true".equals(System.getenv("TEAQL_HTTP_LIVE_OTLP")));
        URI endpoint = URI.create(System.getenv("TEAQL_TELEMETRY_OTLP_ENDPOINT"));
        assertEquals("127.0.0.1", endpoint.getHost());
        String serviceName = System.getenv("TEAQL_TELEMETRY_SERVICE_NAME");
        assertNotNull(serviceName);
        var telemetry = new StandardOpenTelemetryFactory().create(serviceName, endpoint);
        var carrier = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        server.createContext("/api/orders/1", exchange -> {
            carrier.set(exchange.getRequestHeaders().getFirst("traceparent"));
            byte[] body = "{\"status\":\"submitted\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .telemetry(telemetry).build();
            var context = new DefaultUserContext(runtime);
            var profile = HttpServiceProfile.builder("orders",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/"))
                    .build();
            context.putAttribute(HttpRequestService.class.getName(),
                    new JdkHttpRequestService(List.of(profile)));
            var call = HttpRequestService.Call.get("orders",
                    "orders/1?token=PRIVATE_QUERY_CANARY", "load order status", "render order details")
                    .withParentTraceChain(List.of(
                            new TraceNode(TraceKind.PURPOSE, "Order", "render order details"),
                            new TraceNode(TraceKind.COMMENT, "Order", "load order graph")));
            var response = context.httpRequests().execute(context, call);
            assertEquals(200, response.status());
            assertEquals("submitted", ((java.util.Map<?, ?>) response.jsonValue()).get("status"));
            assertNotNull(carrier.get());
            assertTrue(carrier.get().startsWith("00-") && carrier.get().length() == 55);
            telemetry.flush();
            System.out.println("HTTP_OTLP_PROBE_PASS traceparent=" + carrier.get() + " lineage=3");
        } finally {
            server.stop(0);
            telemetry.shutdown();
        }
    }
}
