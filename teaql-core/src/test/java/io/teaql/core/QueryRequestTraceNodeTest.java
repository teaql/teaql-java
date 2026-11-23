package io.teaql.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueryRequestTraceNodeTest {
    private static QueryRequestScope root() {
        return QueryRequestScope.root("School", QueryIntent.of("Load schools", "Render the dashboard"));
    }

    @Test public void oneRequestUsesExistingChainWithoutDuplicateTree() {
        var root = root();
        assertTrue(QueryRequestTraceNode.fromScopes(List.of()).isEmpty());
        assertTrue(QueryRequestTraceNode.fromScopes(List.of(root)).isEmpty());
        assertTrue(QueryRequestTraceNode.fromScopes(List.of(root, root)).isEmpty());
    }

    @Test public void identicalSiblingCommentsDoNotInventAncestry() {
        var root = root();
        var left = root.deriveChild("Kind", "Same local comment");
        var right = root.deriveChild("Kind", "Same local comment");
        var nodes = QueryRequestTraceNode.fromScopes(List.of(left, right, root, left));
        assertEquals(4, nodes.size());
        assertEquals("purpose", nodes.get(0).kind());
        assertNull(nodes.get(0).parentId());
        assertEquals(Integer.valueOf(1), nodes.get(2).parentId());
        assertEquals(Integer.valueOf(1), nodes.get(3).parentId());
        assertEquals(nodes.get(2).detail(), nodes.get(3).detail());
        for (int index = 0; index < nodes.size(); index++) {
            assertEquals(index, nodes.get(index).id());
            assertTrue(nodes.get(index).parentId() == null || nodes.get(index).parentId() < index);
        }
        assertThrows(UnsupportedOperationException.class, () -> nodes.clear());
    }

    @Test public void nativeOperationAndLocalCommentRemainSeparate() throws Exception {
        var root = root();
        var operation = root.deriveOperation("Kind", QueryOperation.nativeMethod(
                QueryOperation.Kind.AGGREGATE, "rollUpToKind", null, "kind"));
        var child = operation.deriveChild("Kind", "Group visible types");
        var nodes = QueryRequestTraceNode.fromScopes(List.of(child, root));
        var json = new ObjectMapper().valueToTree(nodes);
        assertTrue(json.get(0).get("parentId").isNull());
        assertEquals("rollUpToKind", json.get(2).get("method").asText());
        assertEquals("rollUpToKind()", json.get(2).get("detail").asText());
        assertEquals(2, json.get(3).get("parentId").asInt());
        assertEquals("Group visible types", json.get(3).get("detail").asText());
        assertFalse(json.get(3).has("method"));
    }

    @Test public void independentEqualRootsAreNotMergedByContent() {
        var nodes = QueryRequestTraceNode.fromScopes(List.of(root(), root()));
        assertEquals(4, nodes.size());
        assertNull(nodes.get(0).parentId());
        assertNull(nodes.get(2).parentId());
        assertEquals(Integer.valueOf(2), nodes.get(3).parentId());
    }
}
