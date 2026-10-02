package io.teaql.core.sql.portable;

import io.teaql.core.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** #202: derived request factories inherit their explicit parent, never ambient Context. */
public class SqlDiagnosticRequestTest {
    private static class Request extends BaseRequest<BaseEntity> {
        private final String type;
        Request(String type, String comment, String purpose) {
            super(BaseEntity.class, BaseEntity::new);
            this.type = type; internalComment(comment); internalPurpose(purpose);
        }
        @Override public String getTypeName() { return type; }
        void replaceComment(String comment) { internalComment(comment); }
    }

    @Test public void unscopedParentOwnsTheRootEvenWhenTheChildHasDifferentIntent() {
        var parent = new Request("SourceDocument", "inspect the document", "render details");
        var child = new Request("SourceLine", "unrelated child comment", "unrelated purpose");
        var derived = SqlDiagnosticRequest.forRelation(child, null, parent, "lines");
        assertEquals(parent.comment(), derived.comment()); assertEquals(parent.purpose(), derived.purpose());
        assertEquals(List.of(
                new TraceNode(TraceKind.COMMENT, "SourceDocument", parent.comment()),
                new TraceNode(TraceKind.PURPOSE, "SourceDocument", parent.purpose()),
                new TraceNode(TraceKind.RELATION, "lines", "SourceDocument.lines")), derived.sqlTraceSource());
        assertTrue(parent.sqlTraceSource().isEmpty()); assertTrue(child.sqlTraceSource().isEmpty());
    }

    @Test public void nonRelationWorkKeepsAnImmutableParentSnapshotWithoutAFabricatedEdge() {
        var trace = new ArrayList<>(List.of(
                new TraceNode(TraceKind.COMMENT, "SourceDocument", "inspect the document"),
                new TraceNode(TraceKind.PURPOSE, "SourceDocument", "render details"),
                new TraceNode(TraceKind.RELATION, "lines", "SourceDocument.lines")));
        var original = List.copyOf(trace);
        var intent = QueryIntent.of("inspect the document", "render details");
        var parent = new Request("SourceLine", "unused local comment", "unused local purpose") {
            @Override public List<TraceNode> sqlTraceSource() { return trace; }
            @Override public QueryIntent inheritedQueryIntent() { return intent; }
        };
        var child = new Request("Statistic", null, null);
        var derived = SqlDiagnosticRequest.forDerived(child, null, parent);
        trace.clear(); parent.replaceComment("later unrelated comment");
        assertEquals(original, derived.sqlTraceSource());
        assertSame(intent, derived.inheritedQueryIntent());
        assertEquals("inspect the document", derived.comment());
        assertThrows(UnsupportedOperationException.class, () -> derived.sqlTraceSource().clear());
    }

    @Test public void validChildIntentCannotReplaceMissingParentIntent() {
        var child = new Request("SourceLine", "valid child comment", "valid child purpose");
        for (var parent : List.of(new Request("SourceDocument", " ", "render details"),
                new Request("SourceDocument", "inspect the document", " "))) {
            assertThrows(RequestIntentException.class, () -> SqlDiagnosticRequest.forDerived(child, null, parent));
            assertThrows(RequestIntentException.class, () -> SqlDiagnosticRequest.forRelation(child, null, parent, "lines"));
        }
    }
}
