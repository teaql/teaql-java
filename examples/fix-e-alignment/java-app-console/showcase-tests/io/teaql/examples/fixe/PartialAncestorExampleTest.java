package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.GeneratedRuntimeModule;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessClock;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.value.TeaQLNotLoadedException;
import io.teaql.jackson.TeaQLModule;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.sqlite.SQLiteDataSource;
import org.junit.Test;
import static org.junit.Assert.*;

/** A complete changed leaf below two untouched sparse ancestors; generated Q/E/save only. */
public class PartialAncestorExampleTest {
    static Tenant graph(UserContext context, Long orderId, boolean sparseLeaf) {
        var leaf = sparseLeaf ? Q.orderItemsWithMinimalFields() : Q.orderItems();
        return Q.tenantsWithMinimalFields().withIdIs(1L)
                .selectPurchaseOrderListWith(Q.purchaseOrdersWithMinimalFields().withIdIs(orderId)
                        .selectOrderItemListWith(leaf.orderByIdAscending().limit(10)
                                .comment("Load explicitly selected child records"))
                        .orderByIdAscending().limit(10).comment("Load the parent identity and its child list"))
                .limit(1).comment("Load two sparse ancestors above an explicitly selected leaf")
                .purpose("Only the modified entity must be complete before saving the graph")
                .executeForOne(context);
    }

    @Test public void completeModifiedLeafThroughUntouchedPartialAncestors() throws Exception {
        var source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + System.getenv("TEAQL_FIX_E_DB") + ".partial-ancestors.sqlite");
        var sql = new ExpressionBoundaryProbe.ObservedSql(source);
        var ids = new IdSpaceIdGenerator(new GeneratedFixEExampleTest.DatabaseBridge(sql));
        ids.ensureIdSpaceTable();
        var clockValue = new AtomicReference<>(LocalDateTime.of(2032, 2, 29, 10, 15, 30));
        BusinessClock clock = new BusinessClock() {
            public LocalDate businessDate(UserContext context) { return clockValue.get().toLocalDate(); }
            public LocalDateTime businessDateTime(UserContext context) { return clockValue.get(); }
        };
        var service = new SqliteDataServiceExecutor("sqlite", sql, source);
        var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .dataService("default", service).dataService("sqlite", service)
                .idGenerationService(ids).businessClock(clock).build().install(GeneratedRuntimeModule.module());
        UserContext context = new GeneratedFixEExampleTest.Context(runtime);
        context.ensureSchema();
        var order = Q.purchaseOrders().comment("Prepare a complete ancestor control")
                .purpose("Exercise partial graph traversal against real SQLite").newEntity(context);
        order.updateContactPhone("5550600"); order.updateInternalNote(null);
        var item = Q.orderItems().comment("Prepare a complete leaf control")
                .purpose("Persist the graph only through generated audited saves").newEntity(context);
        item.updatePurchaseOrder(order); item.updateProductName("Partial graph original");
        item.updateUnitPrice(new BigDecimal("10.00"));
        item.auditAs("Create the complete partial-ancestor fixture").save(context);
        assertEquals(Long.valueOf(1), E.purchaseOrder(order).getVersion().eval());
        assertEquals(Long.valueOf(1), E.orderItem(item).getVersion().eval());

        var root = graph(context, order.getId(), false);
        var parent = E.tenant(root).getPurchaseOrderList().first().eval();
        var leaf = E.purchaseOrder(parent).getOrderItemList().first().eval();
        assertNotNull(parent); assertNotNull(leaf);
        assertEquals(item.getId(), E.orderItem(leaf).getId().eval());
        int before = sql.calls.get();
        var rootState = root.__internalLoadState(); var parentState = parent.__internalLoadState();
        assertThrows(TeaQLNotLoadedException.class, () -> E.tenant(root).getName().eval());
        assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(parent).getContactPhone().eval());
        var mapper = new ObjectMapper().registerModule(new TeaQLModule(context))
                .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var json = mapper.readTree(mapper.writeValueAsString(root));
        assertFalse(json.has("name"));
        assertFalse(json.get("purchaseOrderList").get(0).has("contactPhone"));
        assertEquals(before, sql.calls.get());
        assertSame(rootState, root.__internalLoadState()); assertSame(parentState, parent.__internalLoadState());
        var cases = new ArrayList<String>();
        cases.add("partial-ancestor-guards-and-JSON-no-I/O");

        var createdAt = E.orderItem(leaf).getCreateTime().eval();
        var changedAt = clockValue.get().plusDays(1); clockValue.set(changedAt);
        leaf.updateProductName("Partial graph updated");
        before = sql.calls.get();
        root.auditAs("Save a complete modified descendant through untouched sparse ancestors").save(context);
        assertTrue("Successful save must calibrate the actual provider observer", sql.calls.get() > before);
        assertSame(rootState, root.__internalLoadState()); assertSame(parentState, parent.__internalLoadState());
        assertThrows(TeaQLNotLoadedException.class, () -> E.tenant(root).getName().eval());
        assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(parent).getContactPhone().eval());
        assertEquals(Long.valueOf(1), E.tenant(root).getVersion().eval());
        assertEquals(Long.valueOf(1), E.purchaseOrder(parent).getVersion().eval());
        var stored = Q.orderItems().withIdIs(item.getId()).limit(1)
                .comment("Read back the modified complete leaf").purpose("Verify actual graph persistence").executeForOne(context);
        assertEquals("Partial graph updated", E.orderItem(stored).getProductName().eval());
        assertEquals(Long.valueOf(2), E.orderItem(stored).getVersion().eval());
        assertEquals(createdAt, E.orderItem(stored).getCreateTime().eval());
        assertEquals(changedAt, E.orderItem(stored).getUpdateTime().eval());
        cases.add("complete-leaf-saved-through-two-untouched-partial-ancestors");

        var sparseRoot = graph(context, order.getId(), true);
        var sparseParent = E.tenant(sparseRoot).getPurchaseOrderList().first().eval();
        var sparseLeaf = E.purchaseOrder(sparseParent).getOrderItemList().first().eval();
        sparseLeaf.updateProductName("Rejected partial leaf");
        var rejections = new ArrayList<Object>();
        for (int attempt = 0; attempt < 2; attempt++) {
            before = sql.calls.get();
            var error = assertThrows(CheckException.class,
                    () -> sparseRoot.auditAs("Reject an incomplete modified descendant").save(context));
            assertEquals("Rejection must happen before any provider entry", before, sql.calls.get());
            assertFalse(error.getViolates().isEmpty());
            assertEquals("Rejected partial leaf", E.orderItem(sparseLeaf).getProductName().eval());
            assertEquals(Long.valueOf(2), E.orderItem(sparseLeaf).getVersion().eval());
            assertTrue(sparseLeaf.getUpdatedProperties().contains("product_name"));
            rejections.add(error.getViolates().stream().map(v -> v.toWire(JsonFieldNamingProfile.CAMEL_CASE)).toList());
        }
        stored = Q.orderItems().withIdIs(item.getId()).limit(1)
                .comment("Read after both rejected save attempts").purpose("Verify no partial writes or version bump").executeForOne(context);
        assertEquals("Partial graph updated", E.orderItem(stored).getProductName().eval());
        assertEquals(Long.valueOf(2), E.orderItem(stored).getVersion().eval());
        cases.add("partial-leaf-rejected-twice-before-provider-with-intent-retained");
        System.out.println("JAVA_PARTIAL_ANCESTORS_RESULT=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "cases", cases, "orderId", order.getId(), "leafId", item.getId(),
                "ancestorVersions", List.of(1, 1), "leafVersion", 2, "rejections", rejections,
                "calibratedProvider", true, "generatedImplementationsModified", false)));
    }
}
