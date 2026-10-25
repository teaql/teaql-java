package io.teaql.examples.http;

import com.sun.net.httpserver.HttpServer;
import io.teaql.core.TraceKind;
import io.teaql.core.TraceNode;
import io.teaql.core.http.HttpObservation;
import io.teaql.core.http.HttpPropagation;
import io.teaql.core.http.HttpRequestFailure;
import io.teaql.core.http.HttpRequestService;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.tools.http.HttpServiceProfile;
import io.teaql.tools.http.JdkHttpRequestService;
import io.teaql.tools.http.impl.JdkHttpTool;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpResilienceExampleTest {
    private static final String TRACEPARENT =
            "00-11111111111111111111111111111111-aaaaaaaaaaaaaaaa-01";

    @Test public void postedRequestWithStalledResponseRequiresReconciliation() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        CountDownLatch received = new CountDownLatch(1), release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        server.createContext("/api/payments", exchange -> {
            requests.incrementAndGet();
            assertEquals("POST", exchange.getRequestMethod());
            exchange.getRequestBody().readAllBytes();
            received.countDown();
            exchange.sendResponseHeaders(200, 2);
            try (var output = exchange.getResponseBody()) {
                output.write('x');
                output.flush();
                try { release.await(15, TimeUnit.SECONDS); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            } catch (java.io.IOException ignored) {
                // The client is expected to close the response after its budget expires.
            }
        });
        server.createContext("/api/ready", exchange -> {
            exchange.sendResponseHeaders(200, 1);
            try (var output = exchange.getResponseBody()) { output.write('r'); }
        });
        server.start();
        try {
            var profile = HttpServiceProfile.builder("payments",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/"))
                    .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(5))
                    .build();
            var service = new JdkHttpRequestService(List.of(profile));
            var context = new DefaultUserContext(null);
            context.putAttribute(HttpRequestService.class.getName(), service);
            // Exclude cold HTTP/context initialization from the deliberately
            // stalled-response case. An unstarted request correctly reports
            // TIMEOUT; this test must prove the POST was actually received.
            assertEquals(200, context.httpRequests().execute(context,
                    HttpRequestService.Call.get("payments", "ready", "warm local HTTP transport",
                            "separate initialization from stalled response")).status());
            var call = HttpRequestService.Call.postText("payments", "payments",
                    "PRIVATE_BODY_CANARY", "submit payment", "confirm external effect");
            var failure = assertThrows(HttpRequestFailure.class,
                    () -> context.httpRequests().execute(context, call));
            assertTrue("POST must reach the server before classifying its outcome", received.await(1, TimeUnit.SECONDS));
            assertEquals(HttpRequestFailure.Code.AMBIGUOUS_OUTCOME, failure.code());
            assertEquals(1, requests.get());
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

    @Test public void contextAndAgentToolUseOneControlledService() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        var traceparents = new CopyOnWriteArrayList<String>();
        var observed = new CopyOnWriteArrayList<HttpObservation.Operation>();
        HttpServer server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        server.createContext("/api/orders/1", exchange -> {
            requests.incrementAndGet();
            traceparents.add(exchange.getRequestHeaders().getFirst("traceparent"));
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
            context.putAttribute(HttpPropagation.class.getName(),
                    (HttpPropagation) carrier -> carrier.put("traceparent", TRACEPARENT));
            context.putAttribute(HttpObservation.class.getName(),
                    (HttpObservation) operation -> {
                        observed.add(operation);
                        return (outcome, attempts, elapsedMillis) -> {};
                    });

            var response = context.httpRequests().execute(context,
                    HttpRequestService.Call.get("orders", "orders/1?token=PRIVATE_QUERY_CANARY",
                            "load order status", "show the order status")
                            .withParentTraceChain(List.of(
                                    new TraceNode(TraceKind.PURPOSE, "Order", "render order details"),
                                    new TraceNode(TraceKind.COMMENT, "Order", "load order graph"))));
            assertEquals(200, response.status());
            assertEquals("submitted", ((java.util.Map<?, ?>) response.jsonValue()).get("status"));
            assertEquals(1, response.attempts());

            String viaTool = new JdkHttpTool(context).get("orders", "orders/1")
                    .comment("load order status for agent")
                    .purpose("explain the order status")
                    .execute();
            assertTrue(viaTool.contains("submitted"));
            assertEquals(2, requests.get());
            assertEquals(List.of(TRACEPARENT, TRACEPARENT), traceparents);
            assertEquals(2, observed.size());
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            assertEquals(3, mapper.readTree(observed.get(0).traceChain()).size());
            assertEquals("load order graph",
                    mapper.readTree(observed.get(0).traceChain()).get(1).get("comment").asText());
            assertEquals(2, mapper.readTree(observed.get(1).traceChain()).size());
            assertFalse(observed.toString().contains("PRIVATE_QUERY_CANARY"));
            assertFalse(observed.get(0).traceChain().contains("PRIVATE_QUERY_CANARY"));
        } finally {
            server.stop(0);
        }
    }
}
