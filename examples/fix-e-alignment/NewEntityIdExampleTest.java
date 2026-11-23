package io.teaql.examples.fixe;

import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.Q;
import io.teaql.core.EntityKey;
import io.teaql.core.checker.CheckException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** Ordinary generated Q/E and audited saves; SQL below is a read-only oracle. */
public class NewEntityIdExampleTest {
    @Test public void generatedFactoriesAssignRealIdsBeforeReturningAndSaveNeverReallocatesThem() throws Exception {
        var f = new ContextRootProbe.Fixture(System.getenv("TEAQL_FIX_E_DB") + ".new-entity-id.sqlite");
        int rowsBefore = f.storage().size(); var factoryBefore = f.counters();
        var order = f.parent(); var abandoned = f.parent(); var independent = f.parent();
        var item = Q.orderItems().comment("Prepare a newly identified graph child")
                .purpose("Verify real identity precedes relation assignment and audited save").newEntity(f.context);
        long orderId = E.purchaseOrder(order).getId().eval();
        long abandonedId = E.purchaseOrder(abandoned).getId().eval();
        long independentId = E.purchaseOrder(independent).getId().eval();
        long itemId = E.orderItem(item).getId().eval();
        assertTrue(orderId > 0 && abandonedId > 0 && independentId > 0 && itemId > 0);
        assertEquals(3, Set.of(orderId, abandonedId, independentId).size());
        for (var row : java.util.List.of(order, abandoned, independent, item)) {
            assertTrue(row.newItem()); assertNull(row.getVersion());
            assertTrue(row.getEntityMutationLedger().isNew(new EntityKey(row.typeName(), row.getId())));
            assertTrue(row.isPropertyLoaded("id"));
        }
        assertNotSame(order.getEntityMutationLedger(), item.getEntityMutationLedger());
        assertNotSame(order.getEntityMutationLedger(), independent.getEntityMutationLedger());
        assertNotSame(order.getEntityMutationLedger(), abandoned.getEntityMutationLedger());
        var independentLedger = independent.getEntityMutationLedger();
        var independentPending = Set.copyOf(independent.getUpdatedProperties());
        var factoryEffects = f.delta(factoryBefore);
        assertEquals(Integer.valueOf(4), factoryEffects.get("allocations"));
        for (String callback : java.util.List.of("policies", "reviews", "audits"))
            assertEquals(callback, Integer.valueOf(0), factoryEffects.get(callback));
        assertEquals(rowsBefore, f.storage().size());

        item.updatePurchaseOrder(order); item.updateProductName("Identified graph child"); item.updateUnitPrice(BigDecimal.ONE);
        var saveBefore = f.counters();
        item.auditAs("Persist only the explicitly identified parent and child graph").save(f.context);
        var saveEffects = f.delta(saveBefore);
        assertEquals(Integer.valueOf(0), saveEffects.get("allocations"));
        assertTrue(saveEffects.get("writes") > 0); assertTrue(saveEffects.get("transactions") > 0);
        assertEquals(Integer.valueOf(2), saveEffects.get("audits"));
        assertEquals(Long.valueOf(orderId), E.purchaseOrder(order).getId().eval());
        assertEquals(Long.valueOf(itemId), E.orderItem(item).getId().eval());
        assertEquals(Long.valueOf(1), E.purchaseOrder(order).getVersion().eval());
        assertEquals(Long.valueOf(1), E.orderItem(item).getVersion().eval());
        assertEquals("5550987", E.purchaseOrder(f.loaded(orderId)).getContactPhone().eval());
        var child = Q.orderItems().withIdIs(itemId).limit(1)
                .comment("Reload the identified graph child")
                .purpose("Prove generated E observes the saved factory identity").executeForOne(f.context);
        assertEquals(Long.valueOf(itemId), E.orderItem(child).getId().eval());
        assertEquals("Identified graph child", E.orderItem(child).getProductName().eval());
        assertNull(f.loaded(abandonedId)); assertNull(f.loaded(independentId));
        assertSame(independentLedger, independent.getEntityMutationLedger());
        assertEquals(independentPending, Set.copyOf(independent.getUpdatedProperties()));
        assertEquals(Set.of(new EntityKey("PurchaseOrder", independentId)), independentLedger.newKeys());

        // Reserving an ID is allowed before validation; a rejected save must
        // neither replace that ID nor turn it into a persisted version.
        var rejected = f.parent(); long retryId = E.purchaseOrder(rejected).getId().eval();
        rejected.updateContactPhone(""); var rejectionBefore = f.counters();
        assertThrows(CheckException.class, () -> rejected.auditAs("Reject invalid business input without reallocating its identity").save(f.context));
        var rejectionEffects = f.delta(rejectionBefore);
        assertTrue(rejectionEffects.toString(), rejectionEffects.values().stream().allMatch(v -> v == 0));
        assertEquals(Long.valueOf(retryId), E.purchaseOrder(rejected).getId().eval());
        assertNull(rejected.getVersion()); assertTrue(rejected.newItem());
        assertTrue(rejected.getEntityMutationLedger().isNew(new EntityKey("PurchaseOrder", retryId)));
        rejected.updateContactPhone("5550222"); var retryBefore = f.counters();
        rejected.auditAs("Retry the corrected object with its original assigned identity").save(f.context);
        var retryEffects = f.delta(retryBefore);
        assertEquals(Integer.valueOf(0), retryEffects.get("allocations"));
        assertEquals(Long.valueOf(retryId), E.purchaseOrder(rejected).getId().eval());
        assertEquals(Long.valueOf(1), E.purchaseOrder(f.loaded(retryId)).getVersion().eval());
        assertEquals("5550222", E.purchaseOrder(f.loaded(retryId)).getContactPhone().eval());
        assertEquals(rowsBefore + 2, f.storage().size());
        try (var connection = f.raw.getConnection(); var statement = connection.prepareStatement(
                "SELECT version,purchase_order,product_name FROM order_item_data WHERE id=?")) {
            statement.setLong(1, itemId);
            try (var row = statement.executeQuery()) {
                assertTrue(row.next()); assertEquals(1L, row.getLong(1)); assertEquals(orderId, row.getLong(2));
                assertEquals("Identified graph child", row.getString(3)); assertFalse(row.next());
            }
        }
        var result = new LinkedHashMap<String,Object>();
        result.put("language", "java"); result.put("case", "new-entity-id-before-return"); result.put("passed", true);
        result.put("orderId", orderId); result.put("itemId", itemId); result.put("retryId", retryId);
        result.put("abandonedId", abandonedId); result.put("independentId", independentId);
        result.put("factoryEffects", factoryEffects); result.put("saveEffects", saveEffects);
        result.put("rejectionEffects", rejectionEffects); result.put("retryEffects", retryEffects);
        result.put("rowsBefore", rowsBefore); result.put("rowsAfter", f.storage().size());
        result.put("identityStableThroughSaveAndRetry", true); result.put("independentPendingIntentRetained", true);
        result.put("abandonedRowsPersisted", false); result.put("finalStorage", f.storage());
        System.out.println("JAVA_NEW_ENTITY_ID_RESULT=" + ContextRootProbe.JSON.writeValueAsString(result));
    }
}
