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
}
