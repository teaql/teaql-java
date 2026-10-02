package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.GenericSQLProperty;
import io.teaql.core.sql.SQLEntityDescriptor;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import io.teaql.sqlite.GraphTraceSqliteTest.GraphEntity;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** #202: actual independent SQLite providers share services, not a graph route. */
public class MutationRouteSqliteTest {
    private static final class Fixture {
        final List<ExecutionMetadata> sql = new CopyOnWriteArrayList<>();
        final List<SafeAuditEvent> audit = new CopyOnWriteArrayList<>();
        final List<String> providerCalls = new CopyOnWriteArrayList<>();
        final JdbcSqlExecutor orders;
        final JdbcSqlExecutor payments;
        final DefaultUserContext context;
        final CountDownLatch entered = new CountDownLatch(2);
        final CountDownLatch proceed = new CountDownLatch(1);
        volatile boolean pause;

        Fixture() throws Exception {
            var orderSource = source(); var paymentSource = source();
            orders = new JdbcSqlExecutor(orderSource); payments = new JdbcSqlExecutor(paymentSource);
            var metadata = new SimpleEntityMetaFactory();
            for (String type : List.of("CustomerOrder", "Payment")) {
                var descriptor = new SQLEntityDescriptor();
                descriptor.setType(type); descriptor.setTargetType(GraphEntity.class);
                descriptor.setEntitySupplier(() -> new GraphEntity(type));
                descriptor.setDataService(type.equals("CustomerOrder") ? "orders" : "payments");
                for (String field : List.of("id", "version", "name")) {
                    var property = (GenericSQLProperty) descriptor.addSimpleProperty(field,
                            field.equals("name") ? String.class : Long.class);
                    property.setColumnType(field.equals("name") ? "VARCHAR(255)" : "BIGINT");
                }
                var children = new Relation(); children.setName("children"); children.setOwner(descriptor);
                children.setType(new SimplePropertyType(SmartList.class));
                var properties = new ArrayList<>(descriptor.getProperties()); properties.add(children);
                descriptor.setProperties(properties); metadata.register(descriptor);
            }
            var orderProvider = provider("orders", orders, orderSource);
            var paymentProvider = provider("payments", payments, paymentSource);
            var runtime = TeaQLRuntime.builder().metadata(metadata)
                    .dataService("orders", orderProvider).dataService("payments", paymentProvider)
                    .logSink((caller, entry) -> sql.add(entry)).build();
            context = new DefaultUserContext(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audit.add(event));
            // Explicit fixture schema capability; do not claim default multi-route schema orchestration.
            context.putAttribute(SchemaExecutor.class.getName(), new SchemaExecutor() {
                @Override public String name() { return "fixture-schema"; }
                @Override public DataServiceCapabilities capabilities() { return orderProvider.capabilities(); }
                @Override public void ensureSchema(UserContext caller, Invocation invocation) {
                    orderProvider.ensureSchema(caller, invocation); paymentProvider.ensureSchema(caller, invocation);
                }
            });
            context.ensureSchema(); sql.clear(); audit.clear(); providerCalls.clear();
        }

        private static SQLiteDataSource source() throws Exception {
            var source = new SQLiteDataSource();
            source.setUrl("jdbc:sqlite:" + Files.createTempFile("teaql-route-isolation-", ".db"));
            return source;
        }

        private SqliteDataServiceExecutor provider(String route, JdbcSqlExecutor driver, SQLiteDataSource source) {
            return new SqliteDataServiceExecutor(route, driver, source) {
                @Override public MutationResult mutate(UserContext caller, PersistenceMutation mutation) {
                    providerCalls.add(route);
                    if (pause) {
                        entered.countDown();
                        try {
                            if (!proceed.await(10, TimeUnit.SECONDS)) throw new AssertionError("SQLite route overlap timeout");
                        } catch (InterruptedException error) {
                            Thread.currentThread().interrupt(); throw new AssertionError(error);
                        }
                    }
                    return super.mutate(caller, mutation);
                }
            };
        }

        GraphEntity entity(String type, long id, String name) {
            var entity = new GraphEntity(type);
            entity.__internalInitializeNewEntityId(id); entity.updateProperty("name", name);
            return entity;
        }

        long count(JdbcSqlExecutor driver, String table) {
            return ((Number) driver.queryForList("SELECT count(*) AS total FROM " + table,
                    new Object[0]).get(0).get("total")).longValue();
        }
    }

    @Test public void independentDifferentRouteSavesCommitOnlyToTheirOwnDatabases() throws Exception {
        var fixture = new Fixture();
        fixture.entity("CustomerOrder", 100L, "local order").auditAs("separate order request").save(fixture.context);
        fixture.entity("Payment", 100L, "local payment").auditAs("separate payment request").save(fixture.context);
        assertEquals(List.of("orders", "payments"), fixture.providerCalls);
        assertEquals(1, fixture.count(fixture.orders, "customer_order_data"));
        assertEquals(0, fixture.count(fixture.orders, "payment_data"));
        assertEquals(0, fixture.count(fixture.payments, "customer_order_data"));
        assertEquals(1, fixture.count(fixture.payments, "payment_data"));
        assertEquals(2, fixture.audit.size());
        for (var event : fixture.audit) {
            String reason = event.entityType().equals("CustomerOrder") ? "separate order request" : "separate payment request";
            assertEquals(List.of(reason), event.traceChain().stream().map(TraceNode::getComment).toList());
            var statements = fixture.sql.stream().filter(entry -> entry.getMutationLineage().equals(event.traceChain())).toList();
            assertEquals("actual write and authoritative readback", 2, statements.size());
            assertTrue(statements.stream().anyMatch(entry -> entry.getOperation() == DataServiceOperation.MUTATION));
            assertTrue(statements.stream().anyMatch(entry -> entry.getOperation() == DataServiceOperation.QUERY));
            assertTrue(statements.stream().allMatch(entry -> reason.equals(entry.getAuditReason())));
        }
        assertNull(fixture.context.getAttribute("__teaql_save_graph_route__"));
    }

    @Test public void independentOverlappingRoutesCommitToDifferentDatabasesWithSeparateLineage() throws Exception {
        var fixture = new Fixture(); fixture.pause = true;
        var workers = Executors.newFixedThreadPool(2);
        try {
            var order = workers.submit(() -> fixture.entity("CustomerOrder", 100L, "parallel order")
                    .auditAs("parallel order request").save(fixture.context));
            var payment = workers.submit(() -> fixture.entity("Payment", 100L, "parallel payment")
                    .auditAs("parallel payment request").save(fixture.context));
            assertTrue("both provider invocations must overlap", fixture.entered.await(10, TimeUnit.SECONDS));
            assertNull(fixture.context.getAttribute("__teaql_save_graph_route__"));
            assertTrue(fixture.sql.isEmpty()); assertTrue(fixture.audit.isEmpty());
            fixture.proceed.countDown(); order.get(10, TimeUnit.SECONDS); payment.get(10, TimeUnit.SECONDS);
        } finally {
            fixture.proceed.countDown(); workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(1, fixture.count(fixture.orders, "customer_order_data"));
        assertEquals(0, fixture.count(fixture.orders, "payment_data"));
        assertEquals(0, fixture.count(fixture.payments, "customer_order_data"));
        assertEquals(1, fixture.count(fixture.payments, "payment_data"));
        assertEquals(2, fixture.audit.size());
        for (var event : fixture.audit) {
            String reason = event.entityType().equals("CustomerOrder") ? "parallel order request" : "parallel payment request";
            assertEquals(List.of(reason), event.traceChain().stream().map(TraceNode::getComment).toList());
            // The SQL parameter value ("parallel order/payment") occurs in the
            // request prose, so the safe SQL sink must redact that substring.
            var safeLineage = List.of(new TraceNode(TraceKind.AUDIT_REASON,
                    event.entityType(), 100L, "[REDACTED] request"));
            var statements = fixture.sql.stream().filter(entry -> entry.getMutationLineage().equals(safeLineage)).toList();
            assertEquals("observed safe SQL lineage: " + fixture.sql.stream()
                    .map(entry -> entry.getMutationLineage().toString()).toList(), 2, statements.size());
            assertTrue(statements.stream().allMatch(entry -> "[REDACTED] request".equals(entry.getAuditReason())));
            assertTrue(statements.stream().anyMatch(entry -> entry.getOperation() == DataServiceOperation.MUTATION));
            assertTrue(statements.stream().anyMatch(entry -> entry.getOperation() == DataServiceOperation.QUERY));
        }
    }

    @Test public void mixedGraphFailsBeforeEitherDatabaseWrites() throws Exception {
        var fixture = new Fixture();
        var order = fixture.entity("CustomerOrder", 100L, "pending order");
        var payment = fixture.entity("Payment", 100L, "pending payment");
        order.updateProperty("children", List.of(payment));
        var error = assertThrows(TeaQLRuntimeException.class,
                () -> order.auditAs("cross-provider graph is not atomic").save(fixture.context));
        assertTrue(error.getMessage().contains("CROSS-PROVIDER MUTATION"));
        assertTrue(fixture.providerCalls.isEmpty()); assertTrue(fixture.sql.isEmpty()); assertTrue(fixture.audit.isEmpty());
        assertEquals(0, fixture.count(fixture.orders, "customer_order_data"));
        assertEquals(0, fixture.count(fixture.orders, "payment_data"));
        assertEquals(0, fixture.count(fixture.payments, "payment_data"));
        assertTrue(order.getEntityMutationLedger().isNew(new EntityKey("Payment", 100L)));
    }
}
