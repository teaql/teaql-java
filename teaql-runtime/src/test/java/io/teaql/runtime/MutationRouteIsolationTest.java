package io.teaql.runtime;

import io.teaql.core.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;
import static io.teaql.runtime.GraphTraceChainTest.*;

/** #202: the atomic route boundary belongs to one mutation plan, not Context. */
public class MutationRouteIsolationTest {
    private static DefaultUserContext context(Provider orders, Provider payments, List<SafeAuditEvent> events) {
        var metadata = metadata();
        metadata.resolveEntityDescriptor("Payment").setDataService("payments");
        var runtime = TeaQLRuntime.builder().metadata(metadata)
                .dataService("fixture", orders).dataService("payments", payments).build();
        var context = new DefaultUserContext(runtime);
        context.putAttribute(AppAuditEventSink.class.getName(),
                (AppAuditEventSink) (caller, event) -> events.add(event));
        return context;
    }

    @Test public void independentSequentialSavesCanUseDifferentRoutesOnOneContext() {
        var orders = new Provider(); var payments = new Provider();
        var events = new CopyOnWriteArrayList<SafeAuditEvent>();
        var context = context(orders, payments, events);
        existing("CustomerOrder", 100L).auditAs("independent order").save(context);
        existing("Payment", 100L).auditAs("independent payment").save(context);
        assertEquals(1, orders.requests.size()); assertEquals(1, payments.requests.size());
        assertReasons(events, "CustomerOrder", List.of("independent order"));
        assertReasons(events, "Payment", List.of("independent payment"));
        assertNull(context.getAttribute("__teaql_save_graph_route__"));
    }

    @Test public void independentOverlappingSavesDoNotBorrowAnotherGraphsRoute() throws Exception {
        var entered = new CountDownLatch(2); var proceed = new CountDownLatch(1);
        var orders = pausingProvider(entered, proceed); var payments = pausingProvider(entered, proceed);
        var events = new CopyOnWriteArrayList<SafeAuditEvent>();
        var context = context(orders, payments, events);
        var workers = Executors.newFixedThreadPool(2);
        try {
            var order = workers.submit(() -> existing("CustomerOrder", 100L).auditAs("overlap order").save(context));
            var payment = workers.submit(() -> existing("Payment", 100L).auditAs("overlap payment").save(context));
            assertTrue("both independent provider routes must be live", entered.await(3, TimeUnit.SECONDS));
            assertNull(context.getAttribute("__teaql_save_graph_route__"));
            proceed.countDown(); order.get(10, TimeUnit.SECONDS); payment.get(10, TimeUnit.SECONDS);
        } finally {
            proceed.countDown(); workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(1, orders.requests.size()); assertEquals(1, payments.requests.size());
        assertReasons(events, "CustomerOrder", List.of("overlap order"));
        assertReasons(events, "Payment", List.of("overlap payment"));
    }

    private static Provider pausingProvider(CountDownLatch entered, CountDownLatch proceed) {
        return new Provider() {
            @Override public MutationResult mutate(UserContext context, PersistenceMutation request) {
                entered.countDown();
                try {
                    if (!proceed.await(10, TimeUnit.SECONDS)) throw new AssertionError("route overlap timeout");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt(); throw new AssertionError(error);
                }
                return super.mutate(context, request);
            }
        };
    }

    @Test public void oneMixedProviderGraphIsRejectedBeforeAnyProviderMutationOrAudit() {
        var orders = new Provider(); var payments = new Provider();
        var events = new CopyOnWriteArrayList<SafeAuditEvent>();
        var context = context(orders, payments, events);
        var order = existing("CustomerOrder", 100L); var payment = existing("Payment", 100L);
        order.updateProperty("children", List.of(payment));
        var error = assertThrows(TeaQLRuntimeException.class,
                () -> order.auditAs("must remain atomic").save(context));
        assertTrue(error.getMessage().contains("CROSS-PROVIDER MUTATION"));
        assertTrue(error.getMessage().contains("Payment"));
        assertTrue(orders.requests.isEmpty()); assertTrue(payments.requests.isEmpty());
        assertTrue(events.isEmpty());
        assertEquals("Payment changed", order.getEntityMutationLedger().get(new EntityKey("Payment", 100L), "name"));
        assertNull(context.getAttribute("__teaql_save_graph_route__"));
    }

    @Test public void detachedDeletionCannotBypassThePlanRouteCheck() {
        var orders = new Provider(); var payments = new Provider();
        var events = new CopyOnWriteArrayList<SafeAuditEvent>();
        var context = context(orders, payments, events);
        var order = existing("CustomerOrder", 100L);
        var key = new EntityKey("Payment", 301L);
        order.getEntityMutationLedger().markAsDelete(key);
        order.getEntityMutationLedger().setOriginalVersion(key, 4L);
        assertThrows(TeaQLRuntimeException.class, () -> order.auditAs("detached cross-route deletion").save(context));
        assertTrue(orders.requests.isEmpty()); assertTrue(payments.requests.isEmpty()); assertTrue(events.isEmpty());
        assertTrue(order.getEntityMutationLedger().isMarkedAsDelete(key));
    }

    @Test public void readOnlyForeignProviderRelationDoesNotCountAsAWrite() {
        var orders = new Provider(); var payments = new Provider();
        var events = new CopyOnWriteArrayList<SafeAuditEvent>();
        var context = context(orders, payments, events);
        var order = existing("CustomerOrder", 100L); var payment = persisted("Payment", 301L);
        var paymentLedger = payment.getEntityMutationLedger();
        order.updateProperty("children", List.of(payment));
        order.auditAs("read-only payment reference").save(context);
        assertEquals(1, orders.requests.size()); assertTrue(payments.requests.isEmpty());
        assertSame(paymentLedger, payment.getEntityMutationLedger());
        assertEquals(1, events.size());
    }

    @Test public void failedSaveDoesNotPoisonTheNextIndependentRoute() {
        var orders = new Provider() {
            @Override public MutationResult mutate(UserContext caller, PersistenceMutation request) {
                throw new IllegalStateException("fixture provider failure");
            }
        };
        var payments = new Provider(); var events = new CopyOnWriteArrayList<SafeAuditEvent>();
        var context = context(orders, payments, events);
        assertThrows(IllegalStateException.class,
                () -> existing("CustomerOrder", 100L).auditAs("failed order").save(context));
        existing("Payment", 100L).auditAs("payment after failed order").save(context);
        assertEquals(1, payments.requests.size()); assertEquals(1, events.size());
        assertReasons(events, "Payment", List.of("payment after failed order"));
        assertNull(context.getAttribute("__teaql_save_graph_route__"));
    }
}
