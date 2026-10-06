package io.teaql.core;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SqlExecutionTraceTest {
    static final class Request extends BaseRequest<BaseEntity> {
        List<TraceNode> source = List.of();
        Request() { super(BaseEntity.class); }
        @Override public String getTypeName() { return "CustomerOrder"; }
        @Override public List<TraceNode> sqlTraceSource() { return source; }
    }

    @Test public void rootQueryCapturesItsOwnIntentNotLaterRequestChanges() {
        var request = new Request();
        request.comment = "first request";
        request.purpose = "verify ownership";
        var first = SqlExecutionTrace.query(request);
        request.comment = "second request";
        var second = SqlExecutionTrace.query(request);
        assertEquals("first request", first.source().get(0).getComment());
        assertEquals("second request", second.source().get(0).getComment());
        assertEquals("CustomerOrder", first.source().get(0).getName());
        assertEquals("select", first.operation());
        assertTrue(first.mutationLineage().isEmpty());
    }

    @Test public void derivedStatementFreezesCompleteOrderedSource() {
        var request = new Request();
        request.source = new ArrayList<>(List.of(
                new TraceNode(TraceKind.COMMENT, "PaymentAttempt", "load payment context"),
                new TraceNode(TraceKind.PURPOSE, "PaymentAttempt", "render details"),
                new TraceNode(TraceKind.RELATION, "payment", "PaymentAttempt.payment"),
                new TraceNode(TraceKind.RELATION, "customerOrder", "Payment.customerOrder")));
        var captured = SqlExecutionTrace.query(request);
        request.source.clear();
        assertEquals(4, captured.source().size());
        assertThrows(UnsupportedOperationException.class, () -> captured.source().clear());
        var canonical = SqlTracePath.canonical(captured.source(), "sqlite", captured.operation());
        assertEquals("PaymentAttempt", canonical.path().get(0).getName());
        assertEquals("payment", canonical.path().get(2).getName());
        assertEquals("customerOrder", canonical.path().get(3).getName());
    }

    @Test public void mutationStatementOwnsRootIntentWithAndWithoutAnObserver() {
        var entity = new BaseEntity();
        entity.__internalSet("id", 201L);
        var lineage = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", 100L, "submit order"),
                new TraceNode(TraceKind.AUDIT_REASON, "Payment", 201L, "authorize payment"));
        var intent = MutationIntent.of("submit order");
        // Standalone canonical vectors remain last-intent-wins. An executed
        // mutation gets its root reason from its validated request, not this fold.
        assertEquals("authorize payment", SqlTracePath.canonical(lineage, "sqlite", "insert").auditReason());
        for (String operation : List.of("insert", "update", "delete", "recover")) {
            for (boolean observing : List.of(false, true)) {
                var captured = new ArrayList<ExecutionMetadata>();
                var trace = SqlExecutionTrace.mutation(entity, lineage, operation, intent);
                if (observing) trace = trace.collecting(captured::add);
                assertSame(intent, trace.mutationIntent());
                var metadata = new ExecutionMetadata();
                metadata.setBackend("sqlite");
                trace.applyTo(metadata);
                trace.recordStatement(metadata);
                assertEquals("submit order", metadata.getAuditReason());
                assertNull(metadata.getComment());
                assertEquals(lineage, metadata.getMutationLineage());
                assertEquals(observing ? 1 : 0, captured.size());
                assertTrue(metadata.getTraceChain().stream().noneMatch(node -> node.getKind() == TraceKind.AUDIT_REASON));
            }
        }
        assertThrows(NullPointerException.class, () -> SqlExecutionTrace.mutation(entity, lineage, "insert", null));
    }

    @Test public void mutationReadbackRetainsIndependentRootReasonAndQueryIntent() {
        var entity = new BaseEntity();
        var intent = MutationIntent.of("submit order");
        var lineage = List.of(new TraceNode(TraceKind.AUDIT_REASON, "CustomerOrder", 100L, "submit order"),
                new TraceNode(TraceKind.AUDIT_REASON, "Shipment", 301L, "prepare shipment"));
        var collected = new ArrayList<ExecutionMetadata>();
        var trace = SqlExecutionTrace.mutation(entity, lineage, "insert", intent).collecting(collected::add).readback(intent);
        var metadata = new ExecutionMetadata();
        metadata.setBackend("sqlite");
        trace.applyTo(metadata);
        trace.recordStatement(metadata);
        assertSame(intent, trace.mutationIntent());
        assertEquals("submit order", metadata.getAuditReason());
        assertEquals("submit order", metadata.getComment());
        assertEquals(intent.readbackIntent().purpose(), metadata.getPurpose());
        assertEquals(lineage, metadata.getMutationLineage());
        assertEquals(List.of(metadata), collected);
    }
}
