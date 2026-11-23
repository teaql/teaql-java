package io.teaql.core.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpCallSerializationTest {
    @Test public void structuredSerializationDoesNotRevealCredentialHeaders() throws Exception {
        String canary = "PRIVATE-VAULT-TOKEN-CANARY";
        var call = new HttpRequestService.Call(
                "vault", HttpRequestService.Method.GET, "v1/secret/data/signing",
                Map.of("X-Vault-Token", canary), null, null,
                "read configured secret", HttpRequestService.IntentKind.PURPOSE,
                "resolve runtime credential", HttpRequestService.RetryMode.NONE);

        Exception jacksonFailure = assertThrows(Exception.class,
                () -> new ObjectMapper().writeValueAsString(call));
        assertFalse(jacksonFailure.toString().contains(canary));
        assertEquals("{}", new Gson().toJson(call));
        assertFalse(call.toString().contains(canary));

        var response = new HttpRequestService.Response(200,
                ("{\"secret\":\"" + canary + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8), 1);
        Exception responseFailure = assertThrows(Exception.class,
                () -> new ObjectMapper().writeValueAsString(response));
        assertFalse(responseFailure.toString().contains(canary));
        assertEquals("{}", new Gson().toJson(response));
        assertFalse(response.toString().contains(canary));
    }
}
