package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.runtime.AppAuditEventSink;
import io.teaql.runtime.EntityPersistenceMutation;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** TC-MUT-11 through actual runtime graph planning, SQLite and committed safe audit.
 * Native fixture metadata is not evidence for generated relation traversal. */
@RunWith(Parameterized.class)
public class BlankLocalReasonSqliteTest {
    @Parameterized.Parameters(name = "logging={0}")
    public static Collection<Object[]> modes() { return List.of(new Object[]{false}, new Object[]{true}); }

    private final boolean logging;
    public BlankLocalReasonSqliteTest(boolean logging) { this.logging = logging; }

    @Test public void blankLocalReasonsInheritAtCommandSqlAndCommittedAudit() throws Exception {
        String[] blanks = {null, "", " \t\r\n", "\u0085", "\u00a0", "\u2003"};
        runGraph(blanks, true);
    }

    @Test public void nonWhiteSpaceControlReasonsSurviveAtActualSinks() throws Exception {
        String[] controls = {"\u001c", "\u001d", "\u001e", "\u001f"};
        runGraph(controls, false);
    }

    private void runGraph(String[] localReasons, boolean inherit) throws Exception {
        var fixture = new GraphTraceSqliteTest.Fixture();
        fixture.queryLogging = logging;
        fixture.mutationLogging = logging;
        if (inherit) {
            for (String blank : localReasons) {
                var invalid = fixture.create("CustomerOrder", 9999, "invalid public root");
                var error = assertThrows(RequestIntentException.class, () -> invalid.auditAs(blank).save(fixture.context));
                assertRequestError(error);
                invalid.setComment(blank);
                assertRequestError(assertThrows(RequestIntentException.class, () -> fixture.context.saveGraph(invalid)));
                assertTrue(fixture.commands.isEmpty());
                assertTrue(fixture.results.isEmpty());
                assertTrue(fixture.sql.isEmpty());
                assertTrue(fixture.audit.isEmpty());
            }
        }

        // Direct DataSource connections are independent of Spring's thread-bound
        // transaction. The safe sink must see committed rows, not pending writes.
        Map<String, String> tables = Map.of("CustomerOrder", "customer_order_data", "Payment", "payment_data",
                "PaymentAttempt", "payment_attempt_data", "Shipment", "shipment_data");
        fixture.context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> {
            assertTrue(event.entityId() instanceof Number);
            long targetId = ((Number) event.entityId()).longValue();
            try (var connection = fixture.dataSource.getConnection();
                    var select = connection.prepareStatement("SELECT id, version FROM " + tables.get(event.entityType()) + " WHERE id = ?")) {
                assertTrue("the audit probe must not read through the graph's transaction", connection.getAutoCommit());
                select.setLong(1, targetId);
                try (var row = select.executeQuery()) {
                    assertTrue("safe audit arrived before real database commit", row.next());
                    assertEquals(targetId, row.getLong(1));
                    assertEquals(1, row.getLong(2));
                    assertFalse(row.next());
                }
            } catch (java.sql.SQLException failure) {
                throw new AssertionError("independent committed-row audit probe failed", failure);
            }
            fixture.audit.add(event);
        });
        var root = fixture.create("CustomerOrder", 100, "native order");
        var payment = fixture.create("Payment", 201, "native payment");
        payment.setComment("authorize payment");
        var shipment = fixture.create("Shipment", 301, "native shipment");
        shipment.setComment("prepare shipment");
        var attempts = new ArrayList<GraphTraceSqliteTest.GraphEntity>();
        for (int index = 0; index < localReasons.length; index++) {
            var attempt = fixture.create("PaymentAttempt", 400L + index, "native attempt");
            // Feed the original local value to planning, never replace blanks
            // with a parent reason or inject an expected lineage into the graph.
            attempt.setComment(localReasons[index]);
            attempts.add(attempt);
        }
        payment.__internalSet("children", attempts);
        root.__internalSet("children", List.of(payment, shipment));
        root.auditAs("submit order").save(fixture.context);

        int count = localReasons.length + 3;
        assertEquals(count, fixture.commands.size());
        assertEquals(count, fixture.results.size());
        assertEquals(count, fixture.audit.size());
        assertEquals(logging ? 2 * count : 0, fixture.sql.size());
        assertTrue("runtime-owned trace must not live on Context", fixture.context.getTraceChain().isEmpty());
        for (EntityPersistenceMutation command : fixture.commands) {
            var entity = command.getEntity();
            var expected = new ArrayList<TraceNode>();
            expected.add(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", 100L, "submit order"));
            if (entity.typeName().equals("Payment") || entity.typeName().equals("PaymentAttempt")) {
                expected.add(new TraceNode(TraceKind.AUDIT_REASON, "Payment", 201L, "authorize payment"));
                if (entity.typeName().equals("PaymentAttempt") && !inherit) {
                    expected.add(new TraceNode(TraceKind.AUDIT_REASON, "PaymentAttempt", entity.getId(),
                            localReasons[Math.toIntExact(entity.getId() - 400)]));
                }
            } else if (entity.typeName().equals("Shipment")) {
                expected.add(new TraceNode(TraceKind.AUDIT_REASON, "Shipment", 301L, "prepare shipment"));
            }
            assertEquals("planner lost an inherited or sibling branch", expected, command.getTraceChain());
            assertEquals("submit order", command.intent().comment());
            var audit = fixture.audit.stream().filter(event -> event.entityType().equals(entity.typeName())
                    && event.entityId().equals(entity.getId())).toList();
            assertEquals("independent committed target must occur exactly once", 1, audit.size());
            assertEquals(expected, audit.get(0).traceChain());
            var result = fixture.results.stream().filter(value -> value.persistedEntity().typeName().equals(entity.typeName())
                    && value.persistedEntity().getId().equals(entity.getId())).findFirst().orElseThrow();
            assertEquals(1, result.persistedEntity().getVersion().longValue());
            assertEquals(2, result.statements().size());
            for (int index = 0; index < 2; index++) {
                var statement = result.statements().get(index);
                assertEquals(expected, statement.getMutationLineage());
                assertEquals(index == 0 ? null : "submit order", statement.getComment());
                assertEquals("submit order", statement.getAuditReason());
                assertEquals("success", statement.getExecutionOutcome());
                assertEquals(index == 0 ? "insert" : "select", statement.getStatementOperation());
                assertEquals("CustomerOrder", statement.getTraceChain().get(0).getName());
                var tail = statement.getTraceChain().size();
                assertEquals("sqlite", statement.getTraceChain().get(tail - 2).getName());
                assertEquals(index == 0 ? "insert" : "select", statement.getTraceChain().get(tail - 1).getName());
            }
            for (var diagnostic : fixture.sql.stream().filter(value -> value.getMutationLineage().equals(expected)).toList()) {
                assertEquals(diagnostic.getStatementOperation().equals("select") ? "submit order" : null,
                        diagnostic.getComment());
                assertEquals("submit order", diagnostic.getAuditReason());
            }
        }
        assertEquals(List.of("submit order", "prepare shipment"),
                GraphTraceSqliteTest.reasons(fixture.audit.stream().filter(event -> event.entityType().equals("Shipment"))
                        .findFirst().orElseThrow().traceChain()));
    }

    private static void assertRequestError(RequestIntentException error) {
        assertEquals("REQUEST_COMMENT_REQUIRED", error.getCode());
        assertEquals("comment", error.getField());
        assertEquals("mutation", error.getRequestKind());
    }
}
