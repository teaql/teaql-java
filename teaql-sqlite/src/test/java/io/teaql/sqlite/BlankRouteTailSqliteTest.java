package io.teaql.sqlite;

import io.teaql.core.*;
import io.teaql.runtime.AppAuditEventSink;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** TC-REQ-13 deliberate native diagnostic input, not generated graph proof. */
public class BlankRouteTailSqliteTest {
    @Test public void explicitRootCommentSurvivesEveryBlankTypedTailAtRealSinks() throws Exception {
        for (boolean logging : new boolean[]{false, true}) {
            for (TraceKind kind : List.of(TraceKind.ENTITY, TraceKind.PROVIDER, TraceKind.SQL)) {
                var fixture = new GraphTraceSqliteTest.Fixture();
                fixture.queryLogging = logging;
                fixture.mutationLogging = logging;
                String comment = "  explicit owned mutation reason  ";
                var entity = fixture.create("CustomerOrder", 810, "native route fixture");
                String name = kind == TraceKind.PROVIDER ? "sqlite"
                        : kind == TraceKind.SQL ? "insert" : "CustomerOrder";
                var tail = new TraceNode(kind, name, null, "");
                var source = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", 810L, comment), tail);
                entity.setTraceChain(source);
                entity.setComment(comment);
                assertEquals("", tail.getComment());
                // The sink reads through an independent connection. Pending
                // rows inside the mutation transaction cannot satisfy this probe.
                fixture.context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> {
                    try (var connection = fixture.dataSource.getConnection();
                            var select = connection.prepareStatement("SELECT id, version FROM customer_order_data WHERE id = ?")) {
                        assertTrue(connection.getAutoCommit());
                        select.setLong(1, 810);
                        try (var row = select.executeQuery()) {
                            assertTrue("audit escaped before real commit", row.next());
                            assertEquals(810, row.getLong(1));
                            assertEquals(1, row.getLong(2));
                            assertFalse(row.next());
                        }
                    } catch (java.sql.SQLException error) {
                        throw new AssertionError("independent committed-row probe failed", error);
                    }
                    assertEquals(comment, event.traceChain().get(0).getComment());
                    fixture.audit.add(event);
                });
                entity.auditAs(comment).save(fixture.context);
                assertEquals(1, fixture.commands.size());
                var emitted = fixture.commands.get(0);
                assertEquals(source, emitted.getTraceChain());
                assertEquals(tail, emitted.getTraceChain().get(emitted.getTraceChain().size() - 1));
                assertEquals(comment, emitted.comment());
                assertEquals(comment, emitted.intent().readbackIntent().comment());
                assertEquals(1, fixture.results.size());
                var statements = fixture.results.get(0).statements();
                assertEquals(2, statements.size());
                for (int index = 0; index < statements.size(); index++) {
                    var statement = statements.get(index);
                    assertEquals(comment, statement.getAuditReason());
                    assertEquals(index == 0 ? null : comment, statement.getComment());
                    assertEquals(source, statement.getMutationLineage());
                    assertEquals("success", statement.getExecutionOutcome());
                    assertEquals(index == 0 ? "insert" : "select", statement.getStatementOperation());
                    var route = statement.getTraceChain();
                    assertEquals("CustomerOrder", route.get(0).getName());
                    assertEquals("sqlite", route.get(route.size() - 2).getName());
                    assertEquals(statement.getStatementOperation(), route.get(route.size() - 1).getName());
                }
                assertEquals(1, fixture.audit.size());
                assertEquals(source, fixture.audit.get(0).traceChain());
                assertEquals(logging ? 2 : 0, fixture.sql.size());
                for (var diagnostic : fixture.sql) assertEquals(comment, diagnostic.getAuditReason());
                assertEquals(comment, source.get(0).getComment());
                assertTrue(fixture.context.getTraceChain().isEmpty());
            }
        }
    }
}
