package io.teaql.examples.fixe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.checkerfixadvanced.E;
import com.teaql.checkerfixadvanced.Q;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import io.teaql.core.UserContext;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

final class PageStreamOwnershipProbe {
    static void verify(UserContext context, Long idA, Long idB) throws Exception {
        int before = Q.purchaseOrders().limit(20).comment("Inspect the page ownership fixture")
                .purpose("Establish the bounded exact total").executeForList(context).size();
        var sentinel = Q.purchaseOrders().comment("Prepare the page-offset sentinel")
                .purpose("Exercise a genuine nonzero page offset").newEntity(context);
        sentinel.updateContactPhone("5550500"); sentinel.updateInternalNote(null);
        sentinel.auditAs("Save the synthetic page sentinel").save(context);
        var page = Q.purchaseOrders().orderByIdDescending().offset(1, 2).count()
                .comment("Load a nonzero-offset page of independent roots")
                .purpose("Verify exact count and page ownership").executeForList(context);
        assertEquals(2, page.size()); assertEquals(before + 1, page.getTotalCount());
        exercise(context, page.stream().toList(), idA, idB, "a nonzero-offset page", "5550401", "5550402", 2);
        List<PurchaseOrder> streamed;
        try (var stream = Q.purchaseOrders().orderByIdAscending().limit(20)
                .comment("Stream independent scalar roots")
                .purpose("Verify deferred-save ownership after stream consumption").executeForStream(context)) {
            streamed = stream.toList();
        }
        assertEquals(before + 1, streamed.size());
        exercise(context, streamed, idA, idB, "a fully consumed scalar stream", "5550402", "5550403", 3);
        System.out.println("CORE_MUTATION_PAGING=" + new ObjectMapper().writeValueAsString(Map.of(
                "language", "java", "passed", true, "pageOffset", 1, "pageSize", 2,
                "exactCount", before + 1, "pageRootIsolation", true, "deferredSaveAfterContextReuse", true,
                "streamProfile", "scalar-typed-provider-controlled-chunks", "streamRootIsolation", true,
                "siblingPendingMutationRetained", true)));
    }

    private static void exercise(UserContext context, List<PurchaseOrder> rows, Long idA, Long idB,
                                 String label, String previous, String next, long version) {
        var a = rows.stream().filter(row -> row.getId().equals(idA)).findFirst().orElseThrow();
        var b = rows.stream().filter(row -> row.getId().equals(idB)).findFirst().orElseThrow();
        a.updateContactPhone("55503" + next.substring(next.length() - 2)); b.updateContactPhone(next);
        Q.tenantsWithMinimalFields().limit(1).comment("Run an unrelated query before deferred saves")
                .purpose("Verify deferred ownership survives Context reuse").executeForList(context);
        a.auditAs("Save only A from " + label).save(context);
        var persisted = Q.purchaseOrders().withIdIs(idB).limit(1).comment("Inspect B after deferred A save")
                .purpose("Verify unrelated root value and version remain untouched").executeForOne(context);
        assertEquals(previous, E.purchaseOrder(persisted).getContactPhone().eval());
        assertEquals(Long.valueOf(version), E.purchaseOrder(persisted).getVersion().eval());
        b.auditAs("Save B later from " + label).save(context);
        persisted = Q.purchaseOrders().withIdIs(idB).limit(1).comment("Inspect the later deferred B save")
                .purpose("Verify B's pending mutation survived A's commit").executeForOne(context);
        assertEquals(next, E.purchaseOrder(persisted).getContactPhone().eval());
        assertEquals(Long.valueOf(version + 1), E.purchaseOrder(persisted).getVersion().eval());
    }
}
