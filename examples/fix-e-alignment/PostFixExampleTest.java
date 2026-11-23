package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.FixEvidence;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.checker.internal.CheckerInvocation;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import static org.junit.Assert.*;

/** Application-owned adversarial vectors over the unchanged three-entity generated library. */
public class PostFixExampleTest {
    private static final LocalDateTime T0 = LocalDateTime.of(2032, 2, 29, 10, 15, 30);
    private static final String PHONE = "PRIVATE-FIX-5550100";
    private static final String NOTE = "Private ordinary business input";

    private static final class Fixture {
        final ContextRootProbe.ObservedSql sql;
        final UserContext context;
        final AtomicInteger clockCalls = new AtomicInteger();

        Fixture(String name) {
            var source = new SQLiteDataSource();
            String database = System.getenv("TEAQL_FIX_E_DB");
            assertNotNull("the example runner owns and retains its databases", database);
            source.setUrl("jdbc:sqlite:" + database + ".post-fix-" + name + ".sqlite");
            sql = new ContextRootProbe.ObservedSql(source);
            var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql));
            ids.ensureIdSpaceTable();
            var service = new SqliteDataServiceExecutor("sqlite", sql, source);
            var clock = new BusinessClock() {
                public LocalDate businessDate(UserContext caller) { return T0.toLocalDate(); }
                public LocalDateTime businessDateTime(UserContext caller) {
                    return T0.plusSeconds(clockCalls.getAndIncrement());
                }
            };
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .dataService("default", service).dataService("sqlite", service)
                    .idGenerationService(ids).businessClock(clock)
                    .build().install(GeneratedRuntimeModule.module());
            context = new GeneratedFixEExampleTest.Context(runtime);
            context.ensureSchema();
            clockCalls.set(0);
        }

        PurchaseOrder order() {
            var row = Q.purchaseOrders().comment("Prepare a post-Fix business input")
                    .purpose("Verify typed managed defaults without rewriting caller fields").newEntity(context);
            row.updateContactPhone(PHONE);
            row.updateInternalNote(NOTE);
            return row;
        }

        OrderItem item(PurchaseOrder parent, String name, BigDecimal price) {
            var row = Q.orderItems().comment("Prepare the post-Fix child")
                    .purpose("Verify graph validation and value-free Fix provenance").newEntity(context);
            row.updatePurchaseOrder(parent);
            row.updateProductName(name);
            row.updateUnitPrice(price);
            return row;
        }

        Map<String, Integer> counters() {
            return Map.of("queries", sql.queries, "writes", sql.writes, "transactions", sql.transactions);
        }
    }

    private static void released(UserContext context) {
        assertNull(CheckerInvocation.current(context));
        for (String key : List.of(Checker.TEAQL_FIX_TIME, Checker.TEAQL_DATA_CHECKED_ITEMS,
                Checker.TEAQL_DATA_CHECK_RESULT, UserContext.TEAQL_FIX_EVIDENCE_CURRENT))
            assertNull(key, context.getAttribute(key));
    }

    private static void receipt(String name, Map<String, ?> facts) throws Exception {
        System.out.println("JAVA_POST_FIX_CASE=" + new ObjectMapper().writeValueAsString(
                Map.of("case", name, "passed", true, "facts", facts)));
    }

    @Test public void generatedRequiredFieldsObserveTypedDefaultsAndPreserveOrdinaryInputs() throws Exception {
        var f = new Fixture("typed");
        var order = f.order();
        var item = f.item(order, "Private product input", new BigDecimal("12.34"));
        item.auditAs("Save the complete post-Fix control graph").save(f.context);
        assertEquals(1, f.clockCalls.get());
        assertEquals(T0, E.purchaseOrder(order).getCreateTime().eval());
        assertEquals(T0, E.purchaseOrder(order).getUpdateTime().eval());
        assertEquals(T0, E.orderItem(item).getCreateTime().eval());
        assertEquals(T0, E.orderItem(item).getUpdateTime().eval());
        assertEquals(Long.valueOf(1), E.purchaseOrder(order).getTenant().getId().eval());
        assertEquals(Long.valueOf(1), E.orderItem(item).getTenant().getId().eval());

        var loaded = Q.purchaseOrders().withIdIs(order.getId()).limit(1)
                .comment("Read the persisted post-Fix caller fields")
                .purpose("Verify Fix did not rewrite business inputs").executeForOne(f.context);
        assertEquals(PHONE, E.purchaseOrder(loaded).getContactPhone().eval());
        assertEquals(NOTE, E.purchaseOrder(loaded).getInternalNote().eval());
        assertEquals(T0, E.purchaseOrder(loaded).getCreateTime().eval());
        released(f.context);
        receipt("typed-defaults-preserve-business-input", Map.of("graphClockCaptures", 1,
                "persistedThroughGeneratedQAndE", true, "typedClockAndRoot", true));
    }

    @Test public void fixProvenanceIsCompleteValueFreeAndDetachedForParentChildGraph() throws Exception {
        var f = new Fixture("evidence");
        var order = f.order();
        var item = f.item(order, "Private product input", BigDecimal.ONE);
        item.auditAs("Observe the Fix receipt for a parent-child graph").save(f.context);
        List<FixEvidence> evidence = f.context.lastFixEvidence();
        Set<String> expected = Set.of("PurchaseOrder:create_time:CLOCK", "PurchaseOrder:update_time:CLOCK",
                "PurchaseOrder:tenant:CONTEXT", "OrderItem:create_time:CLOCK", "OrderItem:update_time:CLOCK",
                "OrderItem:tenant:CONTEXT");
        assertEquals(expected, evidence.stream().map(row -> row.entityType() + ":" + row.modelPath() + ":" + row.source())
                .collect(Collectors.toSet()));
        assertEquals("one receipt per fixed field despite bidirectional graph traversal", expected.size(), evidence.size());
        String wire = new ObjectMapper().writeValueAsString(evidence);
        for (String privateValue : List.of(PHONE, NOTE, "Private product input", T0.toString()))
            assertFalse(wire, wire.contains(privateValue));
        assertThrows(UnsupportedOperationException.class, () -> evidence.add(evidence.get(0)));
        var another = f.order();
        another.auditAs("Run an independent Fix invocation on the same context").save(f.context);
        assertEquals(6, evidence.size());
        assertEquals(3, f.context.lastFixEvidence().size());
        assertEquals(2, f.clockCalls.get());
        released(f.context);
        receipt("value-free-provenance-and-graph-once", Map.of("firstReceiptFields", 6,
                "nextReceiptFields", 3, "detachedAndImmutable", true, "businessValuesExported", false,
                "provenance", evidence));
    }

    @Test public void invalidBusinessFieldsRejectAfterFixBeforeAnyProviderEntryAndRetryRechecks() throws Exception {
        var f = new Fixture("rejection");
        var order = f.order();
        var item = f.item(order, null, new BigDecimal("-0.01"));
        var before = f.counters();
        var error = assertThrows(CheckException.class,
                () -> item.auditAs("Reject invalid input after managed Fixes").save(f.context));
        assertEquals(before, f.counters());
        Set<String> paths = error.getViolates().stream()
                .map(row -> row.toWire(JsonFieldNamingProfile.SNAKE_CASE).instancePath()).collect(Collectors.toSet());
        assertTrue(paths.toString(), paths.contains("/product_name"));
        assertTrue(paths.toString(), paths.contains("/unit_price"));
        assertTrue(f.context.lastFixEvidence().stream().anyMatch(row ->
                row.entityType().equals("OrderItem") && row.modelPath().equals("create_time")
                        && row.source() == FixEvidence.Source.CLOCK));
        assertEquals(1, f.clockCalls.get());
        released(f.context);
        item.updateProductName("Corrected product input");
        item.updateUnitPrice(BigDecimal.ONE);
        item.auditAs("Retry the corrected post-Fix input").save(f.context);
        assertEquals(2, f.clockCalls.get());
        assertTrue(f.sql.writes > before.get("writes"));
        var loaded = Q.orderItems().withIdIs(item.getId()).limit(1)
                .comment("Read the corrected item after rejected save")
                .purpose("Verify retry checks and persists the corrected business input").executeForOne(f.context);
        assertEquals("Corrected product input", E.orderItem(loaded).getProductName().eval());
        assertEquals(0, BigDecimal.ONE.compareTo(E.orderItem(loaded).getUnitPrice().eval()));
        released(f.context);
        receipt("post-fix-reject-zero-provider-and-retry", Map.of("rejectedProviderEntries", 0,
                "requiredAndRangePaths", paths, "freshInvocationOnRetry", true, "correctedRowPersisted", true));
    }
}
