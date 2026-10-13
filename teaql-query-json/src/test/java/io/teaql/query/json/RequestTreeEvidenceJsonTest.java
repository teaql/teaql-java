package io.teaql.query.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Evidence JSON uses business traceChain and explicitly named physical tracePath. */
public class RequestTreeEvidenceJsonTest {
    @Test public void legacyPhysicalGetterCannotReplaceBusinessTraceChainOnTheWire() throws Exception {
        ExecutionMetadata metadata = new ExecutionMetadata();
        metadata.setTraceChain(List.of(new TraceNode(TraceKind.PROVIDER, "sqlite", "")));
        metadata.setRequestTraceChain(List.of(new TraceNode(TraceKind.PURPOSE, "School", "render schools"),
                new TraceNode(TraceKind.COMMENT, "School", "load schools")));
        var mapper = new ObjectMapper();
        var tree = mapper.valueToTree(metadata);
        assertEquals("PURPOSE", tree.path("traceChain").get(0).path("kind").asText());
        assertEquals("PROVIDER", tree.path("tracePath").get(0).path("kind").asText());
        assertFalse(tree.has("requestTraceChain"));
        assertFalse(tree.has("requestScope"));
    }
}
