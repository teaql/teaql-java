package io.teaql.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** #202: exact shared pure-algorithm vectors; not generated relation-loading evidence. */
@RunWith(Parameterized.class)
public class SqlTracePathVectorsTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> cases() throws Exception {
        try (var input = SqlTracePathVectorsTest.class.getResourceAsStream("/sql-trace-path-v1.json")) {
            assertNotNull(input);
            byte[] bytes = input.readAllBytes();
            assertEquals("7cb67eb1fd08a723611e9f8fad121e060a17546c3b01640052346498d5a6b0b7",
                    java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
            var document = new ObjectMapper().readTree(bytes);
            assertEquals("teaql.sql-trace-path.v1", document.path("contract").asText());
            var result = new ArrayList<Object[]>();
            for (var item : document.path("cases")) result.add(new Object[]{item.path("id").asText(), item});
            assertEquals(12, result.size());
            return result;
        }
    }

    private final JsonNode vector;
    public SqlTracePathVectorsTest(String id, JsonNode vector) { this.vector = vector; }

    private static String text(JsonNode input, String name) {
        var value = input.get(name);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static List<TraceNode> nodes(JsonNode values) {
        var result = new ArrayList<TraceNode>();
        for (var value : values) {
            String kind = text(value, "kind").replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(java.util.Locale.ROOT);
            Long id = value.path("entityId").isNull() ? null : value.path("entityId").asLong();
            result.add(new TraceNode(TraceKind.valueOf(kind), text(value, "name"), id, text(value, "detail")));
        }
        return result;
    }

    @Test public void sharedCanonicalPathAndLastIntentWins() {
        String backend = text(vector, "backend"), operation = text(vector, "operation");
        var result = SqlTracePath.canonical(nodes(vector.path("source")), backend, operation);
        assertEquals(nodes(vector.path("expectedPath")), result.path());
        var intent = vector.path("expectedIntent");
        assertEquals(text(intent, "comment"), result.comment());
        assertEquals(text(intent, "purpose"), result.purpose());
        assertEquals(text(intent, "auditReason"), result.auditReason());
        assertEquals(result.path(), SqlTracePath.canonical(result.path(), "ignored-new-backend", operation).path());
        assertThrows(UnsupportedOperationException.class, () -> result.path().clear());
    }
}
