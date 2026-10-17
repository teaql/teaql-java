package io.teaql.core.http;

import io.teaql.core.TraceKind;
import io.teaql.core.TraceNode;
import io.teaql.core.QueryIntent;
import io.teaql.core.QueryRequest;
import io.teaql.core.QueryRequestScope;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpCallTraceTest {
    @Test public void httpBusinessTraceIsAnImmutableRequestChild() throws Exception {
        var parent = new ArrayList<>(List.of(
                new TraceNode(TraceKind.PURPOSE, "School", "render school"),
                new TraceNode(TraceKind.COMMENT, "School", "load school")));
        var first = HttpRequestService.Call.get("orders", "orders/1", "check inventory", "contact inventory")
                .withParentTraceChain(parent);
        var second = HttpRequestService.Call.get("orders", "orders/2", "check payment", "contact payment")
                .withParentTraceChain(parent);
        parent.set(1, new TraceNode(TraceKind.COMMENT, "School", "changed later"));
        assertEquals(List.of(
                new TraceNode(TraceKind.PURPOSE, "School", "render school"),
                new TraceNode(TraceKind.COMMENT, "School", "load school"),
                new TraceNode(TraceKind.COMMENT, "orders", "check inventory")),
                first.businessTraceChain());
        assertEquals("check payment", second.businessTraceChain().get(2).getComment());
        assertEquals(List.of("PURPOSE", "COMMENT", "COMMENT"),
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(first.businessTraceProjection()).findValuesAsText("kind"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.parentTraceChain().add(new TraceNode("mutate")));
        var direct = HttpRequestService.Call.get("orders", "orders/3", "load order", "render order");
        assertEquals(TraceKind.PURPOSE, direct.businessTraceChain().get(0).getKind());
        assertEquals("load order", direct.businessTraceChain().get(1).getComment());
        assertFalse(first.toString().contains("render school"));
        QueryRequest request = new QueryRequest() {
            private final QueryIntent intent = QueryIntent.of("load order graph", "render order details");
            @Override public QueryIntent intent() { return intent; }
            @Override public QueryRequestScope requestScope() {
                return QueryRequestScope.root("Order", intent);
            }
        };
        var fromRequest = direct.withParentRequest(request);
        assertEquals("render order details", fromRequest.businessTraceChain().get(0).getComment());
        assertEquals("load order", fromRequest.businessTraceChain().get(2).getComment());
    }

    @Test public void invalidParentNodesCannotBecomeBusinessTrace() {
        var call = HttpRequestService.Call.get("orders", "orders/1", "check", "render");
        for (var parent : List.of(
                List.of(new TraceNode(TraceKind.SQL, "sqlite", "select")),
                List.of(new TraceNode(TraceKind.PURPOSE, "School", 7L, "render")),
                List.of(new TraceNode(TraceKind.PURPOSE, "School", " ")))) {
            assertEquals(HttpRequestFailure.Code.INVALID_INPUT,
                    assertThrows(HttpRequestFailure.class,
                            () -> call.withParentTraceChain(parent)).code());
        }
    }
}
