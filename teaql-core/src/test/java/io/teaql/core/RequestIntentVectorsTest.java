package io.teaql.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/** Frozen shared conformance vectors. Trace/logging/children never fill a missing root slot. */
@RunWith(Parameterized.class)
public class RequestIntentVectorsTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> cases() throws Exception {
        byte[] bytes;
        try (var input = RequestIntentVectorsTest.class.getResourceAsStream("/request-intent-v1.json")) {
            assertNotNull(input); bytes = input.readAllBytes();
        }
        assertEquals("3b911b0edb1b6634204a41c199f67f709d6408405b87d4f2f72a02110cb0b38b",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        var data = new ObjectMapper().readTree(new String(bytes, StandardCharsets.UTF_8));
        assertEquals("teaql.request-intent.v1", data.path("contract").asText());
        var result = new ArrayList<Object[]>();
        for (var item : data.path("cases")) result.add(new Object[]{item.path("id").asText(), item});
        assertEquals(20, result.size()); return result;
    }

    private final JsonNode vector;
    public RequestIntentVectorsTest(String id, JsonNode vector) { this.vector = vector; }
    private static String field(JsonNode input, String name) {
        var value = input.get(name); return value == null || value.isNull() ? null : value.asText();
    }

    @Test public void sharedRequestIntentContract() {
        var input = vector.path("input");
        String kind = vector.path("kind").asText();
        try {
            if (kind.equals("query")) {
                var intent = QueryIntent.of(field(input, "comment"), field(input, "purpose"));
                assertFalse("Expected rejection", vector.has("error"));
                assertEquals(vector.path("expected").path("comment").asText(), intent.comment());
                assertEquals(vector.path("expected").path("purpose").asText(), intent.purpose());
            } else {
                var intent = MutationIntent.of(field(input, "comment"));
                assertFalse("Expected rejection", vector.has("error"));
                assertEquals(vector.path("expected").path("comment").asText(), intent.comment());
                assertEquals(intent.comment(), intent.auditReason());
                assertEquals(intent.comment(), intent.readbackIntent().comment());
            }
        } catch (RequestIntentException error) {
            assertTrue(vector.has("error"));
            assertEquals(vector.path("error").path("code").asText(), error.getCode());
            assertEquals(vector.path("error").path("field").asText(), error.getField());
            assertEquals(kind, error.getRequestKind());
        }
    }
}
