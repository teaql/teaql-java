package io.teaql.examples.tracechain;

import com.teaql.tracechainservice.E;
import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.GeneratedRuntimeModule;
import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.orderitem.OrderItem;
import com.teaql.tracechainservice.payment.Payment;
import com.teaql.tracechainservice.paymentattempt.PaymentAttempt;
import com.teaql.tracechainservice.shipment.Shipment;
import io.teaql.core.*;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** #202: generated public APIs -> real SQLite -> physical SQL and committed safe audit. */
public class GeneratedTraceChainExampleTest {
    static final class Fixture {
        final List<ExecutionMetadata> sql = new CopyOnWriteArrayList<>();
        final List<SafeAuditEvent> audit = new CopyOnWriteArrayList<>();
        final List<EntityPersistenceMutation> commands = new CopyOnWriteArrayList<>();
        final DefaultUserContext context;
        final JdbcSqlExecutor driver;
        volatile boolean failReadback;
        final long base;

        Fixture() throws Exception {
            String configured = System.getProperty("teaql.trace.database", "");
            Path database = configured.isBlank() ? Files.createTempFile("teaql-generated-trace-", ".db")
                    : Path.of(configured).toAbsolutePath();
            var source = new SQLiteDataSource();
            source.setUrl("jdbc:sqlite:" + database);
            driver = new JdbcSqlExecutor(source) {
                @Override public List<java.util.Map<String, Object>> queryForList(String sql, Object[] args) {
                    if (failReadback && sql.startsWith("SELECT * FROM") && sql.contains("customer_order_data"))
                        execute("DROP TABLE customer_order_data");
                    return super.queryForList(sql, args);
                }
            };
            var ids = new IdSpaceIdGenerator(new IdDatabase(driver));
            var metadata = new SimpleEntityMetaFactory();
            var provider = new SqliteDataServiceExecutor("sqlite", driver, source) {
                @Override public MutationResult mutate(UserContext caller, PersistenceMutation mutation) {
                    commands.add((EntityPersistenceMutation) mutation);
                    return super.mutate(caller, mutation);
                }
            };
            var runtime = TeaQLRuntime.builder().metadata(metadata)
                    .dataService("default", provider).dataService("sqlite", provider)
                    .idGenerationService(ids).logSink((caller, entry) -> sql.add(entry)).build()
                    .install(GeneratedRuntimeModule.module()); // Real generated checkers, no bypass.
            EntityMetaFactory.registerGlobal(metadata);
            context = new DefaultUserContext(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audit.add(event));
            context.ensureSchema();
            context.ensureSchema();
            var previous = Q.customerOrders().orderByIdDescending().limit(1)
                    .comment("what: select the previous fixture identity")
                    .purpose("why: replay without deleting the database").executeForOne(context);
            base = previous == null ? 100 : E.customerOrder(previous).getId().eval() + 1000;
            ids.ensureFloor("CustomerOrder", base - 1);
            ids.ensureFloor("Payment", base - 1);
            ids.ensureFloor("OrderItem", base + 100);
            ids.ensureFloor("PaymentAttempt", base + 300);
            ids.ensureFloor("Shipment", base + 400);
            clear();
        }

        void clear() { sql.clear(); audit.clear(); commands.clear(); }

        Graph saveNormativeGraph() {
            var platform = Q.platforms().withIdIs(1L).limit(1)
                    .comment("what: reuse the seeded root")
                    .purpose("why: do not recreate the bootstrap Platform").executeForOne(context);
            assertNotNull(platform);
            CustomerOrder order = Q.customerOrders().comment("what: prepare the fixture order")
                    .purpose("why: verify generated graph persistence").newEntity(context);
            order.updatePlatform(platform);
            order.updateOrderNumber("TRACE-ORDER-" + base);
            order.updateDescription("Draft fixture");
            OrderItem kept = Q.orderItems().comment("what: prepare the available item")
                    .purpose("why: verify generated graph persistence").newEntity(context);
            kept.updateName("Available item");
            OrderItem removed = Q.orderItems().comment("what: prepare the unavailable item")
                    .purpose("why: seed an existing deletion target").newEntity(context);
            removed.updateName("Unavailable item");
            order.addOrderItem(kept).addOrderItem(removed);
            order.auditAs("seed existing order items").save(context);
            assertEquals(Long.valueOf(base), E.customerOrder(order).getId().eval());
            assertEquals(Long.valueOf(base + 101), E.orderItem(kept).getId().eval());
            assertEquals(Long.valueOf(base + 102), E.orderItem(removed).getId().eval());

            Payment payment = Q.payments().comment("what: prepare payment")
                    .purpose("why: persist payment in the order transaction").newEntity(context);
            payment.updateReferenceCode("TRACE-PAYMENT-" + base);
            payment.comment("authorize payment");
            PaymentAttempt attempt = Q.paymentAttempts().comment("what: prepare a payment attempt")
                    .purpose("why: verify inherited grandchild responsibility").newEntity(context);
            attempt.updateReferenceCode("TRACE-ATTEMPT-" + base);
            payment.addPaymentAttempt(attempt);
            Shipment shipment = Q.shipments().comment("what: prepare shipment")
                    .purpose("why: verify isolated sibling responsibility").newEntity(context);
            shipment.updateReferenceCode("TRACE-SHIPMENT-" + base);
            shipment.comment("dispatch shipment");
            order.addPayment(payment).addShipment(shipment);
            order.updateDescription("Submitted fixture");
            kept.updateName("Confirmed item");
            removed.markForDeletion();
            removed.comment("remove unavailable item");
            clear();
            order.auditAs("submit order").save(context);
            return new Graph(order, kept, removed, payment, attempt, shipment);
        }
    }

    record Graph(CustomerOrder order, OrderItem kept, OrderItem removed, Payment payment,
                 PaymentAttempt attempt, Shipment shipment) {}

    @Test public void generatedNormativeGraphHasPerItemPhysicalSqlAndCommittedAudit() throws Exception {
        var fixture = new Fixture();
        Graph graph = fixture.saveNormativeGraph();
        assertEquals("six physical entity commands", 6, fixture.commands.size());
        assertEquals("six committed entity events", 6, fixture.audit.size());
        assertEquals("same numeric ID must not collapse different types", graph.order.getId(), graph.payment.getId());
        assertTrue("mutation planning must not leave an ambient trace", fixture.context.getTraceChain().isEmpty());
        for (var command : fixture.commands) {
            Entity entity = command.getEntity();
            var expected = expected(fixture.base, entity.typeName(), entity.getId());
            assertEquals("provider command " + entity.typeName(), expected, command.getTraceChain());
            var event = fixture.audit.stream().filter(value -> value.entityType().equals(entity.typeName())
                    && value.entityId().equals(entity.getId())).findFirst().orElseThrow();
            assertEquals("safe committed event", expected, event.traceChain());
            String action = command.getAction() == EntityPersistenceMutation.Action.DELETE ? "delete"
                    : entity.typeName().equals("CustomerOrder") || entity.typeName().equals("OrderItem") ? "update" : "insert";
            var writes = fixture.sql.stream().filter(value -> value.getOperation() == DataServiceOperation.MUTATION
                    && value.getTraceChain().get(1).getName().equals(entity.typeName())
                    && value.getMutationLineage().equals(expected)).toList();
            assertFalse("missing actual SQL for " + entity.typeName(), writes.isEmpty());
            for (var entry : writes) {
                assertEquals("success", entry.getExecutionOutcome());
                assertEquals("CustomerOrder", entry.getTraceChain().get(0).getName());
                assertEquals("sqlite", entry.getTraceChain().get(entry.getTraceChain().size() - 2).getName());
                assertEquals(action, entry.getTraceChain().get(entry.getTraceChain().size() - 1).getName());
            }
            assertTrue("readback retains entity responsibility", fixture.sql.stream().anyMatch(value ->
                    value.getOperation() == DataServiceOperation.QUERY && value.getMutationLineage().equals(expected)));
        }
        assertEquals(Long.valueOf(2), E.customerOrder(graph.order).getVersion().eval());
        assertEquals(Long.valueOf(-2), E.orderItem(graph.removed).getVersion().eval());
        assertEquals(Long.valueOf(1), E.payment(graph.payment).getVersion().eval());
        var current = Q.customerOrders().withIdIs(graph.order.getId()).limit(1)
                .selectOrderItemListWith(Q.orderItems().orderByIdAscending().limit(10))
                .selectPaymentListWith(Q.payments().orderByIdAscending().limit(10)
                        .selectPaymentAttemptListWith(Q.paymentAttempts().orderByIdAscending().limit(10)))
                .comment("what: reload the committed order graph")
                .purpose("why: independently verify Q and E after persistence").executeForOne(fixture.context);
        assertEquals("Submitted fixture", E.customerOrder(current).getDescription().eval());
        assertEquals(Integer.valueOf(1), E.customerOrder(current).getOrderItemList().size().eval());
        assertEquals(Integer.valueOf(1), E.customerOrder(current).getPaymentList().size().eval());
        assertNull(Q.orderItems().withIdIs(graph.removed.getId()).limit(1)
                .comment("what: verify the deletion mark")
                .purpose("why: normal queries must hide deleted rows").executeForOne(fixture.context));
        var deleted = Q.orderItems().withIdIs(graph.removed.getId()).deletedRowsOnly().limit(1)
                .comment("what: inspect the stored deletion version")
                .purpose("why: prove version-aware deletion, not physical removal").executeForOne(fixture.context);
        assertEquals(Long.valueOf(-2), E.orderItem(deleted).getVersion().eval());
        System.out.println("PASS Java generated normative Trace Chain graph: six physical writes and committed audits");
    }

    @Test public void generatedThreeLevelQueryProducesAllRelationFramesAndRootIntent() throws Exception {
        var fixture = new Fixture();
        Graph graph = fixture.saveNormativeGraph();
        fixture.clear();
        var row = Q.paymentAttempts().withIdIs(graph.attempt.getId()).limit(1)
                .selectPaymentWith(Q.payments().limit(1)
                        .selectCustomerOrderWith(Q.customerOrders().limit(1)
                                .selectPlatformWith(Q.platforms().limit(1))))
                .comment("what: load three levels of payment context")
                .purpose("why: verify generated SQL trace propagation").executeForOne(fixture.context);
        var payment = E.paymentAttempt(row).getPayment().eval();
        var order = E.payment(payment).getCustomerOrder().eval();
        var platform = E.customerOrder(order).getPlatform().eval();
        assertEquals("Trace Chain Verification", E.platform(platform).getName().eval());
        var statements = fixture.sql.stream().filter(value -> value.getOperation() == DataServiceOperation.QUERY).toList();
        assertEquals("one root query and three explicit relation queries", 4, statements.size());
        List<String> names = List.of("payment", "customerOrder", "platform");
        for (int depth = 0; depth < statements.size(); depth++) {
            var entry = statements.get(depth);
            assertEquals("what: load three levels of payment context", entry.getComment());
            assertEquals("why: verify generated SQL trace propagation", entry.getPurpose());
            assertEquals("PaymentAttempt", entry.getTraceChain().get(0).getName());
            assertEquals("PaymentAttempt", entry.getTraceChain().get(1).getName());
            var relations = entry.getTraceChain().stream().filter(node -> node.getKind() == TraceKind.RELATION)
                    .map(TraceNode::getName).toList();
            assertEquals("actual relation depth " + depth, names.subList(0, depth), relations);
            var details = entry.getTraceChain().stream().filter(node -> node.getKind() == TraceKind.RELATION)
                    .map(TraceNode::getComment).toList();
            assertEquals(List.of("PaymentAttempt.payment", "Payment.customerOrder", "CustomerOrder.platform")
                    .subList(0, depth), details);
        }
        System.out.println("PASS Java generated three-level SQL Trace Path and inherited request intent");
    }

    @Test public void generatedCheckerRejectsInvalidBusinessStateBeforeProvider() throws Exception {
        var fixture = new Fixture();
        var platform = Q.platforms().withIdIs(1L).limit(1).comment("what: reuse root")
                .purpose("why: verify checker enforcement").executeForOne(fixture.context);
        var invalid = Q.customerOrders().comment("what: prepare an incomplete order")
                .purpose("why: test generated required-field rules").newEntity(fixture.context);
        invalid.updatePlatform(platform);
        invalid.updateDescription("Incomplete fixture");
        fixture.clear();
        var failure = assertThrows(io.teaql.core.checker.CheckException.class,
                () -> invalid.auditAs("reject incomplete order").save(fixture.context));
        assertTrue(failure.getViolates().toString(), failure.getViolates().stream()
                .anyMatch(value -> value.getLocation().modelPath().endsWith("order_number")));
        assertTrue(fixture.commands.isEmpty());
        assertTrue(fixture.sql.isEmpty());
        assertTrue(fixture.audit.isEmpty());
        System.out.println("PASS Java generated Checker rejection before provider access");
    }

    @Test public void generatedProviderFailureKeepsAttemptedLineageWithoutCommittedAudit() throws Exception {
        var fixture = new Fixture();
        Graph prior = fixture.saveNormativeGraph();
        fixture.driver.execute("CREATE UNIQUE INDEX IF NOT EXISTS trace_payment_reference_unique ON payment_data(reference_code)");
        var failed = Q.customerOrders().comment("what: prepare a failing transaction")
                .purpose("why: test generated graph rollback").newEntity(fixture.context);
        var platform = Q.platforms().withIdIs(1L).limit(1).comment("what: reuse root")
                .purpose("why: prepare the authorized fixture").executeForOne(fixture.context);
        failed.updatePlatform(platform);
        failed.updateOrderNumber("TRACE-FAIL-" + fixture.base);
        failed.updateDescription("Will roll back");
        var duplicate = Q.payments().comment("what: prepare duplicate payment")
                .purpose("why: provoke an actual SQLite uniqueness error").newEntity(fixture.context);
        duplicate.updateReferenceCode(E.payment(prior.payment).getReferenceCode().eval());
        duplicate.comment("reject duplicate transfer");
        failed.addPayment(duplicate);
        fixture.clear();
        assertThrows(RuntimeException.class, () -> failed.auditAs("attempt atomic submission").save(fixture.context));
        assertTrue("rollback is not a committed audit", fixture.audit.isEmpty());
        var error = fixture.sql.stream().filter(value -> "failure".equals(value.getBatchOutcome())).findFirst().orElseThrow();
        assertEquals(List.of("attempt atomic submission", "reject duplicate transfer"),
                error.getMutationLineage().stream().map(TraceNode::getComment).toList());
        assertEquals(duplicate.getId(), error.getMutationLineage().get(1).getEntityId());
        assertTrue(fixture.sql.stream().anyMatch(value -> value.getOperation() == DataServiceOperation.MUTATION
                && "success".equals(value.getExecutionOutcome())));
        assertNull(Q.customerOrders().withIdIs(failed.getId()).limit(1)
                .comment("what: query the failed graph identity")
                .purpose("why: prove the earlier root insert rolled back").executeForOne(fixture.context));
        System.out.println("PASS Java generated provider failure: attempted lineage, rollback, no committed audit");
    }

    @Test public void generatedReadbackFailurePreservesWriteTraceAndRetries() throws Exception {
        var fixture = new Fixture();
        Graph graph = fixture.saveNormativeGraph();
        graph.order.updateDescription("Readback retry fixture");
        fixture.clear();
        fixture.failReadback = true;
        assertThrows(RuntimeException.class, () -> graph.order.auditAs("attempt readback").save(fixture.context));
        assertTrue(fixture.audit.isEmpty());
        var write = fixture.sql.stream().filter(value -> value.getOperation() == DataServiceOperation.MUTATION).findFirst().orElseThrow();
        var readback = fixture.sql.stream().filter(value -> value.getOperation() == DataServiceOperation.QUERY
                && "failure".equals(value.getExecutionOutcome())).findFirst().orElseThrow();
        assertEquals("success", write.getExecutionOutcome());
        assertEquals(write.getMutationLineage(), readback.getMutationLineage());
        assertEquals("select", readback.getStatementOperation());
        assertEquals(Long.valueOf(2), E.customerOrder(graph.order).getVersion().eval());
        fixture.failReadback = false;
        fixture.clear();
        graph.order.auditAs("retry readback").save(fixture.context);
        assertEquals(1, fixture.audit.size());
        assertEquals(List.of("retry readback"), fixture.audit.get(0).traceChain().stream().map(TraceNode::getComment).toList());
        assertEquals(Long.valueOf(3), E.customerOrder(graph.order).getVersion().eval());
        System.out.println("PASS Java generated readback failure: separate outcomes and successful retry");
    }

    static List<TraceNode> expected(long base, String type, long id) {
        var root = new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", base, "submit order");
        if (type.equals("Payment") || type.equals("PaymentAttempt"))
            return List.of(root, new TraceNode(TraceKind.AUDIT_REASON, "Payment", base, "authorize payment"));
        if (type.equals("Shipment"))
            return List.of(root, new TraceNode(TraceKind.AUDIT_REASON, "Shipment", base + 401, "dispatch shipment"));
        if (type.equals("OrderItem") && id == base + 102)
            return List.of(root, new TraceNode(TraceKind.AUDIT_REASON, "OrderItem", id, "remove unavailable item"));
        return List.of(root);
    }
}
