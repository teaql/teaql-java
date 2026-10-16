package io.teaql.tools.http;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.http.HttpPropagation;
import io.teaql.core.http.HttpObservation;
import io.teaql.core.http.HttpRequestFailure;
import io.teaql.core.http.HttpRequestService;
import io.teaql.tools.http.impl.JdkHttpTool;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpResilienceTest {
    private Fixture server;

    @Before public void start() throws Exception { server = new Fixture(); }
    @After public void stop() throws Exception { server.close(); }

    private HttpServiceProfile.Builder profile() {
        return HttpServiceProfile.builder("orders", URI.create("http://127.0.0.1:"
                + server.port() + "/api/"));
    }
    private UserContext context(HttpRequestService service) {
        return context(service, null);
    }
    private UserContext context(HttpRequestService service, HttpPropagation propagation) {
        return context(service, propagation, null);
    }
    private UserContext context(HttpRequestService service, HttpPropagation propagation,
                                HttpObservation observation) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("capability")) {
                if (args[0] == HttpRequestService.class) return service;
                if (args[0] == HttpPropagation.class) return propagation;
                if (args[0] == HttpObservation.class) return observation;
                return null;
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            if (method.getReturnType().isPrimitive()) return false;
            return null;
        };
        return (UserContext) Proxy.newProxyInstance(UserContext.class.getClassLoader(),
                new Class<?>[] {UserContext.class}, handler);
    }
    private static HttpRequestService.Call get(String path) {
        return HttpRequestService.Call.get("orders", path, "load order status", "show current order");
    }

    @Test public void brokenObservationCannotChangeTransportOutcome() {
        var service = new JdkHttpRequestService(java.util.List.of(profile().build()));
        HttpObservation brokenStart = operation -> { throw new IllegalStateException("observer failed"); };
        assertEquals(200, service.execute(context(service, null, brokenStart), get("ok")).status());
        HttpObservation brokenFinish = operation -> (outcome, attempts, elapsed) -> {
            throw new IllegalStateException("observer failed");
        };
        assertEquals(200, service.execute(context(service, null, brokenFinish), get("ok")).status());
        assertEquals(2, server.requests.get());
    }
    private static HttpRequestService.Call call(HttpRequestService.Method method, String path,
                                                HttpRequestService.RetryMode retry) {
        return new HttpRequestService.Call("orders", method, path, Map.of(), null, null,
                "call order service", HttpRequestService.IntentKind.PURPOSE,
                "resolve order", retry);
    }

    @Test public void invalidOrUnknownRequestsNeverReachTheNetwork() {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile().build()));
        UserContext context = context(service);
        assertEquals(HttpRequestFailure.Code.INVALID_INPUT, assertThrows(HttpRequestFailure.class,
                () -> HttpRequestService.Call.get("orders", "ok", " ", "purpose")).code());
        assertEquals(HttpRequestFailure.Code.PROFILE_NOT_CONFIGURED, assertThrows(HttpRequestFailure.class,
                () -> service.execute(context, HttpRequestService.Call.get("other", "ok", "comment", "purpose"))).code());
        for (String path : java.util.List.of("../secret", "%2e%2e/secret", "/escape", "//other/", "http://other/")) {
            assertEquals(path, HttpRequestFailure.Code.INVALID_INPUT, assertThrows(HttpRequestFailure.class,
                    () -> service.execute(context, get(path))).code());
        }
        HttpRequestService.Call header = new HttpRequestService.Call("orders",
                HttpRequestService.Method.GET, "ok", Map.of("X-Private", "bad\r\nInjected: value"),
                null, null, "comment", HttpRequestService.IntentKind.PURPOSE,
                "purpose", HttpRequestService.RetryMode.NONE);
        assertEquals(HttpRequestFailure.Code.INVALID_INPUT, assertThrows(HttpRequestFailure.class,
                () -> service.execute(context, header)).code());
        for (boolean nullKey : new boolean[] {true, false}) {
            Map<String, String> invalidHeaders = new java.util.HashMap<>();
            invalidHeaders.put(nullKey ? null : "X-Private", nullKey ? "value" : null);
            assertEquals(HttpRequestFailure.Code.INVALID_INPUT, assertThrows(HttpRequestFailure.class,
                    () -> new HttpRequestService.Call("orders", HttpRequestService.Method.GET,
                            "ok", invalidHeaders, null, null, "comment",
                            HttpRequestService.IntentKind.PURPOSE, "purpose",
                            HttpRequestService.RetryMode.NONE)).code());
        }
        assertEquals(0, server.requests.get());
    }

    @Test public void defaultCallsUseOneAttemptAndNeverFollowRedirects() {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile().build()));
        UserContext context = context(service);
        HttpRequestService.Response get = service.execute(context, get("ok"));
        assertEquals(200, get.status());
        assertEquals("okay", get.text());
        assertEquals(1, get.attempts());
        HttpRequestService.Response post = service.execute(context,
                HttpRequestService.Call.postText("orders", "ok", "PRIVATE-BODY-CANARY",
                        "submit order", "send order"));
        assertEquals(200, post.status());
        assertEquals(1, post.attempts());
        HttpRequestService.Response redirect = service.execute(context, get("redirect"));
        assertEquals(302, redirect.status());
        assertEquals(3, server.requests.get());
    }

    @Test public void retryIsExplicitAndPostTransportFailureIsAmbiguous() {
        HttpServiceProfile configured = profile().retry(3, Duration.ofMillis(1),
                Set.of(HttpRequestService.Method.GET)).build();
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(configured));
        UserContext context = context(service);
        HttpRequestService.Response first = service.execute(context, get("flaky"));
        assertEquals(503, first.status());
        assertEquals(1, first.attempts());
        HttpRequestService.Response retried = service.execute(context,
                call(HttpRequestService.Method.GET, "flaky", HttpRequestService.RetryMode.CONFIRMED_IDEMPOTENT));
        assertEquals(200, retried.status());
        assertEquals(2, retried.attempts());
        int before = server.requests.get();
        assertEquals(HttpRequestFailure.Code.AMBIGUOUS_OUTCOME,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context,
                        call(HttpRequestService.Method.POST, "close", HttpRequestService.RetryMode.NONE))).code());
        assertEquals(before + 1, server.requests.get());
    }

    @Test public void replayablePostKeepsItsBodyAndBackoffObeysTheWholeCallBudget() {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .retry(3, Duration.ofMillis(1), Set.of(HttpRequestService.Method.POST)).build()));
        byte[] payload = "REPLAY-PAYLOAD-CANARY".getBytes(StandardCharsets.UTF_8);
        HttpRequestService.Call replayable = new HttpRequestService.Call("orders",
                HttpRequestService.Method.POST, "retry-post", Map.of(), payload, "text/plain",
                "submit idempotent request", HttpRequestService.IntentKind.PURPOSE,
                "verify request replay", HttpRequestService.RetryMode.CONFIRMED_IDEMPOTENT);
        int before = server.requests.get();
        HttpRequestService.Response response = service.execute(context(service), replayable);
        assertEquals(200, response.status());
        assertEquals(2, response.attempts());
        assertEquals(before + 2, server.requests.get());
        assertEquals(java.util.List.of("REPLAY-PAYLOAD-CANARY", "REPLAY-PAYLOAD-CANARY"),
                java.util.List.copyOf(server.retryBodies));

        AtomicLong monotonic = new AtomicLong(1);
        HttpOperationalClock clock = new HttpOperationalClock() {
            @Override public long nanoTime() { return monotonic.get(); }
            @Override public void pauseNanos(long nanos) { monotonic.addAndGet(nanos); }
        };
        JdkHttpRequestService budgeted = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofMillis(500))
                .retry(3, Duration.ofSeconds(1), Set.of(HttpRequestService.Method.GET)).build()), clock);
        before = server.requests.get();
        assertEquals(HttpRequestFailure.Code.TIMEOUT,
                assertThrows(HttpRequestFailure.class, () -> budgeted.execute(context(budgeted),
                        call(HttpRequestService.Method.GET, "fail",
                                HttpRequestService.RetryMode.CONFIRMED_IDEMPOTENT))).code());
        assertEquals("backoff must not start another attempt after total expiry", before + 1, server.requests.get());
    }

    @Test public void wholeCallTimeoutAndBoundedBodyFailClosed() {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofMillis(200), Duration.ofMillis(120), Duration.ofMillis(150))
                .maxResponseBytes(16).build()));
        UserContext context = context(service);
        assertEquals(HttpRequestFailure.Code.RESPONSE_TOO_LARGE,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context, get("large"))).code());
        assertEquals(HttpRequestFailure.Code.TIMEOUT,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context, get("slow"))).code());
        assertEquals(HttpRequestFailure.Code.TIMEOUT,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context, get("slowbody"))).code());
        assertEquals(HttpRequestFailure.Code.MALFORMED_RESPONSE,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context, get("invalid-utf8")).text()).code());
        assertEquals(HttpRequestFailure.Code.MALFORMED_RESPONSE,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context, get("bad-json")).jsonValue()).code());
    }

    @Test public void postedRequestWithStalledResponseBodyIsAmbiguous() {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofMillis(200), Duration.ofMillis(120), Duration.ofMillis(180))
                .build()));
        UserContext context = context(service);
        HttpRequestService.Call call = HttpRequestService.Call.postText("orders", "slowbody",
                "PRIVATE_BODY_CANARY", "submit payment", "confirm external effect");
        int before = server.requests.get();
        assertEquals(HttpRequestFailure.Code.AMBIGUOUS_OUTCOME,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context, call)).code());
        assertEquals(before + 1, server.requests.get());
    }

    @Test public void postBudgetExpiryBeforeSendRemainsTimeoutWithoutRetry() {
        AtomicInteger reads = new AtomicInteger();
        HttpOperationalClock clock = new HttpOperationalClock() {
            @Override public long nanoTime() {
                // State construction, execute start and the outer budget read
                // are valid; the inner read expires before sendAsync starts.
                return reads.incrementAndGet() <= 3 ? 0 : Duration.ofSeconds(2).toNanos();
            }
            @Override public void pauseNanos(long nanos) { fail("no request or retry may start"); }
        };
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofMillis(200), Duration.ofMillis(200), Duration.ofSeconds(1))
                .retry(2, Duration.ofMillis(1), Set.of(HttpRequestService.Method.POST))
                .build()), clock);
        HttpRequestService.Call call = new HttpRequestService.Call("orders",
                HttpRequestService.Method.POST, "ok", Map.of(),
                "PRIVATE_BODY_CANARY".getBytes(StandardCharsets.UTF_8), "text/plain",
                "submit payment", HttpRequestService.IntentKind.PURPOSE,
                "confirm external effect", HttpRequestService.RetryMode.CONFIRMED_IDEMPOTENT);
        assertEquals(HttpRequestFailure.Code.TIMEOUT,
                assertThrows(HttpRequestFailure.class,
                        () -> service.execute(context(service), call)).code());
        assertEquals(0, server.requests.get());
    }

    @Test public void stalledTlsHandshakeObeysConnectionBudget() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1,
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}))) {
            listener.setSoTimeout(3000);
            AtomicReference<Throwable> serverFailure = new AtomicReference<>();
            Thread stalled = new Thread(() -> {
                try (Socket ignored = listener.accept()) {
                    Thread.sleep(1200);
                } catch (Throwable failure) {
                    serverFailure.set(failure);
                }
            });
            stalled.start();
            HttpServiceProfile secure = HttpServiceProfile.builder("secure",
                    URI.create("https://127.0.0.1:" + listener.getLocalPort() + "/api/"))
                    .budgets(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofSeconds(2))
                    .build();
            JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(secure));
            long started = System.nanoTime();
            HttpRequestFailure failure = assertThrows(HttpRequestFailure.class,
                    () -> service.execute(context(service), HttpRequestService.Call.get(
                            "secure", "health", "check upstream", "assess health")));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            stalled.join(2500);
            assertFalse("TLS fixture thread did not stop", stalled.isAlive());
            assertNull(serverFailure.get());
            assertEquals(HttpRequestFailure.Code.TIMEOUT, failure.code());
            assertTrue("connection phase used request budget: " + elapsedMs + "ms", elapsedMs < 900);
        }
    }

    @Test public void droppedTcpSynObeysConnectionBudget() {
        Assume.assumeTrue("requires the isolated SYN-drop network fixture",
                "1".equals(System.getenv("TEAQL_HTTP_SYN_DROP_FIXTURE")));
        HttpServiceProfile blackhole = HttpServiceProfile.builder("blackhole",
                URI.create("http://127.0.0.2:54321/api/"))
                .budgets(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofSeconds(2))
                .build();
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(blackhole));
        long started = System.nanoTime();
        HttpRequestFailure failure = assertThrows(HttpRequestFailure.class,
                () -> service.execute(context(service), HttpRequestService.Call.get(
                        "blackhole", "health", "check upstream", "assess health")));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertEquals(HttpRequestFailure.Code.TIMEOUT, failure.code());
        assertNull(failure.getCause());
        assertTrue("connect budget was not enforced: " + elapsedMs + "ms", elapsedMs < 900);
    }

    @Test public void cancellationStopsAnActiveRequestWithoutRetry() throws Exception {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofSeconds(2))
                .retry(3, Duration.ofMillis(1), Set.of(HttpRequestService.Method.GET)).build()));
        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                service.execute(context(service), call(HttpRequestService.Method.GET,
                        "slow", HttpRequestService.RetryMode.CONFIRMED_IDEMPOTENT));
            } catch (Throwable failure) { result.set(failure); }
        });
        worker.start();
        long deadline = System.nanoTime() + Duration.ofSeconds(1).toNanos();
        while (server.requests.get() == 0 && System.nanoTime() < deadline) Thread.sleep(5);
        assertEquals(1, server.requests.get());
        worker.interrupt();
        worker.join(1000);
        assertFalse(worker.isAlive());
        assertTrue(result.get() instanceof HttpRequestFailure);
        assertEquals(HttpRequestFailure.Code.CANCELLED, ((HttpRequestFailure) result.get()).code());
        assertEquals(1, server.requests.get());
    }

    @Test public void cancellationDuringBackoffCannotReplayTheRequest() throws Exception {
        BlockingClock clock = new BlockingClock();
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofSeconds(3))
                .retry(3, Duration.ofSeconds(1), Set.of(HttpRequestService.Method.GET)).build()), clock);
        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try { service.execute(context(service), call(HttpRequestService.Method.GET,
                    "fail", HttpRequestService.RetryMode.CONFIRMED_IDEMPOTENT)); }
            catch (Throwable failure) { result.set(failure); }
        });
        worker.start();
        assertTrue("first failure should enter backoff", clock.paused.await(1, TimeUnit.SECONDS));
        assertEquals(1, server.requests.get());
        worker.interrupt();
        worker.join(1000);
        assertFalse(worker.isAlive());
        assertTrue(result.get() instanceof HttpRequestFailure);
        assertEquals(HttpRequestFailure.Code.CANCELLED, ((HttpRequestFailure) result.get()).code());
        assertEquals(1, server.requests.get());
    }

    @Test public void cancellationDuringRateWaitCannotStartAnotherRequest() throws Exception {
        BlockingClock clock = new BlockingClock();
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofSeconds(3))
                .ratePerSecond(1).build()), clock);
        UserContext context = context(service);
        assertEquals(200, service.execute(context, get("ok")).status());
        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try { service.execute(context, get("ok")); }
            catch (Throwable failure) { result.set(failure); }
        });
        worker.start();
        assertTrue("second request should wait for token", clock.paused.await(1, TimeUnit.SECONDS));
        assertEquals(1, server.requests.get());
        worker.interrupt();
        worker.join(1000);
        assertFalse(worker.isAlive());
        assertTrue(result.get() instanceof HttpRequestFailure);
        assertEquals(HttpRequestFailure.Code.CANCELLED, ((HttpRequestFailure) result.get()).code());
        assertEquals(1, server.requests.get());
    }

    private static final class BlockingClock implements HttpOperationalClock {
        final CountDownLatch paused = new CountDownLatch(1);
        @Override public long nanoTime() { return System.nanoTime(); }
        @Override public void pauseNanos(long nanos) {
            paused.countDown();
            LockSupport.parkNanos(nanos);
        }
    }

    @Test public void circuitAndRateAreProfileShared() throws Exception {
        JdkHttpRequestService circuit = new JdkHttpRequestService(java.util.List.of(profile()
                .circuit(2, Duration.ofMillis(80), 1).build()));
        UserContext context = context(circuit);
        assertEquals(404, circuit.execute(context, get("missing")).status());
        assertEquals(404, circuit.execute(context, get("missing")).status());
        assertEquals(503, circuit.execute(context, get("fail")).status());
        assertEquals(503, circuit.execute(context, get("fail")).status());
        int before = server.requests.get();
        assertEquals(HttpRequestFailure.Code.CIRCUIT_OPEN,
                assertThrows(HttpRequestFailure.class, () -> circuit.execute(context, get("ok"))).code());
        assertEquals(before, server.requests.get());
        Thread.sleep(100);
        assertEquals(200, circuit.execute(context, get("ok")).status());
        JdkHttpRequestService limited = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofMillis(200), Duration.ofMillis(200), Duration.ofMillis(200))
                .ratePerSecond(1).build()));
        UserContext limitedContext = context(limited);
        assertEquals(200, limited.execute(limitedContext, get("ok")).status());
        before = server.requests.get();
        assertEquals(HttpRequestFailure.Code.RATE_LIMITED,
                assertThrows(HttpRequestFailure.class, () -> limited.execute(limitedContext, get("ok"))).code());
        assertEquals(before, server.requests.get());
    }

    @Test public void circuitRecoveryUsesInjectedOperationalClock() {
        AtomicLong monotonic = new AtomicLong(1);
        HttpOperationalClock clock = new HttpOperationalClock() {
            @Override public long nanoTime() { return monotonic.get(); }
            @Override public void pauseNanos(long nanos) { monotonic.addAndGet(nanos); }
        };
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .circuit(1, Duration.ofSeconds(5), 1).build()), clock);
        BusinessClock backdated = ignored -> LocalDate.of(1999, 1, 1);
        UserContext context = contextWithBusinessClock(service, backdated);
        assertEquals(LocalDate.of(1999, 1, 1), context.businessDate());
        assertEquals(503, service.execute(context, get("fail")).status());
        int before = server.requests.get();
        assertEquals(HttpRequestFailure.Code.CIRCUIT_OPEN,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context, get("ok"))).code());
        assertEquals(before, server.requests.get());
        monotonic.addAndGet(Duration.ofSeconds(5).toNanos());
        assertEquals(200, service.execute(context, get("ok")).status());
    }

    @Test public void halfOpenAllowsOneProbeAndCancellationReleasesIt() throws Exception {
        AtomicLong monotonic = new AtomicLong(1);
        HttpOperationalClock clock = new HttpOperationalClock() {
            @Override public long nanoTime() { return monotonic.get(); }
            @Override public void pauseNanos(long nanos) { monotonic.addAndGet(nanos); }
        };
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile()
                .budgets(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofSeconds(2))
                .circuit(1, Duration.ofSeconds(5), 1).build()), clock);
        UserContext context = context(service);
        assertEquals(503, service.execute(context, get("fail")).status());
        monotonic.addAndGet(Duration.ofSeconds(5).toNanos());
        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread probe = new Thread(() -> {
            try { service.execute(context, get("slow")); }
            catch (Throwable failure) { result.set(failure); }
        });
        probe.start();
        long deadline = System.nanoTime() + Duration.ofSeconds(1).toNanos();
        while (server.requests.get() < 2 && System.nanoTime() < deadline) Thread.sleep(5);
        assertEquals(2, server.requests.get());
        assertEquals(HttpRequestFailure.Code.CIRCUIT_OPEN,
                assertThrows(HttpRequestFailure.class, () -> service.execute(context, get("ok"))).code());
        probe.interrupt();
        probe.join(1000);
        assertFalse(probe.isAlive());
        assertTrue(result.get() instanceof HttpRequestFailure);
        assertEquals(HttpRequestFailure.Code.CANCELLED, ((HttpRequestFailure) result.get()).code());
        assertEquals(200, service.execute(context, get("ok")).status());
    }

    @Test public void sharedClientKeepsConcurrentRequestCarriersSeparate() throws Exception {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile().build()));
        String firstTrace = "00-11111111111111111111111111111111-aaaaaaaaaaaaaaaa-01";
        String secondTrace = "00-22222222222222222222222222222222-bbbbbbbbbbbbbbbb-01";
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ConcurrentLinkedQueue<String> diagnostics = new ConcurrentLinkedQueue<>();
        Logger logger = Logger.getLogger(JdkHttpRequestService.class.getName());
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) { diagnostics.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(capture);
        try {
            Thread first = concurrentCall(service, firstTrace, "first request", start, failure);
            Thread second = concurrentCall(service, secondTrace, "second request", start, failure);
            first.start();
            second.start();
            start.countDown();
            first.join(2000);
            second.join(2000);
            assertFalse(first.isAlive());
            assertFalse(second.isAlive());
            if (failure.get() != null) throw new AssertionError(failure.get());
            assertEquals(2, server.traceparents.size());
            assertTrue(server.traceparents.contains(firstTrace));
            assertTrue(server.traceparents.contains(secondTrace));
            assertEquals(1, diagnostics.stream().filter(message -> message.contains("comment=first request")).count());
            assertEquals(1, diagnostics.stream().filter(message -> message.contains("comment=second request")).count());
            assertTrue(diagnostics.stream().noneMatch(message -> message.contains("comment=first request")
                    && message.contains("second request")));
            assertTrue(diagnostics.stream().noneMatch(message -> message.contains("comment=second request")
                    && message.contains("first request")));
        } finally {
            logger.removeHandler(capture);
        }
    }

    private static Thread concurrentCall(JdkHttpRequestService service, String trace, String comment,
                                         CountDownLatch start, AtomicReference<Throwable> failure) {
        return new Thread(() -> {
            try {
                start.await();
                UserContext context = contextForTrace(service, trace);
                assertEquals(200, service.execute(context,
                        HttpRequestService.Call.get("orders", "ok", comment, "show current order")).status());
            } catch (Throwable error) { failure.compareAndSet(null, error); }
        });
    }

    private static UserContext contextForTrace(JdkHttpRequestService service, String trace) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("capability")) {
                if (args[0] == HttpRequestService.class) return service;
                if (args[0] == HttpPropagation.class) return (HttpPropagation) carrier -> carrier.put("traceparent", trace);
                return null;
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            if (method.getReturnType().isPrimitive()) return false;
            return null;
        };
        return (UserContext) Proxy.newProxyInstance(UserContext.class.getClassLoader(),
                new Class<?>[] {UserContext.class}, handler);
    }

    private static UserContext contextWithBusinessClock(JdkHttpRequestService service, BusinessClock clock) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("capability")) {
                if (args[0] == HttpRequestService.class) return service;
                if (args[0] == BusinessClock.class) return clock;
                return null;
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            if (method.getReturnType().isPrimitive()) return false;
            return null;
        };
        return (UserContext) Proxy.newProxyInstance(UserContext.class.getClassLoader(),
                new Class<?>[] {UserContext.class}, handler);
    }

    @Test public void toolUsesTheSameServiceAndSafeCarrierAndLogs() {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile().build()));
        UserContext context = context(service, carrier -> carrier.put("traceparent", "00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01"));
        Logger logger = Logger.getLogger(JdkHttpRequestService.class.getName());
        StringBuilder log = new StringBuilder();
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) { log.append(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(capture);
        try {
            assertEquals("okay", new JdkHttpTool(context).get("orders", "ok")
                    .comment("load order").purpose("show order").execute());
            HttpRequestService.Call privateCall = new HttpRequestService.Call("orders",
                    HttpRequestService.Method.POST, "ok?token=PRIVATE_QUERY_CANARY",
                    Map.of("Authorization", "Bearer PRIVATE_HEADER_CANARY"),
                    "PRIVATE_BODY_CANARY".getBytes(StandardCharsets.UTF_8), "text/plain",
                    "send order", HttpRequestService.IntentKind.PURPOSE,
                    "confirm order", HttpRequestService.RetryMode.NONE);
            assertEquals(200, service.execute(context, privateCall).status());
            assertEquals(HttpRequestFailure.Code.INVALID_INPUT,
                    assertThrows(HttpRequestFailure.class, () -> new JdkHttpTool(context)
                            .get("http://127.0.0.1/secret").comment("load").purpose("show").execute()).code());
            assertEquals(HttpRequestFailure.Code.INVALID_INPUT,
                    assertThrows(HttpRequestFailure.class, () -> new JdkHttpTool(context)
                            .get("orders", "ok").purpose("show").execute()).code());
            assertTrue(server.traceparents.contains("00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01"));
            assertFalse(log.toString().contains("127.0.0.1"));
            assertFalse(log.toString().contains("PRIVATE-BODY-CANARY"));
            assertFalse(log.toString().contains("PRIVATE_QUERY_CANARY"));
            assertFalse(log.toString().contains("PRIVATE_HEADER_CANARY"));
            assertFalse(log.toString().contains("PRIVATE_BODY_CANARY"));
        } finally { logger.removeHandler(capture); }
    }

    @Test public void privateTransportMatrixStaysOutOfDiagnosticsAndPublicErrors() {
        JdkHttpRequestService service = new JdkHttpRequestService(java.util.List.of(profile().build()));
        ConcurrentLinkedQueue<String> diagnostics = new ConcurrentLinkedQueue<>();
        HttpObservation observation = operation -> {
            diagnostics.add(operation.profile() + operation.method() + operation.comment()
                    + operation.intent() + operation.traceChain() + operation);
            return (outcome, attempts, elapsed) -> diagnostics.add(outcome + attempts + elapsed);
        };
        UserContext context = context(service, null, observation);
        Logger logger = Logger.getLogger(JdkHttpRequestService.class.getName());
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) { diagnostics.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(capture);
        try {
            for (int index = 0; index < 12; index++) {
                String canary = "HTTP_PRIVACY_" + index + "_CANARY";
                HttpRequestService.Call call = new HttpRequestService.Call("orders",
                        HttpRequestService.Method.POST,
                        "private-response/" + canary + "?token=" + canary,
                        Map.of("Authorization", "Bearer " + canary, "Cookie", "sid=" + canary),
                        canary.getBytes(StandardCharsets.UTF_8), "application/x-" + canary,
                        "check remote order", HttpRequestService.IntentKind.PURPOSE,
                        "show order status", HttpRequestService.RetryMode.NONE);
                HttpRequestService.Response response = service.execute(context, call);
                assertEquals(503, response.status());
                assertEquals("REMOTE_RESPONSE_CANARY", response.text());
                assertFalse(call.toString().contains(canary));
                assertFalse(response.toString().contains("REMOTE_RESPONSE_CANARY"));

                HttpRequestFailure invalid = assertThrows(HttpRequestFailure.class,
                        () -> service.execute(context, get("../" + canary)));
                assertEquals(HttpRequestFailure.Code.INVALID_INPUT, invalid.code());
                assertNull(invalid.getCause());
                assertFalse(invalid.toString().contains(canary));

                HttpRequestService.Call dropped = new HttpRequestService.Call("orders",
                        HttpRequestService.Method.POST, "close?token=" + canary,
                        Map.of("Authorization", "Bearer " + canary),
                        canary.getBytes(StandardCharsets.UTF_8), "text/plain",
                        "check remote order", HttpRequestService.IntentKind.PURPOSE,
                        "show order status", HttpRequestService.RetryMode.NONE);
                HttpRequestFailure transport = assertThrows(HttpRequestFailure.class,
                        () -> service.execute(context, dropped));
                assertEquals(HttpRequestFailure.Code.AMBIGUOUS_OUTCOME, transport.code());
                assertNull(transport.getCause());
                assertFalse(transport.toString().contains(canary));
            }
            String exported = String.join("\n", diagnostics);
            assertTrue(exported.contains("comment=check remote order"));
            assertFalse(exported.contains("REMOTE_REASON_CANARY"));
            assertFalse(exported.contains("REMOTE_RESPONSE_CANARY"));
            for (int index = 0; index < 12; index++) {
                assertFalse(exported.contains("HTTP_PRIVACY_" + index + "_CANARY"));
            }
        } finally {
            logger.removeHandler(capture);
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final ServerSocket socket;
        private final Thread thread;
        private final AtomicInteger requests = new AtomicInteger();
        private final AtomicInteger flaky = new AtomicInteger();
        private final AtomicInteger retryPost = new AtomicInteger();
        private final ConcurrentLinkedQueue<String> traceparents = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> retryBodies = new ConcurrentLinkedQueue<>();
        private volatile boolean closed;
        Fixture() throws IOException {
            socket = new ServerSocket(0, 32, InetAddress.getLoopbackAddress());
            socket.setSoTimeout(100);
            thread = new Thread(this::serve, "teaql-http-fixture");
            thread.start();
        }
        int port() { return socket.getLocalPort(); }
        private void serve() {
            while (!closed) {
                try {
                    Socket client = socket.accept();
                    Thread handler = new Thread(() -> handle(client), "teaql-http-fixture-request");
                    handler.setDaemon(true);
                    handler.start();
                } catch (SocketTimeoutException ignored) {
                } catch (IOException ignored) {
                    if (closed) break;
                }
            }
        }
        private void handle(Socket socket) {
                try (Socket client = socket) {
                    client.setSoTimeout(1000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    String line = reader.readLine();
                    if (line == null) return;
                    requests.incrementAndGet();
                    String target = line.split(" ")[1];
                    int contentLength = 0;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        if (line.toLowerCase(java.util.Locale.ROOT).startsWith("traceparent:"))
                            traceparents.add(line.substring(line.indexOf(':') + 1).trim());
                        if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:"))
                            contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                    }
                    if (target.contains("/retry-post")) {
                        char[] body = new char[contentLength];
                        int offset = 0;
                        while (offset < body.length) {
                            int count = reader.read(body, offset, body.length - offset);
                            if (count < 0) return;
                            offset += count;
                        }
                        retryBodies.add(new String(body));
                    }
                    if (target.contains("/close")) return;
                    if (target.endsWith("/slow")) Thread.sleep(300);
                    int status = target.contains("/private-response") ? 503
                            : target.contains("/redirect") ? 302 : target.contains("/missing") ? 404
                            : target.contains("/retry-post") && retryPost.incrementAndGet() == 1 ? 503
                            : target.contains("/fail") || target.contains("/flaky") && flaky.incrementAndGet() <= 2
                            ? 503 : 200;
                    byte[] body = target.contains("/private-response")
                            ? "REMOTE_RESPONSE_CANARY".getBytes(StandardCharsets.UTF_8)
                            : target.contains("/large") ? "L".repeat(4096).getBytes(StandardCharsets.UTF_8)
                            : target.contains("/invalid-utf8") ? new byte[] {(byte) 0xc3, 0x28}
                            : target.contains("/bad-json") ? "{bad".getBytes(StandardCharsets.UTF_8)
                            : "okay".getBytes(StandardCharsets.UTF_8);
                    String reason = target.contains("/private-response") ? "REMOTE_REASON_CANARY" : "Result";
                    String head = "HTTP/1.1 " + status + " " + reason + "\r\nContent-Length: " + body.length
                            + "\r\nConnection: close\r\n"
                            + (status == 302 ? "Location: https://elsewhere.example/secret\r\n" : "") + "\r\n";
                    client.getOutputStream().write(head.getBytes(StandardCharsets.UTF_8));
                    if (target.contains("/slowbody")) {
                        client.getOutputStream().write(body, 0, 1);
                        client.getOutputStream().flush();
                        Thread.sleep(300);
                        client.getOutputStream().write(body, 1, body.length - 1);
                    } else {
                        client.getOutputStream().write(body);
                    }
                    client.getOutputStream().flush();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (IOException ignored) {
                    // A timed-out or cancelled client may close the fixture socket.
                }
        }
        @Override public void close() throws Exception {
            closed = true;
            socket.close();
            thread.join(1000);
        }
    }
}
