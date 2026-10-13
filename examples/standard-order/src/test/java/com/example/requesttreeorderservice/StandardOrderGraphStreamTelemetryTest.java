package com.example.requesttreeorderservice;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import io.teaql.core.InternalIdGenerationService;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.AppAuditEventSink;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.SafeAuditEvent;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Generated Q/E and audited graph save; no SDK imports or handwritten application SQL. */
public class StandardOrderGraphStreamTelemetryTest {
    static final String PRIVATE = "STANDARD_GRAPH_STREAM_PRIVATE_7a3120c8";

    @Test public void graphSaveKeepsInheritedAndExplicitEntityCommentsAndIndependentAudit() throws Exception {
        try (var fixture = new Fixture()) {
            var context = fixture.context;
            var platform = Q.platforms().limit(1).comment("load graph root seed")
                    .purpose("prepare bounded graph fixture").executeForOne(context);
            var customer = Q.customers().comment("prepare graph customer").purpose("prepare bounded graph fixture").newEntity(context);
            customer.updateName(PRIVATE); customer.updatePlatform(platform);
            customer.auditAs("create graph customer");
            var order = Q.customerOrders().comment("prepare graph order").purpose("prepare bounded graph fixture").newEntity(context);
            order.updateCustomer(customer); order.updateOrderNumber(PRIVATE);
            order.updateOrderDate(LocalDate.of(2026, 10, 12)); order.updateTotalAmount(BigDecimal.valueOf(60));
            order.updateStatusToNew(); order.setComment(null); // Intentionally inherit the graph entry's mutation comment.
            var item = Q.orderItems().comment("prepare graph entry").purpose("prepare bounded graph fixture").newEntity(context);
            item.updateCustomerOrder(order); item.updateProductName("Controlled graph box");
            item.updateQuantity(3); item.updateUnitPrice(BigDecimal.valueOf(20));
            int before = fixture.audits.size();
            item.auditAs("save bounded order graph").save(context);
            var committed = fixture.audits.subList(before, fixture.audits.size());
            assertEquals(3, committed.size());
            assertEquals(java.util.Set.of("Customer", "CustomerOrder", "OrderItem"),
                    committed.stream().map(SafeAuditEvent::entityType).collect(java.util.stream.Collectors.toSet()));
            for (var event : committed) {
                var comments = event.traceChain().stream().map(io.teaql.core.TraceNode::getComment).toList();
                assertTrue(comments.contains("save bounded order graph"));
                if (event.entityType().equals("Customer")) assertTrue(comments.contains("create graph customer"));
            }
            var actual = Q.orderItems().withIdIs(item.getId())
                    .selectCustomerOrderWith(Q.customerOrders())
                    .limit(1).comment("verify persisted bounded graph").purpose("verify graph audit independent of export")
                    .executeForOne(context);
            assertEquals("Controlled graph box", E.orderItem(actual).getProductName().eval());
            var parent = E.orderItem(actual).getCustomerOrder().eval();
            assertEquals(PRIVATE, E.customerOrder(parent).getOrderNumber().eval());
            assertEquals(1, Q.customers().withIdIs(customer.getId()).limit(1)
                    .comment("verify persisted graph customer").purpose("verify graph audit independent of export").executeForList(context).size());
            assertEquals(before + 3, fixture.audits.size()); assertEquals(0, fixture.ordinaryLogs.get());
            fixture.runtime.getTelemetry().flush();
        }
        System.out.println("JAVA_STANDARD_GRAPH PASSED committedEntities=3 inherited=true explicit=true ordinarySqlLogs=0");
    }

    @Test public void streamsRetainRootAndCloseResourcesOnExhaustionAndEarlyClose() throws Exception {
        try (var fixture = new Fixture()) {
            var context = fixture.context;
            var platform = Q.platforms().limit(1).comment("load stream root seed").purpose("prepare bounded stream fixture").executeForOne(context);
            var customer = Q.customers().comment("prepare stream customer").purpose("prepare bounded stream fixture").newEntity(context);
            customer.updateName("Stream retail"); customer.updatePlatform(platform); customer.auditAs("seed stream customer").save(context);
            for (int index = 1; index <= 3; index++) {
                var order = Q.customerOrders().comment("prepare stream order").purpose("prepare bounded stream fixture").newEntity(context);
                order.updateCustomer(customer); order.updateOrderNumber(PRIVATE + "-" + index);
                order.updateOrderDate(LocalDate.of(2026, 10, index)); order.updateTotalAmount(BigDecimal.valueOf(index * 10));
                order.updateStatusToNew(); order.auditAs("seed stream order").save(context);
            }
            int before = fixture.audits.size();
            List<CustomerOrder> rows;
            try (var stream = Q.customerOrdersWithMinimalFields().selectOrderNumber().orderByIdAscending().limit(3)
                    .comment("stream complete bounded orders").purpose("verify delayed stream completion").executeForStream(context)) {
                // The public list executes while a stream is open: its scope must
                // not accidentally inherit a leaked stream activation.
                assertEquals(1, Q.platformsWithMinimalFields().limit(1).comment("query between stream reads")
                        .purpose("verify stream activation isolation").executeForList(context).size());
                rows = stream.toList();
            }
            assertEquals(3, rows.size());
            for (int index = 0; index < 3; index++) assertEquals(PRIVATE + "-" + (index + 1), E.customerOrder(rows.get(index)).getOrderNumber().eval());
            assertEquals(0, fixture.source.openConnections.get());
            try (var stream = Q.customerOrdersWithMinimalFields().selectOrderNumber().orderByIdAscending().limit(3)
                    .comment("stream early-close bounded orders").purpose("verify explicit stream cancellation").executeForStream(context)) {
                var first = stream.findFirst().orElseThrow();
                assertEquals(PRIVATE + "-1", E.customerOrder(first).getOrderNumber().eval());
            }
            assertEquals(0, fixture.source.openConnections.get());
            assertEquals(before, fixture.audits.size()); assertEquals(0, fixture.ordinaryLogs.get());
            fixture.runtime.getTelemetry().flush();
        }
        System.out.println("JAVA_STANDARD_STREAM PASSED exhaustedRows=3 cancelledRows=1 connections=0 auditsUnchanged=true ordinarySqlLogs=0");
    }

    private static final class Fixture implements AutoCloseable {
        final CountingSource source = new CountingSource();
        final ArrayList<SafeAuditEvent> audits = new ArrayList<>();
        final AtomicInteger ordinaryLogs = new AtomicInteger();
        final TeaQLRuntime runtime;
        final DefaultUserContext context;
        Fixture() throws Exception {
            source.setUrl("jdbc:sqlite:" + Files.createTempDirectory("teaql-java-standard-graph-stream-").resolve("orders.sqlite"));
            var service = new SqliteDataServiceExecutor("sqlite", new JdbcSqlExecutor(source), source);
            var ids = new AtomicLong(4000);
            runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(new InternalIdGenerationService() {
                        public Long generateId(UserContext caller, io.teaql.core.Entity entity) { return ids.incrementAndGet(); }
                        public void ensureFloor(String type, long floor) { ids.accumulateAndGet(floor, Math::max); }
                    }).queryExecutionLogging(false).mutationExecutionLogging(false)
                    .logSink((caller, metadata) -> ordinaryLogs.incrementAndGet()).build().install(GeneratedRuntimeModule.module());
            context = new DefaultUserContext(runtime);
            context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> audits.add(event));
            context.ensureSchema(); assertEquals(4, audits.size()); context.ensureSchema(); assertEquals(4, audits.size());
        }
        public void close() { runtime.close(); }
    }

    /** Infrastructure observation only: driver calls and SQL remain runtime-owned. */
    private static final class CountingSource extends SQLiteDataSource {
        final AtomicInteger openConnections = new AtomicInteger();
        @Override public java.sql.Connection getConnection() throws java.sql.SQLException {
            var connection = super.getConnection(); openConnections.incrementAndGet();
            var closed = new java.util.concurrent.atomic.AtomicBoolean();
            return (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(java.sql.Connection.class.getClassLoader(),
                    new Class<?>[]{java.sql.Connection.class}, (proxy, method, args) -> {
                        try { return method.invoke(connection, args); }
                        catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                        finally { if (method.getName().equals("close") && closed.compareAndSet(false, true)) openConnections.decrementAndGet(); }
                    });
        }
    }
}
