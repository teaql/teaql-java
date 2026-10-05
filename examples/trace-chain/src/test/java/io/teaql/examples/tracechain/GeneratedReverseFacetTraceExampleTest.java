package io.teaql.examples.tracechain;

import com.teaql.tracechainservice.E;
import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.customerorder.CustomerOrder;
import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceKind;
import io.teaql.core.TraceNode;
import io.teaql.runtime.LogPrivacy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Application-owned TC-SQL-09 acceptance; no generated-library inspection or edits. */
public class GeneratedReverseFacetTraceExampleTest {
    @Test public void generatedReverseCollectionsRetainScopedFacetsFullCountsAndLoadedEmpty() throws Exception {
        for (boolean logging : List.of(false, true)) for (boolean includeAll : List.of(false, true)) {
            var fixture = new GeneratedTraceChainExampleTest.Fixture(logging);
            var graph = fixture.saveNormativeGraph();
            var extra = Q.payments().comment("prepare second child of first parent")
                    .purpose("verify full membership beyond child page").newEntity(fixture.context);
            extra.updateReferenceCode("REVERSE-EXTRA-" + fixture.base);
            graph.order().addPayment(extra);
            graph.order().auditAs("seed two members for first parent").save(fixture.context);
            var second = newOrder(fixture, graph.order(), "SECOND");
            var payment = Q.payments().comment("prepare child of second parent")
                    .purpose("verify independent relation membership").newEntity(fixture.context);
            payment.updateReferenceCode("REVERSE-SECOND-" + fixture.base);
            second.addPayment(payment);
            second.auditAs("seed independently scoped second parent").save(fixture.context);
            var empty = newOrder(fixture, graph.order(), "EMPTY");
            empty.auditAs("seed parent with no payments").save(fixture.context);
            var ids = new Long[]{graph.order().getId(), second.getId(), empty.getId()};
            fixture.clear(); fixture.queryResults.clear();
            String comment = "load independently scoped reverse collection facets";
            String purpose = "verify generated Q/E, full counts and requested empty";
            // Exact selector from local field Assist at generator 218785e2.
            var rows = Q.customerOrders().withIdIn(ids).orderByIdAscending().limit(3)
                    .selectPaymentListWith(Q.payments().orderByIdAscending().limit(1)
                            .facetByCustomerOrderAs("orders", Q.customerOrders().withIdIn(ids).limit(3), includeAll))
                    .comment(comment).purpose(purpose).executeForList(fixture.context);
            assertEquals(3, rows.size());
            for (var parent : rows) {
                long parentId = E.customerOrder(parent).getId().eval();
                int fullCount = parentId == ids[0] ? 2 : parentId == ids[1] ? 1 : 0;
                var children = E.customerOrder(parent).getPaymentList().eval();
                assertNotNull("requested empty reverse collection is loaded, not NotLoaded", children);
                assertEquals(Math.min(1, fullCount), E.customerOrder(parent).getPaymentList().size().eval().intValue());
                var facets = children.getFacet("orders");
                assertNotNull("returned generated collection retains requested Facet metadata", facets);
                assertEquals(includeAll ? 3 : fullCount == 0 ? 0 : 1, facets.size());
                for (var value : facets) {
                    var target = (CustomerOrder) value;
                    long targetId = E.customerOrder(target).getId().eval();
                    int expected = targetId == parentId ? fullCount : 0;
                    assertEquals("counts cover full scoped membership, not the visible child page",
                            expected, ((Number) target.getDynamicProperty("count")).intValue());
                }
            }
            var raw = fixture.queryResults.get(fixture.queryResults.size() - 1).statements();
            assertEquals("root plus three independently scoped child/count/target queries", 10, raw.size());
            for (int i = 0; i < raw.size(); i++) {
                var expected = new ArrayList<>(List.of(
                        new TraceNode(TraceKind.OPERATION, "CustomerOrder", null, "query"),
                        new TraceNode(TraceKind.REQUEST, "CustomerOrder", null, "")));
                if (i > 0) expected.add(new TraceNode(TraceKind.RELATION, "paymentList", null, "CustomerOrder.paymentList"));
                if (i > 0 && i % 3 == 0) expected.add(new TraceNode(TraceKind.RELATION, "customerOrder", null, "Payment.customerOrder"));
                expected.add(new TraceNode(TraceKind.PROVIDER, "sqlite", null, ""));
                expected.add(new TraceNode(TraceKind.SQL, "select", null, ""));
                assertFact(raw.get(i), expected, comment, purpose);
                assertFact(LogPrivacy.sql(raw.get(i), false), expected, comment, purpose);
                if (logging) assertFact(fixture.sql.get(i), expected, comment, purpose);
            }
            assertEquals(logging ? 10 : 0, fixture.sql.size());
            assertEquals("query metadata cannot schedule writes", 0, fixture.commands.size());
            assertEquals(0, fixture.audit.size());
            assertTrue(fixture.context.getTraceChain().isEmpty());
            System.out.printf("JAVA_GENERATED_REVERSE_FACET logging=%s includeAll=%s parents=3 fullCounts=true requestedEmpty=true physicalStatements=10 safeSinkStatements=%d mutationCommands=0%n",
                    logging, includeAll, fixture.sql.size());
        }
    }

    private static CustomerOrder newOrder(GeneratedTraceChainExampleTest.Fixture fixture, CustomerOrder first, String suffix) throws Exception {
        var order = Q.customerOrders().comment("prepare distinct reverse collection parent")
                .purpose("verify independently scoped membership").newEntity(fixture.context);
        order.updatePlatform(E.customerOrder(first).getPlatform().eval());
        order.updateOrderNumber("REVERSE-" + suffix + "-" + fixture.base);
        order.updateDescription("Reverse Facet " + suffix);
        return order;
    }

    private static void assertFact(ExecutionMetadata fact, List<TraceNode> path, String comment, String purpose) {
        assertEquals("complete physical ancestry", path, fact.getTraceChain());
        assertEquals(comment, fact.getComment()); assertEquals(purpose, fact.getPurpose());
        assertEquals("success", fact.getExecutionOutcome());
    }
}
