package io.teaql.core.secret;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class SecretValueSerializationTest {
    @Test public void commonJsonSerializersDoNotExposeSecretFields() throws Exception {
        SecretValue value = new SecretValue(
                "SENSITIVE_CANARY".getBytes(StandardCharsets.UTF_8), "v1", null);

        String gsonJson = new Gson().toJson(value);
        assertEquals("{}", gsonJson);
        assertFalse(gsonJson.contains("SENSITIVE_CANARY"));

        Exception jacksonFailure = assertThrows(Exception.class,
                () -> new ObjectMapper().writeValueAsString(value));
        assertFalse(jacksonFailure.toString().contains("SENSITIVE_CANARY"));
        assertEquals("SENSITIVE_CANARY",
                new String(value.exposeBytes(), StandardCharsets.UTF_8));
        assertEquals("v1", value.version());
    }
}
