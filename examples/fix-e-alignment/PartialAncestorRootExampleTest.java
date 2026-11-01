package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.Q;
import io.teaql.core.ContextRootException;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.internal.CheckerInvocation;
import io.teaql.core.value.TeaQLNotLoadedException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** The managed-root case below two untouched sparse ancestors, through generated Q/E/save. */
public class PartialAncestorRootExampleTest {
    private static List<List<Object>> itemStorage(ContextRootProbe.Fixture f) throws Exception {
        var rows = new ArrayList<List<Object>>();
        // Independent read-only oracle; never DDL, seed SQL or a replacement for audited mutation.
        try (var connection = f.raw.getConnection(); var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT id,version,tenant,purchase_order,product_name FROM order_item_data ORDER BY id")) {
            while (result.next()) rows.add(List.of(result.getLong(1), result.getLong(2),
                    result.getLong(3), result.getLong(4), result.getString(5)));
        }
        return rows;
    }

    private static void released(UserContext context) {
        assertNull(CheckerInvocation.current(context));
        for (String key : List.of(Checker.TEAQL_FIX_TIME, Checker.TEAQL_DATA_CHECKED_ITEMS,
                Checker.TEAQL_DATA_CHECK_RESULT, UserContext.TEAQL_FIX_EVIDENCE_CURRENT))
            assertNull(key, context.getAttribute(key));
    }

    @Test public void wrongActiveRootCannotBypassUntouchedPartialAncestorsAndRetryPreservesIntent() throws Exception {
        var f = new ContextRootProbe.Fixture(System.getenv("TEAQL_FIX_E_DB") + ".partial-ancestor-root.sqlite");
        var order = f.parent();
        var item = Q.orderItems().comment("Prepare a complete root-bound descendant")
                .purpose("Verify ownership admission through two untouched sparse ancestors").newEntity(f.context);
        item.updatePurchaseOrder(order); item.updateProductName("Partial root original");
        item.updateUnitPrice(BigDecimal.ONE);
        item.auditAs("Create the managed-root partial-ancestor control").save(f.context);
        var root = PartialAncestorExampleTest.graph(f.context, order.getId(), false);
        var parent = E.tenant(root).getPurchaseOrderList().first().eval();
        var leaf = E.purchaseOrder(parent).getOrderItemList().first().eval();
        assertEquals(item.getId(), E.orderItem(leaf).getId().eval());
        assertEquals(Long.valueOf(1), E.orderItem(leaf).getTenant().getId().eval());
        assertThrows(TeaQLNotLoadedException.class, () -> E.tenant(root).getName().eval());
        assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(parent).getContactPhone().eval());
        var rootState = root.__internalLoadState(); var parentState = parent.__internalLoadState();
        leaf.updateProductName("Partial root accepted retry");
        var pending = Set.copyOf(leaf.getUpdatedProperties());
        assertEquals(Set.of("product_name"), pending);
        var originalItems = itemStorage(f); var originalParents = f.storage();
        var wrong = f.withRoot("Tenant", f.otherRoot);
        var denials = new ArrayList<Map<String, Object>>();
        for (int attempt = 1; attempt <= 2; attempt++) {
            var before = f.counters();
            var error = assertThrows(ContextRootException.class,
                    () -> root.auditAs("Reject a wrong root below untouched sparse ancestors").save(wrong));
            assertEquals(ContextRootException.Reason.VALUE_MISMATCH, error.getReason());
            var effects = f.delta(before);
            assertTrue(effects.toString(), effects.values().stream().allMatch(v -> v == 0));
            assertEquals(originalItems, itemStorage(f)); assertEquals(originalParents, f.storage());
            assertSame(rootState, root.__internalLoadState()); assertSame(parentState, parent.__internalLoadState());
            assertEquals(pending, Set.copyOf(leaf.getUpdatedProperties()));
            assertEquals("Partial root accepted retry", E.orderItem(leaf).getProductName().eval());
            assertEquals(Long.valueOf(1), E.orderItem(leaf).getVersion().eval());
            assertTrue(wrong.lastFixEvidence().isEmpty()); released(wrong);
            denials.add(Map.of("attempt", attempt, "reason", error.getReason().name(), "effects", effects,
                    "pendingIntentRetained", true, "physicalStateUnchanged", true, "scopeReleased", true));
        }
        var before = f.counters();
        root.auditAs("Retry the same partial graph under its correct active root").save(f.context);
        var accepted = f.delta(before);
        assertTrue(accepted.get("writes") > 0); assertTrue(accepted.get("transactions") > 0);
        assertTrue(accepted.get("reviews") > 0); assertEquals(Integer.valueOf(1), accepted.get("audits"));
        assertEquals(Integer.valueOf(0), accepted.get("rootLookups"));
        assertSame(rootState, root.__internalLoadState()); assertSame(parentState, parent.__internalLoadState());
        assertEquals(Long.valueOf(1), E.tenant(root).getVersion().eval());
        assertEquals(Long.valueOf(1), E.purchaseOrder(parent).getVersion().eval());
        assertThrows(TeaQLNotLoadedException.class, () -> E.tenant(root).getName().eval());
        assertThrows(TeaQLNotLoadedException.class, () -> E.purchaseOrder(parent).getContactPhone().eval());
        assertTrue(leaf.getUpdatedProperties().isEmpty());
        var stored = Q.orderItems().withIdIs(item.getId()).limit(1)
                .comment("Read the authorized descendant update")
                .purpose("Verify the managed-root graph retry persisted exactly once").executeForOne(f.context);
        assertEquals("Partial root accepted retry", E.orderItem(stored).getProductName().eval());
        assertEquals(Long.valueOf(2), E.orderItem(stored).getVersion().eval());
        assertEquals(Long.valueOf(1), E.orderItem(stored).getTenant().getId().eval());
        assertEquals(originalParents, f.storage()); released(f.context);
        System.out.println("JAVA_PARTIAL_ANCESTOR_ROOT_RESULT=" + new ObjectMapper().writeValueAsString(Map.of(
                "passed", true, "orderId", order.getId(), "leafId", item.getId(), "otherRootId", f.otherRoot,
                "denials", denials, "acceptedEffects", accepted, "ancestorVersions", List.of(1, 1),
                "leafVersion", 2, "sameGraphRetry", true, "generatedQAndE", true)));
    }
}
