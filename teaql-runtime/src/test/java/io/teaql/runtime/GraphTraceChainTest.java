package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

/** #202: real runtime planning and safe audit, not a SQL integration substitute. */
public class GraphTraceChainTest {
    static final class GraphEntity extends BaseEntity {
        private final String type;
        private final Map<String, Object> values = new HashMap<>();
        GraphEntity(String type) { this.type = type; }
        @Override public String typeName() { return type; }
        @Override public Object __internalGet(String field) {
            return field.equals("name") || field.equals("children") ? values.get(field) : super.__internalGet(field);
        }
        @Override public void __internalSet(String field, Object value) {
            if (field.equals("name") || field.equals("children")) values.put(field, value);
            else super.__internalSet(field, value);
        }
    }

    static class Provider implements MutationExecutor {
        final List<EntityPersistenceMutation> requests = new CopyOnWriteArrayList<>();
        @Override public MutationResult mutate(UserContext context, PersistenceMutation request) {
            EntityPersistenceMutation item = (EntityPersistenceMutation) request;
            requests.add(item);
            return new DefaultMutationResult(item.getEntity());
        }
        @Override public String name() { return "fixture"; }
        @Override public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
    }

    static class BatchProvider extends Provider implements BatchMutationExecutor {
        int batchCalls;
        boolean reverseResults;
        @Override public List<MutationResult> mutateBatch(UserContext context, MutationBatchRequest request) {
            batchCalls++;
            assertEquals("runtime owns the batch root intent", "save grouped graph", request.comment());
            var results = new ArrayList<MutationResult>();
            for (var item : request.items()) results.add(super.mutate(context, item));
            if (reverseResults) Collections.reverse(results);
            return results;
        }
    }

    @Test public void sameTypeGraphUsesOptionalBatchCapabilityAndKeepsItemCommands() {
        var provider = new BatchProvider();
        var events = new ArrayList<SafeAuditEvent>();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var context = new DefaultUserContext(runtime);
        context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> events.add(event));
        var root = existing("CustomerOrder", 100L);
        var first = new GraphEntity("OrderItem");
        first.__internalInitializeNewEntityId(201L);
        first.updateProperty("name", "alpha item");
        first.setComment("alpha branch");
        var second = new GraphEntity("OrderItem");
        second.__internalInitializeNewEntityId(202L);
        second.updateProperty("name", "beta item");
        second.setComment("beta branch");
        root.updateProperty("children", List.of(second, first));
        root.auditAs("save grouped graph").save(context);
        assertEquals(1, provider.batchCalls);
        assertReasons(events, "OrderItem", 201L, List.of("save grouped graph", "alpha branch"));
        assertReasons(events, "OrderItem", 202L, List.of("save grouped graph", "beta branch"));
        assertEquals("keys are planned in deterministic ID order", Long.valueOf(201), provider.requests.get(0).getEntity().getId());
    }

    @Test public void reversedProviderBatchResultsCannotBeAppliedToDifferentEntities() {
        var provider = new BatchProvider();
        provider.reverseResults = true;
        var events = new ArrayList<SafeAuditEvent>();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var context = new DefaultUserContext(runtime);
        context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> events.add(event));
        var root = existing("CustomerOrder", 100L);
        var first = new GraphEntity("OrderItem");
        first.__internalInitializeNewEntityId(201L);
        first.updateProperty("name", "alpha item");
        var second = new GraphEntity("OrderItem");
        second.__internalInitializeNewEntityId(202L);
        second.updateProperty("name", "beta item");
        root.updateProperty("children", List.of(first, second));
        var error = assertThrows(TeaQLRuntimeException.class, () -> root.auditAs("save grouped graph").save(context));
        assertEquals("Batch mutation result identity does not match its ordered command", error.getMessage());
        assertTrue(events.isEmpty());
        assertEquals("alpha item", first.getProperty("name"));
        assertEquals("beta item", second.getProperty("name"));
    }

    static SimpleEntityMetaFactory metadata() {
        var metadata = new SimpleEntityMetaFactory();
        for (String type : List.of("CustomerOrder", "OrderItem", "Payment", "PaymentAttempt", "Shipment")) {
            var descriptor = new EntityDescriptor();
            descriptor.setType(type);
            descriptor.setDataService("fixture");
            descriptor.setTargetType(GraphEntity.class);
            descriptor.setEntitySupplier(() -> new GraphEntity(type));
            var children = new Relation();
            children.setName("children");
            children.setOwner(descriptor);
            descriptor.setProperties(List.of(children));
            metadata.register(descriptor);
        }
        return metadata;
    }

    static GraphEntity persisted(String type, long id) {
        var entity = new GraphEntity(type);
        entity.__internalSet("id", id);
        entity.__internalSet("version", 1L);
        entity.set$status(EntityStatus.PERSISTED);
        return entity;
    }

    static GraphEntity existing(String type, long id) {
        var entity = persisted(type, id);
        entity.updateProperty("name", type + " changed");
        return entity;
    }

    @Test public void normativeGraphKeepsOwnAndInheritedReasonsAtCommittedAudit() {
        var provider = new Provider();
        var events = new ArrayList<SafeAuditEvent>();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var context = new DefaultUserContext(runtime);
        context.putAttribute(AppAuditEventSink.class.getName(),
                (AppAuditEventSink) (caller, event) -> events.add(event));
        var root = existing("CustomerOrder", 100L);
        root.setComment("submit order");
        var item = existing("OrderItem", 201L);
        var removed = persisted("OrderItem", 202L);
        removed.setComment("remove unavailable item");
        removed.markForDeletion();
        var payment = existing("Payment", 100L); // Same numeric ID, different type.
        payment.setComment("authorize payment");
        var attempt = existing("PaymentAttempt", 401L);
        payment.updateProperty("children", List.of(attempt));
        var shipment = existing("Shipment", 501L);
        shipment.setComment("dispatch shipment");
        root.updateProperty("children", List.of(item, payment, shipment, removed));

        root.auditAs("submit order").save(context);

        assertEquals(6, provider.requests.size());
        assertEquals(6, events.size());
        assertReasons(events, "CustomerOrder", List.of("submit order"));
        assertReasons(events, "Payment", List.of("submit order", "authorize payment"));
        assertReasons(events, "PaymentAttempt", List.of("submit order", "authorize payment"));
        assertReasons(events, "Shipment", List.of("submit order", "dispatch shipment"));
        assertReasons(events, "OrderItem", 201L, List.of("submit order"));
        assertReasons(events, "OrderItem", 202L, List.of("submit order", "remove unavailable item"));
        for (var request : provider.requests) {
            var event = events.stream().filter(value -> value.entityType().equals(request.getEntity().typeName())
                    && Objects.equals(value.entityId(), request.getEntity().getId())).findFirst().orElseThrow();
            assertEquals("command and committed audit must retain the same typed identity", request.getTraceChain(), event.traceChain());
            assertEquals(Long.valueOf(100), request.getTraceChain().get(0).getEntityId());
            assertThrows(UnsupportedOperationException.class, () -> request.getTraceChain().clear());
        }
        assertTrue("save must not mutate ambient Context trace", context.getTraceChain().isEmpty());
    }

    @Test public void completeLedgerChainReplacesFallbackOnlyForItsTypedKey() {
        var provider = new Provider();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var context = new DefaultUserContext(runtime);
        var root = existing("CustomerOrder", 100L);
        root.setComment("root fallback");
        var payment = existing("Payment", 100L);
        payment.setComment("local fallback must not be appended");
        var complete = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", 100L, "delegated root"),
                new TraceNode(TraceKind.AUDIT_REASON, "Payment", 100L, "delegated payment"));
        payment.setTraceChain(complete);
        root.updateProperty("children", List.of(payment));
        root.auditAs("root fallback").save(context);
        var paymentRequest = provider.requests.stream().filter(item -> item.getEntity().typeName().equals("Payment")).findFirst().orElseThrow();
        assertEquals(complete, paymentRequest.getTraceChain());
        var rootRequest = provider.requests.stream().filter(item -> item.getEntity().typeName().equals("CustomerOrder")).findFirst().orElseThrow();
        assertEquals(List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", 100L, "root fallback")), rootRequest.getTraceChain());
        assertNull("completed ledger releases its override",
                root.getEntityMutationLedger().getTraceChain(new EntityKey("Payment", 100L)));
    }

    @Test public void newDescendantReasonCarriesAllocatedIdAndBlankReasonInherits() {
        var provider = new Provider();
        var ids = new AtomicLong(700);
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider)
                .idGenerationService((caller, entity) -> ids.getAndIncrement()).build();
        var context = new DefaultUserContext(runtime);
        var root = existing("CustomerOrder", 100L);
        var payment = new GraphEntity("Payment");
        payment.updateProperty("name", "new payment");
        payment.setComment("new local payment reason");
        var attempt = new GraphEntity("PaymentAttempt");
        attempt.updateProperty("name", "new attempt");
        attempt.setComment("\u2003 \t");
        payment.updateProperty("children", List.of(attempt));
        root.updateProperty("children", List.of(payment));
        root.auditAs("allocated root reason").save(context);
        var paymentRequest = provider.requests.stream().filter(item -> item.getEntity().typeName().equals("Payment")).findFirst().orElseThrow();
        assertEquals(Long.valueOf(700), paymentRequest.getTraceChain().get(1).getEntityId());
        var attemptRequest = provider.requests.stream().filter(item -> item.getEntity().typeName().equals("PaymentAttempt")).findFirst().orElseThrow();
        assertEquals(paymentRequest.getTraceChain(), attemptRequest.getTraceChain());
    }

    @Test public void concurrentGraphsDoNotShareContextReasons() throws Exception {
        var entered = new CountDownLatch(2);
        var proceed = new CountDownLatch(1);
        var provider = new Provider() {
            @Override public MutationResult mutate(UserContext context, PersistenceMutation request) {
                entered.countDown();
                try {
                    if (!proceed.await(10, TimeUnit.SECONDS)) throw new AssertionError("provider overlap timeout");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt(); throw new AssertionError(interrupted);
                }
                return super.mutate(context, request);
            }
        };
        var events = new CopyOnWriteArrayList<SafeAuditEvent>();
        var runtime = TeaQLRuntime.builder().metadata(metadata()).dataService("fixture", provider).build();
        var context = new DefaultUserContext(runtime);
        context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> events.add(event));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> existing("CustomerOrder", 100L).auditAs("graph A").save(context));
            var b = executor.submit(() -> existing("CustomerOrder", 200L).auditAs("graph B").save(context));
            assertTrue("test must prove live overlap", entered.await(10, TimeUnit.SECONDS));
            assertTrue("no request may push a Context trace while paused inside provider", context.getTraceChain().isEmpty());
            proceed.countDown();
            a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
        } finally {
            proceed.countDown(); executor.shutdownNow();
        }
        assertEquals(2, events.size());
        assertReasons(events, "CustomerOrder", 100L, List.of("graph A"));
        assertReasons(events, "CustomerOrder", 200L, List.of("graph B"));
    }

    static void assertReasons(List<SafeAuditEvent> events, String type, List<String> reasons) {
        var event = events.stream().filter(value -> value.entityType().equals(type)).findFirst().orElseThrow();
        assertReasons(event, reasons);
    }

    static void assertReasons(List<SafeAuditEvent> events, String type, long id, List<String> reasons) {
        var event = events.stream().filter(value -> value.entityType().equals(type)
                && Objects.equals(value.entityId(), id)).findFirst().orElseThrow();
        assertReasons(event, reasons);
    }

    static void assertReasons(SafeAuditEvent event, List<String> reasons) {
        assertEquals("committed lineage of " + event.entityType() + "#" + event.entityId(), reasons,
                event.traceChain().stream().filter(node -> node.getKind() == TraceKind.AUDIT_REASON)
                        .map(TraceNode::getComment).toList());
    }
}
