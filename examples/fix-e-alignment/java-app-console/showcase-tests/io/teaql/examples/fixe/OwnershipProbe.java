package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.Q;
import io.teaql.core.UserContext;
import java.util.Map;
import static org.junit.Assert.*;

final class OwnershipProbe {
    static void verify(UserContext context) throws Exception {
        int before = Q.purchaseOrders().limit(20).comment("Inspect the initial ownership fixture")
                .purpose("Verify independent roots do not share pending changes").executeForList(context).size();
        var first = Q.purchaseOrders().comment("Prepare independent new owner A")
                .purpose("Verify Context does not own mutation ledgers").newEntity(context);
        var second = Q.purchaseOrders().comment("Prepare independent new owner B")
                .purpose("Verify Context does not own mutation ledgers").newEntity(context);
        first.updateContactPhone("5550300"); first.updateInternalNote(null);
        second.updateContactPhone("5550400"); second.updateInternalNote(null);
        var savedA = first.auditAs("Save only independent new owner A").save(context);
        assertEquals(before + 1, Q.purchaseOrders().limit(20).comment("Inspect A's save before saving B")
                .purpose("Verify the unsaved new owner did not leak into persistence").executeForList(context).size());
        var savedB = second.auditAs("Explicitly save independent new owner B").save(context);
        var batch = Q.purchaseOrders().limit(20).comment("Load independent owners in one batch")
                .purpose("Verify list roots have separate mutation boundaries").executeForList(context);
        assertEquals(before + 2, batch.size());
        var ownerA = batch.stream().filter(row -> row.getId().equals(savedA.getId())).findFirst().orElseThrow();
        var ownerB = batch.stream().filter(row -> row.getId().equals(savedB.getId())).findFirst().orElseThrow();
        ownerA.updateContactPhone("5550301"); ownerB.updateContactPhone("5550401");
        ownerA.auditAs("Save only A from the multi-root result").save(context);
        var unchanged = Q.purchaseOrders().withIdIs(savedB.getId()).limit(1)
                .comment("Inspect B after saving only A").purpose("Verify A neither persists nor commits B").executeForOne(context);
        assertEquals("5550400", E.purchaseOrder(unchanged).getContactPhone().eval());
        assertEquals(Long.valueOf(1), E.purchaseOrder(unchanged).getVersion().eval());
        ownerB.auditAs("Save B after A without discarding its pending mutation").save(context);
        var changed = Q.purchaseOrders().withIdIs(savedB.getId()).limit(1)
                .comment("Inspect the later independent B save").purpose("Verify B's pending mutation survived A's save").executeForOne(context);
        assertEquals("5550401", E.purchaseOrder(changed).getContactPhone().eval());
        assertEquals(Long.valueOf(2), E.purchaseOrder(changed).getVersion().eval());
        PageStreamOwnershipProbe.verify(context, savedA.getId(), savedB.getId());
        System.out.println("CORE_MUTATION_OWNERSHIP=" + new ObjectMapper().writeValueAsString(Map.of(
                "language", "java", "passed", true, "independentNewRoots", true,
                "batchRootIsolation", true, "siblingVersionUnchanged", true, "siblingPendingMutationRetained", true)));
    }
}
