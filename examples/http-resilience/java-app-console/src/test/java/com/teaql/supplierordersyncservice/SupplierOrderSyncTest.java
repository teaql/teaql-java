package com.teaql.supplierordersyncservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.teaql.core.UserContext;
import io.teaql.core.http.HttpObservation;
import io.teaql.core.http.HttpRequestFailure;
import io.teaql.core.http.HttpRequestService;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.tools.http.HttpServiceProfile;
import io.teaql.tools.http.JdkHttpRequestService;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class SupplierOrderSyncTest {
    private static final String SUPPLIER_PROFILE = "supplier-orders";

    private record Scenario(String name, int attempts, String status) {
        String reference(String round) { return "SYNC-" + name + "-" + round; }
        boolean canRetry() { return Set.of("retry", "failed", "cancel").contains(name); }
    }

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("direct", 1, "acknowledged"),
            new Scenario("retry", 2, "dispatched"),
            new Scenario("failed", 2, "pending"),
            new Scenario("cancel", 1, "pending"),
            new Scenario("mismatch", 1, "pending"));

    private static void serve(HttpExchange exchange, String round, Map<String, Integer> attempts,
                              CountDownLatch arrived, CountDownLatch release) throws IOException {
        try (exchange) {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("probe=PRIVATE_QUERY_SYNC", exchange.getRequestURI().getRawQuery());
            String reference = exchange.getRequestURI().getPath().substring("/api/orders/".length());
            var scenario = SCENARIOS.stream().filter(item -> item.reference(round).equals(reference))
                    .findFirst().orElseThrow();
            int attempt = attempts.merge(reference, 1, Integer::sum);
            if (scenario.name().equals("cancel")) {
                arrived.countDown();
                try {
                    assertTrue(release.await(8, TimeUnit.SECONDS));
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            boolean unavailable = scenario.name().equals("failed") || (scenario.name().equals("retry") && attempt == 1);
            boolean wrongOrder = scenario.name().equals("mismatch");
            var body = ("{\"reference\":\"" + (wrongOrder ? "OTHER-ORDER" : reference)
                    + "\",\"status\":\"" + scenario.status() + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(unavailable ? 503 : 200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private static HttpRequestService.Call request(Scenario scenario, String reference) {
        return new HttpRequestService.Call(SUPPLIER_PROFILE, HttpRequestService.Method.GET,
                "orders/" + reference + "?probe=PRIVATE_QUERY_SYNC", Map.of(), null, null,
                "Read dispatch status from the named supplier", HttpRequestService.IntentKind.PURPOSE,
                "Reconcile the procurement order with supplier status",
                scenario.canRetry() ? HttpRequestService.RetryMode.CONFIRMED_IDEMPOTENT
                        : HttpRequestService.RetryMode.NONE);
    }

    private static void cancel(UserContext context, HttpRequestService.Call call,
                               CountDownLatch arrived, CountDownLatch release) throws InterruptedException {
        var result = new AtomicReference<Throwable>();
        var worker = new Thread(() -> {
            try {
                context.httpRequests().execute(context, call);
            } catch (Throwable failure) {
                result.set(failure);
            }
        });
        worker.start();
        try {
            assertTrue("The supplier must receive the order request before cancelling",
                    arrived.await(5, TimeUnit.SECONDS));
            worker.interrupt();
            worker.join(4000);
            assertFalse(worker.isAlive());
            assertTrue(result.get() instanceof HttpRequestFailure);
            assertEquals(HttpRequestFailure.Code.CANCELLED, ((HttpRequestFailure) result.get()).code());
        } finally {
            release.countDown();
            if (worker.isAlive()) {
                worker.interrupt();
                worker.join(8500);
            }
            assertFalse("The example owns the cancelled worker", worker.isAlive());
        }
    }

    @Test public void applyOnlyTheMatchingSupplierResponseToThePersistedOrder() throws Exception {
        String round = System.getenv("TEAQL_SHOWCASE_ROUND");
        var source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + System.getenv("TEAQL_SUPPLIER_SYNC_DATABASE"));
        var jdbc = new JdbcSqlExecutor(source);
        var sqlite = new SqliteDataServiceExecutor("sqlite", jdbc, source);
        var generator = new IdSpaceIdGenerator(new WorkspaceTeaQLDatabase(jdbc, source));
        generator.ensureIdSpaceTable();
        var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", sqlite).dataService("sqlite", sqlite)
                .idGenerationService(generator).build().install(GeneratedRuntimeModule.module());
        var context = new DefaultUserContext(runtime);
        context.ensureSchema();
        var supplier = Q.suppliersWithMinimalFields().withIdIs(1L).limit(1)
                .comment("Read the model-initialized supplier").purpose("Link orders to the configured supplier")
                .executeForOne(context);
        assertNotNull(supplier);

        var attempts = new ConcurrentHashMap<String, Integer>();
        var observed = new CopyOnWriteArrayList<HttpObservation.Operation>();
        var arrived = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/orders/", exchange -> serve(exchange, round, attempts, arrived, release));
        server.start();
        try {
            var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/");
            var profile = HttpServiceProfile.builder(SUPPLIER_PROFILE, uri)
                    .budgets(Duration.ofSeconds(2), Duration.ofSeconds(7), Duration.ofSeconds(8))
                    .retry(2, Duration.ofMillis(5), Set.of(HttpRequestService.Method.GET)).build();
            context.putAttribute(HttpRequestService.class.getName(), new JdkHttpRequestService(List.of(profile)));
            context.putAttribute(HttpObservation.class.getName(), (HttpObservation) operation -> {
                observed.add(operation);
                return (outcome, number, elapsedMs) -> {};
            });
            var ids = new LinkedHashMap<String, Long>();
            for (var scenario : SCENARIOS) {
                String reference = scenario.reference(round);
                var order = Q.supplierOrders().comment("Create a procurement order to reconcile")
                        .purpose("Retain the pending supplier state until verification").newEntity(context);
                order.updateSupplier(supplier);
                order.updateExternalReference(reference);
                order.updateSyncStatus("pending");
                order.auditAs("Record an order awaiting supplier status").save(context);
                var call = request(scenario, E.supplierOrder(order).getExternalReference().eval());
                if (scenario.name().equals("cancel")) {
                    cancel(context, call, arrived, release);
                } else {
                    var reply = context.httpRequests().execute(context, call);
                    assertEquals(scenario.attempts(), reply.attempts());
                    var payload = (Map<?, ?>) reply.jsonValue();
                    if (scenario.name().equals("failed")) {
                        assertEquals(503, reply.status());
                    } else if (scenario.name().equals("mismatch")) {
                        assertEquals(200, reply.status());
                        assertNotEquals(reference, payload.get("reference"));
                    } else {
                        assertEquals(200, reply.status());
                        assertEquals(reference, payload.get("reference"));
                        assertEquals(scenario.status(), payload.get("status"));
                        order.updateSyncStatus((String) payload.get("status"));
                        order.auditAs("Apply the verified supplier status").save(context);
                    }
                }
                var stored = Q.supplierOrders().withIdIs(order.getId()).limit(1)
                        .comment("Read the order after the supplier call").purpose("Verify the local reconciliation")
                        .executeForOne(context);
                var value = E.supplierOrder(stored);
                assertEquals(reference, value.getExternalReference().eval());
                assertEquals(scenario.status(), value.getSyncStatus().eval());
                assertEquals(Long.valueOf(scenario.status().equals("pending") ? 1 : 2), stored.getVersion());
                assertEquals(Integer.valueOf(scenario.attempts()), attempts.get(reference));
                ids.put(scenario.name(), stored.getId());
            }
            assertEquals(5, observed.size());
            assertFalse(observed.toString().contains("PRIVATE_QUERY_SYNC"));
            assertEquals(7, attempts.values().stream().mapToInt(Integer::intValue).sum());
            Class<?> type = com.teaql.supplierordersyncservice.supplierorder.SupplierOrder.class;
            assertTrue(type.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()
                    .endsWith("/java-lib-core/lib/target/classes/"));
            var marker = Map.of("round", round, "orders", ids, "requests", 7, "cancelled", true);
            System.out.println("SUPPLIER_ORDER_SYNC=" + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(marker));
        } finally {
            release.countDown();
            server.stop(0);
        }
    }
}
