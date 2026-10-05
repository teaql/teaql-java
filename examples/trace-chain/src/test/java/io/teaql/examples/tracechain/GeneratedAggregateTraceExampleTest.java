package io.teaql.examples.tracechain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.tracechainservice.E;
import com.teaql.tracechainservice.Q;
import com.teaql.tracechainservice.customerorder.CustomerOrder;
import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceKind;
import io.teaql.core.TraceNode;
import io.teaql.runtime.LogPrivacy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Application-owned TC-SQL-10: generated Q/E plus inherited runtime aggregate APIs. */
public class GeneratedAggregateTraceExampleTest {
    private static final String PRIVATE = "JAVA-PRIVATE-AGGREGATE";
    private static final String COMMENT = "inspect " + PRIVATE;
    private static final String PURPOSE = "verify original aggregate ancestry";

    @Test public void generatedAggregateRetainsRawPathsPrivacyAndNumericPartition() throws Exception {
        for (boolean nested : List.of(false, true)) for (boolean logging : List.of(false, true)) {
            var fixture = new GeneratedTraceChainExampleTest.Fixture(logging);
            var graph = fixture.saveNormativeGraph();
            graph.kept().updateName(PRIVATE);
            graph.order().auditAs("seed private aggregate operand").save(fixture.context);
            fixture.clear(); fixture.queryResults.clear();

            var count = Q.orderItems().withNameIs(PRIVATE).limit(10);
            // Public BaseRequest contract, exercised by DerivedQueryTraceSqliteTest.
            // Partition by the real forward model edge; runtime resolves the reverse edge.
            count.setPartitionProperty("customerOrder"); count.count("count");
            var owner = Q.customerOrders().withIdIs(graph.order().getId()).limit(1);
            owner.addSingleAggregateDynamicProperty("selectedItemCount", count);
            CustomerOrder result = nested
                    ? E.payment(Q.payments().withIdIs(graph.payment().getId()).limit(1)
                        .selectCustomerOrderWith(owner).comment(COMMENT).purpose(PURPOSE)
                        .executeForOne(fixture.context)).getCustomerOrder().eval()
                    : owner.comment(COMMENT).purpose(PURPOSE).executeForOne(fixture.context);
            assertEquals(graph.order().getId(), E.customerOrder(result).getId().eval());
            assertEquals("related aggregate must retain its actual scoped count", 1,
                    ((Number) result.getDynamicProperty("selectedItemCount")).intValue());
            var routes = nested ? List.of(List.<String>of(), List.of("customerOrder"),
                    List.of("customerOrder", "orderItemList"))
                    : List.of(List.<String>of(), List.of("orderItemList"));
            var raw = assertFacts(fixture, nested ? "Payment" : "CustomerOrder", routes, COMMENT,
                    PURPOSE, "inspect [REDACTED]", logging);
            assertEquals(1, raw.stream().filter(f -> f.getParameterizedQuery().toUpperCase(java.util.Locale.ROOT).contains("COUNT(")).count());
            emit("JAVA_AGGREGATE_OBSERVED", Map.of("nested", nested, "logging", logging,
                    "count", 1, "raw", observations(raw), "safe", observations(fixture.sql),
                    "mutationCommands", fixture.commands.size(), "committedAudits", fixture.audit.size()));

            fixture.clear(); fixture.queryResults.clear();
            // Identity is a numeric partition, not a model relation. It cannot add a relation frame.
            var selfCount = Q.customerOrders().withIdIs(graph.order().getId()).limit(1);
            selfCount.setPartitionProperty("id"); selfCount.count("count");
            var numericOwner = Q.customerOrders().withIdIs(graph.order().getId()).limit(1);
            numericOwner.addSingleAggregateDynamicProperty("selfCount", selfCount);
            var numeric = nested
                    ? E.payment(Q.payments().withIdIs(graph.payment().getId()).limit(1)
                        .selectCustomerOrderWith(numericOwner).comment(COMMENT).purpose("numeric partition has no edge")
                        .executeForOne(fixture.context)).getCustomerOrder().eval()
                    : numericOwner.comment(COMMENT).purpose("numeric partition has no edge").executeForOne(fixture.context);
            assertEquals(1, ((Number) numeric.getDynamicProperty("selfCount")).intValue());
            var numericRoutes = nested ? List.of(List.<String>of(), List.of("customerOrder"), List.of("customerOrder"))
                    : List.of(List.<String>of(), List.<String>of());
            var numericRaw = assertFacts(fixture, nested ? "Payment" : "CustomerOrder", numericRoutes,
                    COMMENT, "numeric partition has no edge", COMMENT, logging);
            assertEquals(1, numericRaw.stream().filter(f -> f.getParameterizedQuery().toUpperCase(java.util.Locale.ROOT).contains("COUNT(")).count());
            emit("JAVA_AGGREGATE_NUMERIC", Map.of("nested", nested, "logging", logging,
                    "count", 1, "raw", observations(numericRaw), "safe", observations(fixture.sql)));
        }
    }

    @Test public void generatedCountAndMembershipSurviveFilteredForwardDetail() throws Exception {
        for (boolean nested : List.of(false, true)) for (boolean logging : List.of(false, true))
                for (boolean filtered : List.of(false, true)) {
            var fixture = new GeneratedTraceChainExampleTest.Fixture(logging);
            var graph = fixture.saveNormativeGraph();
            graph.kept().updateName(PRIVATE);
            var extra = Q.orderItems().comment("prepare second visible member")
                    .purpose("verify full aggregate membership").newEntity(fixture.context);
            extra.updateName("Public aggregate member"); graph.order().addOrderItem(extra);
            graph.order().auditAs("seed independently scoped aggregate members").save(fixture.context);
            fixture.clear(); fixture.queryResults.clear();
            var count = Q.orderItems().withNameIs(PRIVATE).limit(10);
            count.setPartitionProperty("customerOrder"); count.count("count");
            var detail = Q.customerOrders().withIdIs(filtered ? -1L : graph.order().getId()).limit(1);
            var owner = Q.customerOrders().withIdIs(graph.order().getId()).limit(1)
                    .selectOrderItemListWith(Q.orderItems().orderByIdAscending().limit(10)
                        .selectCustomerOrderWith(detail));
            owner.addSingleAggregateDynamicProperty("selectedItemCount", count);
            var result = nested
                    ? E.payment(Q.payments().withIdIs(graph.payment().getId()).limit(1)
                        .selectCustomerOrderWith(owner).comment(COMMENT).purpose(PURPOSE)
                        .executeForOne(fixture.context)).getCustomerOrder().eval()
                    : owner.comment(COMMENT).purpose(PURPOSE).executeForOne(fixture.context);
            var items = E.customerOrder(result).getOrderItemList().eval();
            assertEquals("unfetched forward detail cannot erase list membership", 2, items.size());
            assertEquals(1, ((Number) result.getDynamicProperty("selectedItemCount")).intValue());
            for (var item : items) {
                var identity = E.orderItem(item).getCustomerOrder().eval();
                assertNotNull(identity);
                assertEquals(graph.order().getId(), E.customerOrder(identity).getId().eval());
                if (filtered) assertThrows(io.teaql.core.value.TeaQLNotLoadedException.class,
                        () -> E.customerOrder(identity).getDescription().eval());
                else assertEquals(graph.order().getDescription(), E.customerOrder(identity).getDescription().eval());
            }
            var routes = nested ? List.of(List.<String>of(), List.of("customerOrder"),
                    List.of("customerOrder", "orderItemList"), List.of("customerOrder", "orderItemList", "customerOrder"),
                    List.of("customerOrder", "orderItemList"))
                    : List.of(List.<String>of(), List.of("orderItemList"), List.of("orderItemList", "customerOrder"), List.of("orderItemList"));
            var raw = assertFacts(fixture, nested ? "Payment" : "CustomerOrder", routes, COMMENT,
                    PURPOSE, "inspect [REDACTED]", logging);
            emit("JAVA_AGGREGATE_MEMBERSHIP", Map.of("nested", nested, "logging", logging,
                    "filtered", filtered, "count", 1, "members", 2, "rootID", graph.order().getId(),
                    "foreignIDs", items.stream().map(i -> E.customerOrder(E.orderItem(i).getCustomerOrder().eval()).getId().eval()).toList(),
                    "detail", filtered ? "NotLoaded" : "Loaded", "raw", observations(raw), "safe", observations(fixture.sql)));
            if (filtered) {
                fixture.clear(); fixture.queryResults.clear();
                var full = Q.orderItems().withIdIs(graph.kept().getId()).limit(1)
                        .selectCustomerOrderWith(Q.customerOrders().limit(1))
                        .comment("independent full detail").purpose("verify edge-owned view").executeForOne(fixture.context);
                assertEquals(graph.order().getDescription(), E.customerOrder(E.orderItem(full).getCustomerOrder().eval()).getDescription().eval());
                for (var item : items) assertThrows(io.teaql.core.value.TeaQLNotLoadedException.class,
                        () -> E.customerOrder(E.orderItem(item).getCustomerOrder().eval()).getDescription().eval());
                var visibleRaw = assertFacts(fixture, "OrderItem", List.of(List.<String>of(), List.of("customerOrder")),
                        "independent full detail", "verify edge-owned view", "independent full detail", logging);
                emit("JAVA_AGGREGATE_FORWARD", Map.of("nested", nested, "logging", logging,
                        "originalDetailsStillNotLoaded", true, "fullDetailIndependent", true, "raw", observations(visibleRaw)));
            }
        }
    }

    private static List<ExecutionMetadata> assertFacts(GeneratedTraceChainExampleTest.Fixture fixture,
            String root, List<List<String>> routes, String comment, String purpose, String safeComment, boolean logging) {
        var raw = fixture.queryResults.get(fixture.queryResults.size() - 1).statements();
        assertEquals(routes.size(), raw.size()); assertEquals(logging ? raw.size() : 0, fixture.sql.size());
        for (int i = 0; i < raw.size(); i++) {
            var expected = new ArrayList<>(List.of(new TraceNode(TraceKind.OPERATION, root, null, "query"),
                    new TraceNode(TraceKind.REQUEST, root, null, "")));
            String parent = root;
            for (String relation : routes.get(i)) {
                expected.add(new TraceNode(TraceKind.RELATION, relation, null, parent + "." + relation));
                parent = relation.equals("orderItemList") ? "OrderItem" : "CustomerOrder";
            }
            expected.add(new TraceNode(TraceKind.PROVIDER, "sqlite", null, ""));
            expected.add(new TraceNode(TraceKind.SQL, "select", null, ""));
            assertFact(raw.get(i), expected, comment, purpose);
            assertFact(LogPrivacy.sql(raw.get(i), false), expected, safeComment, purpose);
            if (logging) assertFact(fixture.sql.get(i), expected, safeComment, purpose);
        }
        assertTrue(fixture.commands.isEmpty()); assertTrue(fixture.audit.isEmpty());
        assertTrue(fixture.context.getTraceChain().isEmpty()); return raw;
    }

    private static void assertFact(ExecutionMetadata fact, List<TraceNode> expected, String comment, String purpose) {
        assertEquals(expected, fact.getTraceChain()); assertEquals(comment, fact.getComment());
        assertEquals(purpose, fact.getPurpose()); assertEquals("success", fact.getExecutionOutcome());
        assertTrue(fact.getParameterizedQuery().startsWith("SELECT"));
    }

    private static List<Map<String, Object>> observations(List<ExecutionMetadata> facts) {
        return facts.stream().map(f -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", f.getTraceChain().stream().map(n -> {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("kind", n.getKind().name()); node.put("name", n.getName());
                node.put("entityId", n.getEntityId()); node.put("comment", n.getComment()); return node;
            }).toList());
            row.put("comment", f.getComment()); row.put("purpose", f.getPurpose());
            row.put("outcome", f.getExecutionOutcome()); row.put("sql", f.getParameterizedQuery()); return row;
        }).toList();
    }

    private static void emit(String marker, Object value) throws Exception {
        System.out.println(marker + " " + new ObjectMapper().writeValueAsString(value));
    }
}
