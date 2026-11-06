package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.*;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** TC-REQ-06: real generated nested queries overlap on one Context and SQLite provider. */
public class ConcurrentQueryExampleTest {
    private static final class Fixture {
        final SQLiteDataSource source = new SQLiteDataSource();
        final AtomicInteger reads = new AtomicInteger(), entered = new AtomicInteger(), eReads = new AtomicInteger(), audits = new AtomicInteger();
        final CountDownLatch first = new CountDownLatch(1), both = new CountDownLatch(2), release = new CountDownLatch(1);
        final List<Map<String, Object>> events = new CopyOnWriteArrayList<>();
        final AtomicReference<UserContext> caller = new AtomicReference<>();
        final UserContext context;
        final String database;
        volatile boolean armed;

        Fixture() {
            database = Objects.requireNonNull(System.getenv("TEAQL_FIX_E_DB")) + ".concurrent-query.sqlite";
            source.setUrl("jdbc:sqlite:" + database);
            var sql = new JdbcSqlExecutor(source) {
                @Override public List<Map<String, Object>> queryForList(String statement, Object[] arguments) {
                    reads.incrementAndGet(); return super.queryForList(statement, arguments);
                }
                @Override public <T extends Entity> List<T> query(String statement, Object[] arguments, CompiledRowMapper<T> mapper) {
                    reads.incrementAndGet(); return super.query(statement, arguments, mapper);
                }
            };
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql)); ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite", sql, source) {
                @Override public QueryResult query(UserContext current, QueryRequest request) {
                    if (armed && request.parentRequest() == null && List.of("alpha root", "beta root").contains(request.comment())) {
                        assertSame(caller.get(), current);
                        assertTrue("runtime query frames must not live on Context", current.getTraceChain().isEmpty());
                        entered.incrementAndGet(); first.countDown(); both.countDown(); await(release);
                    }
                    return super.query(current, request);
                }
            };
            var clock = new BusinessClock() {
                public LocalDate businessDate(UserContext current) { return LocalDate.of(2032, 2, 29); }
                public LocalDateTime businessDateTime(UserContext current) { return businessDate(current).atTime(10, 15, 30); }
            };
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service).idGenerationService(ids)
                    .businessClock(clock).executionLogging(true).logSink(new RuntimeLogSink() {
                        public void writeExecutionLog(UserContext current, ExecutionMetadata event) {
                            if (!armed || !"select".equals(event.getStatementOperation())) return;
                            assertSame(caller.get(), current);
                            assertTrue(current.getTraceChain().isEmpty());
                            var row = new LinkedHashMap<String, Object>();
                            row.put("comment", event.getComment()); row.put("purpose", event.getPurpose());
                            row.put("statement", event.getStatementOperation()); row.put("backend", event.getBackend());
                            row.put("request", nodes(event.getRequestTraceChain())); row.put("path", nodes(event.getTracePath()));
                            row.put("thread", Thread.currentThread().getName()); row.put("resultCount", event.getResultCount());
                            events.add(row);
                        }
                        public void writeAuditEvent(UserContext current, RawAuditEvent event) { if (armed) audits.incrementAndGet(); }
                    }).build().install(GeneratedRuntimeModule.module());
            context = new GeneratedFixEExampleTest.Context(runtime); caller.set(context); context.ensureSchema();
        }
        PurchaseOrder seed(String label) {
            var parent = Q.purchaseOrders().comment("Prepare " + label + " parent").purpose("Seed the query isolation example").newEntity(context);
            parent.updateContactPhone(label); parent.updateInternalNote(null);
            var child = Q.orderItems().comment("Prepare " + label + " child").purpose("Seed a bounded nested query").newEntity(context);
            child.updateProductName(label + " child"); child.updateUnitPrice(BigDecimal.ONE); parent.addOrderItem(child);
            return parent.auditAs("Seed one independent query graph").save(context);
        }
        PurchaseOrder query(long id, String label) {
            return Q.purchaseOrders().withIdIs(id)
                    .selectOrderItemListWith(Q.orderItems().limit(10).comment(label + " children"))
                    .limit(1).comment(label + " root").purpose(label + " purpose").executeForOne(context);
        }
        void loaded(PurchaseOrder row, String expected) {
            int before = reads.get();
            assertEquals(expected, E.purchaseOrder(row).getContactPhone().eval());
            assertEquals(Integer.valueOf(1), E.purchaseOrder(row).getOrderItemList().size().eval());
            assertEquals(expected + " child", E.purchaseOrder(row).getOrderItemList().first().getProductName().eval());
            eReads.addAndGet(reads.get() - before);
            assertEquals("E evaluation must not read the provider", before, reads.get());
        }
    }
    private static List<Map<String, String>> nodes(List<TraceNode> source) {
        return source.stream().map(node -> {
            var value = new LinkedHashMap<String, String>(); value.put("kind", node.getKind().name());
            value.put("name", node.getName()); value.put("comment", node.getComment()); return (Map<String, String>) value;
        }).toList();
    }
    private static void await(CountDownLatch latch) {
        try { assertTrue("bounded query overlap checkpoint", latch.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
    @Test public void overlappingGeneratedQueriesKeepIntentAndSqlLineageRequestOwned() throws Exception {
        var fixture = new Fixture(); var alpha = fixture.seed("alpha"); var beta = fixture.seed("beta");
        assertTrue(fixture.context.getTraceChain().isEmpty()); fixture.armed = true;
        var workers = Executors.newFixedThreadPool(2);
        try {
            var left = workers.submit(() -> fixture.query(alpha.getId(), "alpha")); await(fixture.first);
            var right = workers.submit(() -> fixture.query(beta.getId(), "beta")); await(fixture.both);
            assertEquals(2, fixture.entered.get()); assertTrue("both queries are live", !left.isDone() && !right.isDone());
            assertTrue(fixture.context.getTraceChain().isEmpty()); fixture.release.countDown();
            var leftRow = left.get(15, TimeUnit.SECONDS); var rightRow = right.get(15, TimeUnit.SECONDS);
            fixture.loaded(leftRow, "alpha"); fixture.loaded(rightRow, "beta");
        } finally {
            fixture.release.countDown(); workers.shutdownNow(); assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS));
        }
        fixture.loaded(fixture.query(alpha.getId(), "after"), "alpha");
        var result = new LinkedHashMap<String, Object>();
        result.put("round", Integer.parseInt(System.getenv("TEAQL_FIX_E_ROUND"))); result.put("overlapEntered", fixture.entered.get());
        result.put("contextUnchanged", fixture.context.getTraceChain().isEmpty()); result.put("generatedQAndE", true);
        result.put("database", "fixture.sqlite.concurrent-query.sqlite"); result.put("alphaId", alpha.getId()); result.put("betaId", beta.getId());
        result.put("events", fixture.events); result.put("eProviderReads", fixture.eReads.get()); result.put("queryAuditEvents", fixture.audits.get());
        System.out.println("JAVA_CONCURRENT_QUERY_OBSERVATION=" + new ObjectMapper().writeValueAsString(result));
        assertTrue(fixture.context.getTraceChain().isEmpty()); assertEquals(6, fixture.events.size());
        assertEquals(0, fixture.audits.get());
        for (String label : List.of("alpha", "beta", "after")) {
            var own = fixture.events.stream().filter(row -> (label + " purpose").equals(row.get("purpose"))).toList();
            assertEquals("one root and one derived physical SELECT", 2, own.size());
            assertEquals(Set.of(label + " root"), new HashSet<>(own.stream().map(row -> row.get("comment")).toList()));
            for (var row : own) {
                assertEquals("sqlite", row.get("backend")); assertEquals("select", row.get("statement"));
                String request = new ObjectMapper().writeValueAsString(row.get("request"));
                String path = new ObjectMapper().writeValueAsString(row.get("path"));
                assertTrue("root business intent survives derived work", request.contains(label + " root"));
                for (String other : List.of("alpha", "beta", "after")) if (!other.equals(label)) {
                    for (String suffix : List.of(" root", " children", " purpose")) {
                        assertFalse("no sibling intent in business chain", request.contains(other + suffix));
                        assertFalse("no sibling intent in physical path", path.contains(other + suffix));
                    }
                }
                @SuppressWarnings("unchecked") var physical = (List<Map<String, String>>) row.get("path");
                assertEquals("PROVIDER", physical.get(physical.size() - 2).get("kind"));
                assertEquals("SQL", physical.get(physical.size() - 1).get("kind"));
                assertEquals("select", physical.get(physical.size() - 1).get("name"));
                long relations = physical.stream().filter(node -> "RELATION".equals(node.get("kind"))).count();
                assertTrue(relations == 0L || relations == 1L);
                assertEquals("explicit nested comment belongs to its business child node", relations == 1L, request.contains(label + " children"));
            }
            assertEquals(1L, own.stream().filter(row -> new ObjectMapper().valueToTree(row.get("path")).toString().contains("RELATION")).count());
        }
        assertEquals(2, fixture.events.stream().filter(row -> List.of("alpha purpose", "beta purpose").contains(row.get("purpose")))
                .map(row -> row.get("thread")).distinct().count());
        result.put("passed", true);
        System.out.println("JAVA_CONCURRENT_QUERY=" + new ObjectMapper().writeValueAsString(result));
    }
}
