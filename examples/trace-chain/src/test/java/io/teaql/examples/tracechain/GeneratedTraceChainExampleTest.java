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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** #202: generated public APIs -> real SQLite -> physical SQL and committed safe audit. */
public class GeneratedTraceChainExampleTest {
    private static void verifyReturnedStatements(MutationResult result, EntityPersistenceMutation request) {
        var statements = result.statements();
        assertEquals("actual write and authoritative readback retained independently of sinks", 2, statements.size());
        var write = statements.get(0);
        var read = statements.get(1);
        assertEquals(DataServiceOperation.MUTATION, write.getOperation());
        assertEquals(DataServiceOperation.QUERY, read.getOperation());
        assertEquals(request.getTraceChain(), write.getMutationLineage());
        assertEquals(write.getMutationLineage(), read.getMutationLineage());
        assertEquals(TraceKind.REQUEST, read.getTraceChain().get(1).getKind());
        assertEquals(request.getTraceChain().get(0).getName(), read.getTraceChain().get(0).getName());
        assertEquals("select", read.getStatementOperation());
        assertEquals(Long.valueOf(1), write.getAffectedRows());
        assertEquals(Integer.valueOf(1), read.getResultCount());
        assertEquals(request.intent().readbackIntent().purpose(), read.getPurpose());
    }

    static final class Fixture {
        final List<ExecutionMetadata> sql = new CopyOnWriteArrayList<>();
        final List<SafeAuditEvent> audit = new CopyOnWriteArrayList<>();
        final List<EntityPersistenceMutation> commands = new CopyOnWriteArrayList<>();
        final List<Integer> itemInsertBatchSizes = new CopyOnWriteArrayList<>();
        final List<Integer> itemUpdateBatchSizes = new CopyOnWriteArrayList<>();
        final List<Integer> itemDeleteBatchSizes = new CopyOnWriteArrayList<>();
        final List<Integer> itemRecoverBatchSizes = new CopyOnWriteArrayList<>();
        final DefaultUserContext context;
        final JdbcSqlExecutor driver;
        volatile boolean failReadback;
        volatile boolean serializeTransactions;
        volatile Consumer<UserContext> checkerBegin;
        volatile Consumer<UserContext> checkerFinish;
        volatile java.util.function.BiConsumer<UserContext, QueryRequest> queryBegin;
        volatile Consumer<UserContext> streamOpen;
        final long base;

        Fixture() throws Exception {
            String configured = System.getProperty("teaql.trace.database", "");
            Path database = configured.isBlank() ? Files.createTempFile("teaql-generated-trace-", ".db")
                    : Path.of(configured).toAbsolutePath();
            var source = new SQLiteDataSource();
            source.setUrl("jdbc:sqlite:" + database);
            driver = new JdbcSqlExecutor(source) {
                @Override public void executeInTransaction(Runnable action) {
                    // SQLite has one writer. Only physical transactions serialize;
                    // the tests still overlap the real generated Checker invocations.
                    if (serializeTransactions) {
                        synchronized (this) { super.executeInTransaction(action); }
                    } else super.executeInTransaction(action);
                }
                @Override public int[] batchUpdate(String text, List<Object[]> rows) {
                    if (text.startsWith("INSERT INTO order_item_data")) itemInsertBatchSizes.add(rows.size());
                    if (text.startsWith("UPDATE order_item_data") && !rows.isEmpty()) {
                        assertTrue("committed audit must wait for all member writes", audit.isEmpty());
                        Object[] first = rows.get(0);
                        if (first.length != 3) itemUpdateBatchSizes.add(rows.size());
                        else if (((Number) first[0]).longValue() < 0) itemDeleteBatchSizes.add(rows.size());
                        else itemRecoverBatchSizes.add(rows.size());
                    }
                    return super.batchUpdate(text, rows);
                }
                @Override public List<java.util.Map<String, Object>> queryForList(String sql, Object[] args) {
                    if (failReadback && sql.startsWith("SELECT * FROM") && sql.contains("customer_order_data"))
                        execute("DROP TABLE customer_order_data");
                    return super.queryForList(sql, args);
                }
                @Override public java.util.stream.Stream<java.util.Map<String, Object>> queryForStream(String sql, Object[] args) {
                    if (streamOpen != null) streamOpen.accept(Fixture.this.context);
                    return super.queryForStream(sql, args);
                }
            };
            var ids = new IdSpaceIdGenerator(new IdDatabase(driver));
            var metadata = new SimpleEntityMetaFactory();
            var provider = new SqliteDataServiceExecutor("sqlite", driver, source) {
                @Override public QueryResult query(UserContext caller, QueryRequest request) {
                    if (queryBegin != null) queryBegin.accept(caller, request);
                    return super.query(caller, request);
                }
                @Override public MutationResult mutate(UserContext caller, PersistenceMutation mutation) {
                    commands.add((EntityPersistenceMutation) mutation);
                    var result = super.mutate(caller, mutation);
                    verifyReturnedStatements(result, (EntityPersistenceMutation) mutation);
                    return result;
                }
                @Override public List<MutationResult> mutateBatch(UserContext caller, MutationBatchRequest request) {
                    request.items().forEach(item -> commands.add((EntityPersistenceMutation) item));
                    var results = super.mutateBatch(caller, request);
                    for (int i = 0; i < results.size(); i++)
                        verifyReturnedStatements(results.get(i), (EntityPersistenceMutation) request.items().get(i));
                    return results;
                }
            };
            var runtime = TeaQLRuntime.builder().metadata(metadata)
                    .dataService("default", provider).dataService("sqlite", provider)
                    .idGenerationService(ids).logSink((caller, entry) -> sql.add(entry)).build()
                    .install(GeneratedRuntimeModule.module()); // Real generated checkers, no bypass.
            EntityMetaFactory.registerGlobal(metadata);
            context = new DefaultUserContext(runtime) {
                @Override public void beginFixEvidence() {
                    super.beginFixEvidence();
                    if (checkerBegin != null) checkerBegin.accept(this);
                }
                @Override public void finishFixEvidence() {
                    if (checkerFinish != null) checkerFinish.accept(this);
                    super.finishFixEvidence();
                }
            };
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

        void clear() {
            sql.clear(); audit.clear(); commands.clear(); itemInsertBatchSizes.clear();
            itemUpdateBatchSizes.clear(); itemDeleteBatchSizes.clear(); itemRecoverBatchSizes.clear();
        }

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

    private static void await(CountDownLatch latch) {
        try { assertTrue("generated execution must reach checkpoint", latch.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static Throwable saveFailure(CustomerOrder order, UserContext context, String reason) {
        try { order.auditAs(reason).save(context); return null; }
        catch (Throwable failure) { return failure; }
    }

    @Test public void overlappingGeneratedCheckersKeepValidAndInvalidRequestsIndependent() throws Exception {
        var fixture = new Fixture();
        var platform = Q.platforms().withIdIs(1L).limit(1).comment("what: reuse root")
                .purpose("why: prepare independent generated graph saves").executeForOne(fixture.context);
        var valid = Q.customerOrders().comment("what: prepare valid order")
                .purpose("why: exercise the actual generated Checker").newEntity(fixture.context);
        valid.updatePlatform(platform);
        valid.updateOrderNumber("TRACE-CONCURRENT-" + fixture.base);
        valid.updateDescription("Valid overlapping request");
        var invalid = Q.customerOrders().comment("what: prepare incomplete order")
                .purpose("why: require an independent Checker rejection").newEntity(fixture.context);
        invalid.updatePlatform(platform);
        invalid.updateDescription("Invalid overlapping request");
        assertNotSame(valid.getEntityMutationLedger(), invalid.getEntityMutationLedger());
        var validThread = new AtomicReference<Thread>();
        var validEntered = new CountDownLatch(1);
        var invalidFinishing = new CountDownLatch(1);
        var validFinished = new CountDownLatch(1);
        fixture.checkerBegin = caller -> {
            assertSame(fixture.context, caller);
            if (Thread.currentThread() == validThread.get()) {
                validEntered.countDown();
                await(invalidFinishing);
            } else await(validEntered);
        };
        fixture.checkerFinish = caller -> {
            if (Thread.currentThread() != validThread.get()) {
                invalidFinishing.countDown();
                await(validFinished);
            }
        };
        fixture.clear();
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> {
                validThread.set(Thread.currentThread());
                try { return saveFailure(valid, fixture.context, "accept overlapping valid order"); }
                finally { validFinished.countDown(); }
            });
            // The first run must enter its initialized Checker before the second
            // can initialize its own state. No timing sleeps or fake checkers.
            await(validEntered);
            var second = workers.submit(() -> saveFailure(invalid, fixture.context, "reject overlapping incomplete order"));
            Throwable accepted = first.get(20, TimeUnit.SECONDS);
            Throwable rejected = second.get(20, TimeUnit.SECONDS);
            assertNull("valid request must not inherit another check's violations: " + accepted, accepted);
            assertTrue(rejected instanceof io.teaql.core.checker.CheckException);
            assertTrue(((io.teaql.core.checker.CheckException) rejected).getViolates().stream()
                    .anyMatch(value -> value.getLocation().modelPath().endsWith("order_number")));
            assertEquals(1, fixture.commands.size());
            assertEquals(1, fixture.audit.size());
            var lineage = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", valid.getId(), "accept overlapping valid order"));
            assertEquals(lineage, fixture.commands.get(0).getTraceChain());
            assertEquals(lineage, fixture.audit.get(0).traceChain());
            assertTrue(fixture.sql.stream().allMatch(entry -> entry.getMutationLineage().equals(lineage)));
            assertNull("Checker rejects before allocation", invalid.getId());
        } finally {
            validEntered.countDown(); invalidFinishing.countDown(); validFinished.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
            fixture.checkerBegin = null; fixture.checkerFinish = null;
        }
        var loaded = Q.customerOrders().withIdIs(valid.getId()).limit(1)
                .comment("what: reload accepted concurrent request")
                .purpose("why: prove generated Q/E observe its committed row").executeForOne(fixture.context);
        assertEquals("TRACE-CONCURRENT-" + fixture.base, E.customerOrder(loaded).getOrderNumber().eval());
        assertTrue(fixture.context.getTraceChain().isEmpty());
        System.out.println("PASS Java generated overlapping Checker: valid commits, invalid rejected before provider");
    }

    @Test public void independentGeneratedGraphsShareOneContextWithoutTraceOrAuditCrossTalk() throws Exception {
        var fixture = new Fixture();
        var platform = Q.platforms().withIdIs(1L).limit(1).comment("what: reuse root")
                .purpose("why: prepare two independent graphs").executeForOne(fixture.context);
        var platformLedger = platform.getEntityMutationLedger();
        var orders = new java.util.ArrayList<CustomerOrder>();
        for (String suffix : List.of("alpha", "beta")) {
            var order = Q.customerOrders().comment("what: prepare " + suffix)
                    .purpose("why: exercise concurrent graph ownership").newEntity(fixture.context);
            order.updatePlatform(platform);
            order.updateOrderNumber("TRACE-PARALLEL-" + fixture.base + "-" + suffix);
            order.updateDescription("Parallel graph " + suffix);
            var item = Q.orderItems().comment("what: prepare " + suffix + " item")
                    .purpose("why: exercise local child responsibility").newEntity(fixture.context);
            item.updateName("Parallel entry " + suffix);
            item.comment("append " + suffix);
            order.addOrderItem(item);
            orders.add(order);
        }
        assertNotSame(orders.get(0).getEntityMutationLedger(), orders.get(1).getEntityMutationLedger());
        var checkpoint = new CyclicBarrier(2);
        fixture.checkerBegin = caller -> {
            assertSame(fixture.context, caller);
            try { checkpoint.await(10, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
            catch (Exception failure) { throw new AssertionError(failure); }
        };
        fixture.serializeTransactions = true;
        fixture.clear();
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> saveFailure(orders.get(0), fixture.context, "save alpha graph"));
            var second = workers.submit(() -> saveFailure(orders.get(1), fixture.context, "save beta graph"));
            Throwable alpha = first.get(20, TimeUnit.SECONDS);
            Throwable beta = second.get(20, TimeUnit.SECONDS);
            if (alpha != null) throw new AssertionError("alpha graph failed", alpha);
            if (beta != null) throw new AssertionError("beta graph failed", beta);
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
            fixture.checkerBegin = null;
        }
        assertEquals(4, fixture.commands.size());
        assertEquals(4, fixture.audit.size());
        assertSame("shared read-only relation retains its independent ledger", platformLedger, platform.getEntityMutationLedger());
        for (int index = 0; index < orders.size(); index++) {
            var order = orders.get(index);
            String suffix = index == 0 ? "alpha" : "beta";
            var graphCommands = fixture.commands.stream().filter(value ->
                    value.getTraceChain().get(0).getEntityId().equals(order.getId())).toList();
            assertEquals(2, graphCommands.size());
            for (var command : graphCommands) {
                var expected = command.getEntity().typeName().equals("CustomerOrder")
                        ? List.of("save " + suffix + " graph")
                        : List.of("save " + suffix + " graph", "append " + suffix);
                assertEquals(expected, command.getTraceChain().stream().map(TraceNode::getComment).toList());
                var event = fixture.audit.stream().filter(value -> value.entityType().equals(command.getEntity().typeName())
                        && value.entityId().equals(command.getEntity().getId())).findFirst().orElseThrow();
                assertEquals(command.getTraceChain(), event.traceChain());
                assertTrue(fixture.sql.stream().anyMatch(value -> value.getOperation() == DataServiceOperation.MUTATION
                        && value.getMutationLineage().equals(command.getTraceChain())));
                assertTrue(fixture.sql.stream().anyMatch(value -> value.getOperation() == DataServiceOperation.QUERY
                        && value.getMutationLineage().equals(command.getTraceChain())));
            }
            var loaded = Q.customerOrders().withIdIs(order.getId()).limit(1)
                    .selectOrderItemListWith(Q.orderItems().limit(10))
                    .comment("what: reload " + suffix + " graph")
                    .purpose("why: verify independent commits through generated Q/E").executeForOne(fixture.context);
            assertEquals(Integer.valueOf(1), E.customerOrder(loaded).getOrderItemList().size().eval());
            assertEquals("TRACE-PARALLEL-" + fixture.base + "-" + suffix, E.customerOrder(loaded).getOrderNumber().eval());
        }
        assertTrue(fixture.context.getTraceChain().isEmpty());
        System.out.println("PASS Java generated concurrent graphs: same Context, independent ledgers and per-item SQL/audit lineage");
    }

    @Test public void generatedSameTypePreparedBatchKeepsItemReasonsAndCompleteLedgerReplacement() throws Exception {
        var fixture = new Fixture();
        var platform = Q.platforms().withIdIs(1L).limit(1)
                .comment("what: reuse the bootstrap root for batch acceptance")
                .purpose("why: keep fixture writes inside generated APIs").executeForOne(fixture.context);
        var order = Q.customerOrders().comment("what: initialize the batch order")
                .purpose("why: verify generated prepared graph persistence").newEntity(fixture.context);
        order.updatePlatform(platform);
        order.updateOrderNumber("TRACE-BATCH-" + fixture.base);
        order.updateDescription("Prepared batch fixture");
        var first = Q.orderItems().comment("what: initialize entry alpha")
                .purpose("why: verify per-item responsibility").newEntity(fixture.context);
        first.updateName("Batch entry alpha");
        first.comment("append alpha");
        var second = Q.orderItems().comment("what: initialize entry beta")
                .purpose("why: verify independent item responsibility").newEntity(fixture.context);
        second.updateName("Batch entry beta");
        second.comment("append beta");
        order.addOrderItem(first).addOrderItem(second);
        fixture.clear();
        order.auditAs("compose generated batch").save(fixture.context);
        long orderId = E.customerOrder(order).getId().eval();
        long firstId = E.orderItem(first).getId().eval();
        long secondId = E.orderItem(second).getId().eval();

        assertEquals("one actual two-row prepared JDBC insert", List.of(2), fixture.itemInsertBatchSizes);
        assertEquals(3, fixture.commands.size());
        assertEquals(3, fixture.audit.size());
        var expectedFirst = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", orderId, "compose generated batch"),
                new TraceNode(TraceKind.AUDIT_REASON, "OrderItem", firstId, "append alpha"));
        var expectedSecond = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", orderId, "compose generated batch"),
                new TraceNode(TraceKind.AUDIT_REASON, "OrderItem", secondId, "append beta"));
        for (long id : List.of(firstId, secondId)) {
            var expected = id == firstId ? expectedFirst : expectedSecond;
            var command = fixture.commands.stream().filter(value -> value.getEntity().typeName().equals("OrderItem")
                    && value.getEntity().getId().equals(id)).findFirst().orElseThrow();
            assertEquals(expected, command.getTraceChain());
            var event = fixture.audit.stream().filter(value -> value.entityType().equals("OrderItem")
                    && value.entityId().equals(id)).findFirst().orElseThrow();
            assertEquals(expected, event.traceChain());
            var writes = fixture.sql.stream().filter(value -> value.getOperation() == DataServiceOperation.MUTATION
                    && value.getMutationLineage().equals(expected)).toList();
            assertEquals(1, writes.size());
            assertEquals("success", writes.get(0).getExecutionOutcome());
            assertEquals(Long.valueOf(1), writes.get(0).getAffectedRows());
            assertTrue(fixture.sql.stream().anyMatch(value -> value.getOperation() == DataServiceOperation.QUERY
                    && value.getMutationLineage().equals(expected)));
        }
        var loaded = Q.customerOrders().withIdIs(orderId).limit(1)
                .selectOrderItemListWith(Q.orderItems().orderByIdAscending().limit(10))
                .comment("what: reload the batch through generated relations")
                .purpose("why: independently validate FK association and scalar readback").executeForOne(fixture.context);
        assertEquals(Integer.valueOf(2), E.customerOrder(loaded).getOrderItemList().size().eval());
        assertEquals("Batch entry alpha", E.orderItem(first).getName().eval());
        assertEquals("Batch entry beta", E.orderItem(second).getName().eval());
        assertEquals(Long.valueOf(1), E.orderItem(first).getVersion().eval());
        assertEquals(Long.valueOf(1), E.orderItem(second).getVersion().eval());

        // Java assigns IDs at graph-save time. After the generated insert/readback,
        // prove complete-ledger replacement on an identified existing child.
        order.updateDescription("Ledger override fixture");
        second.updateName("Updated beta entry");
        second.comment("local fallback must not be appended");
        var complete = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", orderId, "delegated batch root"),
                new TraceNode(TraceKind.AUDIT_REASON, "OrderItem", secondId, "delegated beta"));
        second.setTraceChain(complete);
        fixture.clear();
        order.auditAs("replacement graph fallback").save(fixture.context);
        var overrideCommand = fixture.commands.stream().filter(value -> value.getEntity().typeName().equals("OrderItem")
                && value.getEntity().getId().equals(secondId)).findFirst().orElseThrow();
        assertEquals(complete, overrideCommand.getTraceChain());
        var overrideAudit = fixture.audit.stream().filter(value -> value.entityType().equals("OrderItem")
                && value.entityId().equals(secondId)).findFirst().orElseThrow();
        assertEquals(complete, overrideAudit.traceChain());
        assertTrue(fixture.sql.stream().anyMatch(value -> value.getOperation() == DataServiceOperation.MUTATION
                && value.getMutationLineage().equals(complete)));
        assertTrue(fixture.sql.stream().anyMatch(value -> value.getOperation() == DataServiceOperation.QUERY
                && value.getMutationLineage().equals(complete)));
        System.out.println("PASS Java generated prepared batch: per-item lineage and complete ledger replacement");
    }

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

    @Test public void generatedPreparedUpdateDeleteRecoveryCycleKeepsUnequalVersionsAndItemTraces() throws Exception {
        var fixture = new Fixture();
        Graph graph = fixture.saveNormativeGraph();
        fixture.clear();
        // Discovered through current Java Delete Assist; no generated-source lookup.
        graph.removed.markToRecover();
        graph.removed.comment("prepare previously removed item");
        graph.order.auditAs("prepare active cycle fixtures").save(fixture.context);
        assertEquals(Long.valueOf(3), E.orderItem(graph.removed).getVersion().eval());

        fixture.clear();
        graph.kept.updateName("Cycle entry alpha");
        graph.kept.comment("revise alpha");
        graph.removed.updateName("Cycle entry beta");
        graph.removed.comment("revise beta");
        graph.order.auditAs("revise generated entries").save(fixture.context);
        assertEquals(List.of(2), fixture.itemUpdateBatchSizes);
        assertCycleBoundaries(fixture, graph, "update", MutationAuditKind.UPDATED, "revise generated entries", "revise alpha", "revise beta");
        assertEquals(Long.valueOf(3), E.orderItem(graph.kept).getVersion().eval());
        assertEquals(Long.valueOf(4), E.orderItem(graph.removed).getVersion().eval());

        fixture.clear();
        graph.kept.markForDeletion();
        graph.kept.comment("remove alpha");
        graph.removed.markForDeletion();
        graph.removed.comment("remove beta");
        graph.order.auditAs("remove generated entries").save(fixture.context);
        assertEquals(List.of(2), fixture.itemDeleteBatchSizes);
        assertCycleBoundaries(fixture, graph, "delete", MutationAuditKind.DELETED, "remove generated entries", "remove alpha", "remove beta");
        assertEquals(Long.valueOf(-4), E.orderItem(graph.kept).getVersion().eval());
        assertEquals(Long.valueOf(-5), E.orderItem(graph.removed).getVersion().eval());
        assertNull(Q.orderItems().withIdIs(graph.kept.getId()).limit(1)
                .comment("what: inspect normal visibility after graph deletion")
                .purpose("why: prove pending deletion was actually saved").executeForOne(fixture.context));
        var deleted = Q.orderItems().withIdIs(graph.removed.getId()).deletedRowsOnly().limit(1)
                .comment("what: inspect retained deleted item")
                .purpose("why: verify its independent negative version").executeForOne(fixture.context);
        assertEquals(Long.valueOf(-5), E.orderItem(deleted).getVersion().eval());

        fixture.clear();
        graph.kept.markToRecover();
        graph.kept.comment("restore alpha");
        graph.removed.markToRecover();
        graph.removed.comment("restore beta");
        graph.order.auditAs("restore generated entries").save(fixture.context);
        assertEquals(List.of(2), fixture.itemRecoverBatchSizes);
        assertCycleBoundaries(fixture, graph, "recover", MutationAuditKind.RECOVERED, "restore generated entries", "restore alpha", "restore beta");
        assertEquals(Long.valueOf(5), E.orderItem(graph.kept).getVersion().eval());
        assertEquals(Long.valueOf(6), E.orderItem(graph.removed).getVersion().eval());
        var restored = Q.customerOrders().withIdIs(graph.order.getId()).limit(1)
                .selectOrderItemListWith(Q.orderItems().orderByIdAscending().limit(10))
                .comment("what: reload the restored order graph")
                .purpose("why: verify two visible children through generated Q and E").executeForOne(fixture.context);
        assertEquals(Integer.valueOf(2), E.customerOrder(restored).getOrderItemList().size().eval());
        assertEquals("Cycle entry alpha", E.orderItem(graph.kept).getName().eval());
        assertEquals("Cycle entry beta", E.orderItem(graph.removed).getName().eval());
        System.out.println("PASS Java generated prepared update/delete/recover: unequal versions and per-item lineage");
    }

    private static void assertCycleBoundaries(Fixture fixture, Graph graph, String operation, MutationAuditKind kind,
            String rootReason, String firstReason, String secondReason) {
        assertEquals(2, fixture.commands.size());
        assertEquals(2, fixture.audit.size());
        for (var item : List.of(graph.kept, graph.removed)) {
            long id = E.orderItem(item).getId().eval();
            var expected = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", graph.order.getId(), rootReason),
                    new TraceNode(TraceKind.AUDIT_REASON, "OrderItem", id, item == graph.kept ? firstReason : secondReason));
            var command = fixture.commands.stream().filter(value -> value.getEntity().getId().equals(id)).findFirst().orElseThrow();
            var audit = fixture.audit.stream().filter(value -> value.entityId().equals(id)).findFirst().orElseThrow();
            assertEquals(expected, command.getTraceChain());
            assertEquals(expected, audit.traceChain());
            assertEquals(kind, audit.kind());
            var writes = fixture.sql.stream().filter(value -> value.getOperation() == DataServiceOperation.MUTATION
                    && value.getMutationLineage().equals(expected)).toList();
            assertEquals(1, writes.size());
            assertEquals(operation, writes.get(0).getStatementOperation());
            assertEquals(Long.valueOf(1), writes.get(0).getAffectedRows());
            assertTrue(fixture.sql.stream().anyMatch(value -> value.getOperation() == DataServiceOperation.QUERY
                    && value.getMutationLineage().equals(expected)));
        }
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

    private static PaymentAttempt loadPaymentContext(Fixture fixture, Graph graph, String comment, String purpose) {
        var row = Q.paymentAttempts().withIdIs(graph.attempt.getId()).limit(1)
                .selectPaymentWith(Q.payments().limit(1)
                        .selectCustomerOrderWith(Q.customerOrders().limit(1)
                                .selectPlatformWith(Q.platforms().limit(1))))
                .comment(comment).purpose(purpose).executeForOne(fixture.context);
        assertEquals(graph.attempt.getId(), E.paymentAttempt(row).getId().eval());
        var payment = E.paymentAttempt(row).getPayment().eval();
        var order = E.payment(payment).getCustomerOrder().eval();
        var platform = E.customerOrder(order).getPlatform().eval();
        assertEquals("Trace Chain Verification", E.platform(platform).getName().eval());
        return row;
    }

    @Test public void overlappingGeneratedQueriesKeepThreeLevelRoutesOffContext() throws Exception {
        var fixture = new Fixture();
        Graph graph = fixture.saveNormativeGraph(); fixture.clear();
        var firstEntered = new CountDownLatch(1);
        var bothEntered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        fixture.queryBegin = (caller, request) -> {
            assertSame(fixture.context, caller);
            firstEntered.countDown(); bothEntered.countDown(); await(release);
            assertTrue("root and relation queries must never write a Context trace stack", caller.getTraceChain().isEmpty());
        };
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> loadPaymentContext(fixture, graph,
                    "what: inspect payment ownership", "why: render the first view"));
            await(firstEntered);
            var second = workers.submit(() -> loadPaymentContext(fixture, graph,
                    "what: inspect payment trace", "why: render the second view"));
            await(bothEntered);
            assertTrue("both real generated Q executions are live", fixture.context.getTraceChain().isEmpty());
            release.countDown();
            var left = first.get(20, TimeUnit.SECONDS); var right = second.get(20, TimeUnit.SECONDS);
            assertNotSame("hydrated root objects belong to independent queries", left, right);
            for (String comment : List.of("what: inspect payment ownership", "what: inspect payment trace")) {
                var statements = fixture.sql.stream().filter(entry -> comment.equals(entry.getComment())).toList();
                assertEquals("each query emits its own root plus three relation statements", 4, statements.size());
                String purpose = comment.endsWith("ownership") ? "why: render the first view" : "why: render the second view";
                for (int depth = 0; depth < statements.size(); depth++) {
                    var entry = statements.get(depth);
                    assertEquals(purpose, entry.getPurpose());
                    assertEquals("PaymentAttempt", entry.getTraceChain().get(0).getName());
                    assertEquals(List.of("payment", "customerOrder", "platform").subList(0, depth),
                            entry.getTraceChain().stream().filter(node -> node.getKind() == TraceKind.RELATION)
                                    .map(TraceNode::getName).toList());
                }
            }
            assertEquals(8, fixture.sql.size());
            assertTrue(fixture.context.getTraceChain().isEmpty());
        } finally {
            release.countDown(); workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
        System.out.println("PASS Java generated overlapping queries: request-owned three-level SQL paths, Context unchanged");
    }

    @Test public void generatedStreamKeepsItsIntentAcrossLateConsumption() throws Exception {
        var fixture = new Fixture(); Graph graph = fixture.saveNormativeGraph(); fixture.clear();
        fixture.streamOpen = caller -> {
            assertSame(fixture.context, caller);
            assertTrue("the actual JDBC cursor open must not depend on Context frames", caller.getTraceChain().isEmpty());
        };
        try (var stream = Q.customerOrders().withIdIs(graph.order.getId()).limit(1)
                .comment("what: stream the selected order").purpose("why: consume after another query")
                .executeForStream(fixture.context)) {
            Q.platforms().withIdIs(1L).limit(1).comment("what: inspect an unrelated platform")
                    .purpose("why: prove delayed cursors keep their own intent").executeForOne(fixture.context);
            var rows = stream.toList();
            assertEquals(1, rows.size());
            assertEquals(graph.order.getId(), E.customerOrder(rows.get(0)).getId().eval());
        }
        assertEquals(2, fixture.sql.size());
        var cursor = fixture.sql.stream().filter(entry -> "what: stream the selected order".equals(entry.getComment()))
                .findFirst().orElseThrow();
        assertEquals("why: consume after another query", cursor.getPurpose());
        assertEquals("CustomerOrder", cursor.getTraceChain().get(0).getName());
        assertTrue(fixture.context.getTraceChain().isEmpty());
        System.out.println("PASS Java generated stream: request-owned SQL path and delayed consumption intent");
    }

    private static void assertQueryPaths(Fixture fixture, String comment, String purpose,
                                         List<List<String>> expectedRelations) {
        assertEquals(expectedRelations.size(), fixture.sql.size());
        for (int index = 0; index < fixture.sql.size(); index++) {
            var entry = fixture.sql.get(index);
            assertEquals(comment, entry.getComment()); assertEquals(purpose, entry.getPurpose());
            var kinds = new java.util.ArrayList<>(List.of(TraceKind.OPERATION, TraceKind.REQUEST));
            expectedRelations.get(index).forEach(relation -> kinds.add(TraceKind.RELATION));
            kinds.add(TraceKind.PROVIDER); kinds.add(TraceKind.SQL);
            assertEquals(kinds, entry.getTraceChain().stream().map(TraceNode::getKind).toList());
            assertEquals("PaymentAttempt", entry.getTraceChain().get(0).getName());
            assertEquals("PaymentAttempt", entry.getTraceChain().get(1).getName());
            assertEquals("sqlite", entry.getTraceChain().get(entry.getTraceChain().size() - 2).getName());
            assertEquals("select", entry.getTraceChain().get(entry.getTraceChain().size() - 1).getName());
            assertEquals(expectedRelations.get(index), entry.getTraceChain().stream()
                    .filter(node -> node.getKind() == TraceKind.RELATION).map(TraceNode::getName).toList());
        }
        assertTrue(fixture.context.getTraceChain().isEmpty());
    }

    @Test public void generatedNestedFacetsKeepTheOriginalRootAndLogicalRoute() throws Exception {
        var fixture = new Fixture(); Graph graph = fixture.saveNormativeGraph(); fixture.clear();
        String comment = "what: inspect payment ownership facets";
        String purpose = "why: retain the request route through nested facet materialization";
        var rows = Q.paymentAttempts().withIdIs(graph.attempt.getId()).limit(1)
                .facetByPaymentAs("payments", Q.payments().withIdIs(graph.payment.getId()).limit(1)
                        .facetByCustomerOrderAs("orders", Q.customerOrders().withIdIs(graph.order.getId()).limit(1)))
                .comment(comment).purpose(purpose).executeForList(fixture.context);
        assertEquals(1, rows.size());
        assertEquals(graph.attempt.getId(), E.paymentAttempt(rows.get(0)).getId().eval());
        var payments = rows.getFacet("payments"); assertNotNull(payments); assertEquals(1, payments.size());
        var payment = (Payment) payments.get(0);
        assertEquals(graph.payment.getId(), E.payment(payment).getId().eval());
        assertEquals(1, ((Number) payment.getDynamicProperty("count")).intValue());
        assertQueryPaths(fixture, comment, purpose, List.of(List.of(), List.of(),
                List.of("payment"), List.of("payment"), List.of("payment", "customerOrder")));
        System.out.println("PASS Java generated nested facets: filtered counts and original root/relation SQL paths");
    }

    @Test public void generatedFacetInsideALoadedRelationKeepsItsAncestorPath() throws Exception {
        var fixture = new Fixture(); Graph graph = fixture.saveNormativeGraph(); fixture.clear();
        String comment = "what: inspect related order facets";
        String purpose = "why: retain already loaded relation ancestry";
        var row = Q.paymentAttempts().withIdIs(graph.attempt.getId()).limit(1)
                .selectPaymentWith(Q.payments().limit(1)
                        .facetByCustomerOrderAs("orders", Q.customerOrders().withIdIs(graph.order.getId()).limit(1)))
                .comment(comment).purpose(purpose).executeForOne(fixture.context);
        assertEquals(graph.attempt.getId(), E.paymentAttempt(row).getId().eval());
        assertEquals(graph.payment.getId(), E.payment(E.paymentAttempt(row).getPayment().eval()).getId().eval());
        assertQueryPaths(fixture, comment, purpose, List.of(List.of(), List.of("payment"),
                List.of("payment"), List.of("payment", "customerOrder")));
        System.out.println("PASS Java generated relation facet: original root and complete inherited SQL route");
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
