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
import io.teaql.tools.http.HttpOperationalClock;
import io.teaql.tools.http.JdkHttpRequestService;
import io.teaql.tools.http.impl.JdkHttpTool;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpResilienceExampleTest {
    private static final String TRACEPARENT =
            "00-11111111111111111111111111111111-aaaaaaaaaaaaaaaa-01";

    @Test public void postedRequestWithStalledResponseRequiresReconciliation() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        CountDownLatch firstByteSent = new CountDownLatch(1);
        CountDownLatch releaseResponse = new CountDownLatch(1);
        AtomicLong monotonic = new AtomicLong(1);
        AtomicReference<Throwable> result = new AtomicReference<>();
        AtomicReference<Thread> worker = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        server.createContext("/api/payments", exchange -> {
            requests.incrementAndGet();
            assertEquals("POST", exchange.getRequestMethod());
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, 2);
            try (var output = exchange.getResponseBody()) {
                output.write('x');
                output.flush();
                firstByteSent.countDown();
                try { releaseResponse.await(10, TimeUnit.SECONDS); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            } catch (java.io.IOException ignored) {
                // The client is expected to close the response after its budget expires.
            }
        });
        server.start();
        try {
            var profile = HttpServiceProfile.builder("payments",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/"))
                    .budgets(Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofSeconds(5))
                    .build();
            HttpOperationalClock clock = new HttpOperationalClock() {
                @Override public long nanoTime() { return monotonic.get(); }
                @Override public void pauseNanos(long nanos) { java.util.concurrent.locks.LockSupport.parkNanos(nanos); }
            };
            var service = new JdkHttpRequestService(List.of(profile), clock);
            var context = new DefaultUserContext(null);
            context.putAttribute(HttpRequestService.class.getName(), service);
            var call = HttpRequestService.Call.postText("payments", "payments",
                    "PRIVATE_BODY_CANARY", "submit payment", "confirm external effect");
            Thread request = new Thread(() -> {
                try {
                    context.httpRequests().execute(context, call);
                    result.set(new AssertionError("incomplete response unexpectedly succeeded"));
                } catch (Throwable failure) { result.set(failure); }
            });
            worker.set(request); request.start();
            assertTrue("POST must reach the fixture before expiring the clock",
                    firstByteSent.await(5, TimeUnit.SECONDS));
            monotonic.addAndGet(Duration.ofSeconds(6).toNanos());
            request.join(2000);
            assertFalse("expired HTTP request did not stop", request.isAlive());
            assertTrue(result.get() instanceof HttpRequestFailure);
            assertEquals(HttpRequestFailure.Code.AMBIGUOUS_OUTCOME, ((HttpRequestFailure) result.get()).code());
            assertEquals(1, requests.get());
        } finally {
            releaseResponse.countDown();
            Thread request = worker.get();
            if (request != null && request.isAlive()) { request.interrupt(); request.join(2000); }
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

            for (int round = 1; round <= 2; round++) {
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
            assertEquals(2 * round, requests.get());
            assertEquals(java.util.Collections.nCopies(2 * round, TRACEPARENT), traceparents);
            assertEquals(2 * round, observed.size());
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            int offset = 2 * (round - 1);
            assertEquals(3, mapper.readTree(observed.get(offset).traceChain()).size());
            assertEquals("load order graph",
                    mapper.readTree(observed.get(offset).traceChain()).get(1).get("comment").asText());
            assertEquals(2, mapper.readTree(observed.get(offset + 1).traceChain()).size());
            assertFalse(observed.toString().contains("PRIVATE_QUERY_CANARY"));
            assertFalse(observed.get(offset).traceChain().contains("PRIVATE_QUERY_CANARY"));
            System.out.println("HTTP_SAME_FIXTURE_PASS language=java round=" + round
                    + " requests=" + requests.get() + " trace=" + observed.size() + " lineage=3");
            }
        } finally {
            server.stop(0);
        }
    }
}
