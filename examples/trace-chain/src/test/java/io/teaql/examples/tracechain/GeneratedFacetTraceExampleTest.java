package io.teaql.examples.tracechain;

import com.teaql.tracechainservice.E;
import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.payment.Payment;
import io.teaql.core.Entity;
import io.teaql.core.ExecutionMetadata;
import io.teaql.core.SmartList;
import io.teaql.core.TraceKind;
import io.teaql.core.TraceNode;
import io.teaql.runtime.LogPrivacy;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** TC-SQL-09: check returned nested metadata, not just successful facet SQL. */
public class GeneratedFacetTraceExampleTest {
    @Test public void nestedFacetMetadataRemainsAttachedToTheReturnedFacetCollection() throws Exception {
        var fixture = new GeneratedTraceChainExampleTest.Fixture();
        var graph = fixture.saveNormativeGraph();
        fixture.clear();
        var rows = Q.paymentAttempts().withIdIs(graph.attempt().getId()).limit(1)
                .facetByPaymentAs("payments", Q.payments().withIdIs(graph.payment().getId()).limit(1)
                        .facetByCustomerOrderAs("orders", Q.customerOrders().withIdIs(graph.order().getId()).limit(1)))
                .comment("load nested facet metadata").purpose("verify the returned collection, not only SQL").executeForList(fixture.context);
        assertEquals(1, rows.size());
        var payments = rows.getFacet("payments");
        assertNotNull(payments); assertEquals(1, payments.size());
        var orders = payments.getFacet("orders");
        assertNotNull("nested Facet metadata must survive materialization", orders);
        assertEquals(1, orders.size());
        assertEquals(graph.order().getId(), E.customerOrder((CustomerOrder) orders.get(0)).getId().eval());
        assertEquals(1, count((CustomerOrder) orders.get(0)));
        System.out.println("JAVA_NESTED_FACET_CARRIER returned nested metadata and count verified");
    }

    @Test public void nestedFacetCountsSurviveMaterializationBeyondTheVisiblePage() throws Exception {
        for (boolean logging : List.of(false, true)) {
            for (boolean includeAll : List.of(false, true)) {
                var fixture = new GeneratedTraceChainExampleTest.Fixture(logging);
                var graph = fixture.saveNormativeGraph();
                var payment = Q.payments().comment("prepare another facet member")
                        .purpose("verify full membership beyond one visible row").newEntity(fixture.context);
                payment.updateReferenceCode("FACET-PAYMENT-" + fixture.base);
                for (int i = 0; i < 2; i++) {
                    var attempt = Q.paymentAttempts().comment("prepare a counted attempt")
                            .purpose("verify full facet membership").newEntity(fixture.context);
                    attempt.updateReferenceCode("FACET-ATTEMPT-" + fixture.base + "-" + i);
                    payment.addPaymentAttempt(attempt);
                }
                graph.order().addPayment(payment);
                graph.order().auditAs("seed nested facet membership").save(fixture.context);
                long orderId = E.customerOrder(graph.order()).getId().eval();
                long paymentId = E.payment(payment).getId().eval();
                fixture.clear(); fixture.queryResults.clear();
                String comment = "load bounded nested payment facets";
                String purpose = "verify returned full counts and original query ancestry";
                var rows = Q.paymentAttempts()
                        .withPaymentMatching(Q.payments().filterByCustomerOrder(orderId))
                        .orderByIdDescending().limit(1)
                        .facetByPaymentAs("payments", Q.payments().filterByCustomerOrder(orderId)
                                .orderByIdDescending().limit(1)
                                .facetByCustomerOrderAs("orders", Q.customerOrders().withIdIs(orderId).limit(1), includeAll), includeAll)
                        .comment(comment).purpose(purpose).executeForList(fixture.context);
                assertEquals("bounded visible attempt page", 1, rows.size());
                var payments = rows.getFacet("payments");
                assertNotNull("first-level facet metadata", payments);
                assertEquals(1, payments.size());
                var selected = (Payment) payments.get(0);
                assertEquals(paymentId, E.payment(selected).getId().eval().longValue());
                assertEquals("first-level count is not the one-row visible page", 2, count(selected));
                var orders = payments.getFacet("orders");
                assertNotNull("nested Facet metadata must survive materialization", orders);
                assertEquals(1, orders.size());
                var order = (CustomerOrder) orders.get(0);
                assertEquals(orderId, E.customerOrder(order).getId().eval().longValue());
                assertEquals("nested count uses both filtered payments, not the visible payment page", 2, count(order));
                var raw = fixture.queryResults.get(fixture.queryResults.size() - 1).statements();
                // The page and its COUNT each materialize the predicate. Both
                // real SELECTs must belong to the originating collector.
                var routes = List.of(List.of("payment"), List.<String>of(), List.of("payment"),
                        List.<String>of(), List.of("payment"), List.of("payment"),
                        List.of("payment", "customerOrder"));
                assertEquals(routes.size(), raw.size());
                for (int i = 0; i < raw.size(); i++) {
                    assertPath(raw.get(i), routes.get(i), comment, purpose);
                    assertEquals("real SELECT completed", "success", raw.get(i).getExecutionOutcome());
                    var safe = LogPrivacy.sql(raw.get(i), false);
                    assertPath(safe, routes.get(i), comment, purpose);
                    if (i == 3 || i == 5) {
                        assertTrue("physical membership COUNT", raw.get(i).getParameterizedQuery().toUpperCase().contains("COUNT("));
                        assertFalse("membership count must not reuse the visible page limit",
                                raw.get(i).getParameterizedQuery().toUpperCase().contains("LIMIT 1"));
                    }
                }
                assertEquals(logging ? raw.size() : 0, fixture.sql.size());
                for (int i = 0; i < fixture.sql.size(); i++) assertPath(fixture.sql.get(i), routes.get(i), comment, purpose);
                assertTrue(fixture.context.getTraceChain().isEmpty());
                System.out.printf("JAVA_NESTED_FACET {\"logging\":%s,\"includeAll\":%s,\"visible\":1,\"paymentCount\":2,\"orderCount\":2,\"physicalStatements\":7,\"safeSinkStatements\":%d}%n",
                        logging, includeAll, fixture.sql.size());
            }
        }
    }

    private static int count(Entity entity) {
        return ((Number) entity.getDynamicProperty("count")).intValue();
    }

    private static void assertPath(ExecutionMetadata fact, List<String> route, String comment, String purpose) {
        var expected = new java.util.ArrayList<>(List.of(
                new TraceNode(TraceKind.OPERATION, "PaymentAttempt", null, "query"),
                new TraceNode(TraceKind.REQUEST, "PaymentAttempt", null, "")));
        String owner = "PaymentAttempt";
        for (String edge : route) {
            expected.add(new TraceNode(TraceKind.RELATION, edge, null, owner + "." + edge));
            owner = edge.equals("payment") ? "Payment" : "CustomerOrder";
        }
        expected.add(new TraceNode(TraceKind.PROVIDER, "sqlite", null, ""));
        expected.add(new TraceNode(TraceKind.SQL, "select", null, ""));
        assertEquals("complete canonical physical path", expected, fact.getTraceChain());
        assertEquals(comment, fact.getComment());
        assertEquals(purpose, fact.getPurpose());
    }
}
