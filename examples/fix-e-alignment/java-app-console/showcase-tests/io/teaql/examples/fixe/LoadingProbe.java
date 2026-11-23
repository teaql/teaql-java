package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.Q;
import io.teaql.core.UserContext;
import io.teaql.core.checker.CheckException;
import io.teaql.core.value.TeaQLNotLoadedException;
import java.util.Map;
import static org.junit.Assert.*;

/** Real generated Q/E and save calls; no generated source or hand-built row states. */
final class LoadingProbe {
    static void verify(UserContext context, Long orderId) throws Exception {
        var owner = Q.purchaseOrders().withIdIs(orderId)
                .selectTenantWith(Q.tenantsWithMinimalFields().withIdIs(2L).limit(1)
                        .comment("Load only a different tenant's detail"))
                .limit(1).comment("Keep the selected reference when target detail is filtered out")
                .purpose("Verify filtered forward details retain NotLoaded semantics").executeForOne(context);
        assertNotNull(owner);
        assertEquals(Long.valueOf(1), E.purchaseOrder(owner).getTenant().getId().eval());
        var missing = assertThrows(TeaQLNotLoadedException.class,
                () -> E.purchaseOrder(owner).getTenant().getName().eval());
        assertEquals("/tenant/name", missing.toWire().attemptedInstancePath());
        assertTrue(missing.getExpressionWithBrokenPoint().contains("<broken>"));
        owner.updateContactPhone("5550102");
        owner.auditAs("Change only the complete owner, not its partial target").save(context);
        var complete = Q.purchaseOrders().withIdIs(orderId).limit(1)
                .comment("Reload the successful owner update")
                .purpose("Verify untouched partial targets do not prevent saving the owner").executeForOne(context);
        assertEquals("5550102", E.purchaseOrder(complete).getContactPhone().eval());
        var version = E.purchaseOrder(complete).getVersion().eval();
        var partial = Q.purchaseOrdersWithMinimalFields().withIdIs(orderId).limit(1)
                .comment("Load an identity-only owner")
                .purpose("Reject changes to an incomplete owner before persistence").executeForOne(context);
        partial.updateContactPhone("5550103");
        var rejected = assertThrows(CheckException.class,
                () -> partial.auditAs("Attempt to modify an incomplete owner").save(context));
        assertFalse(rejected.getViolates().isEmpty());
        var unchanged = Q.purchaseOrders().withIdIs(orderId).limit(1)
                .comment("Reload after the rejected incomplete mutation")
                .purpose("Verify persisted values and version remain unchanged").executeForOne(context);
        assertEquals("5550102", E.purchaseOrder(unchanged).getContactPhone().eval());
        assertEquals(version, E.purchaseOrder(unchanged).getVersion().eval());
        System.out.println("CORE_LOAD_EXAMPLE=" + new ObjectMapper().writeValueAsString(Map.of(
                "language", "java", "passed", true, "foreignKeyPreserved", true,
                "filteredDetailNotLoaded", missing.toWire(), "untouchedPartialTargetSave", true,
                "modifiedPartialOwnerRejected", true, "rejectedVersionUnchanged", true)));
    }
}
