package io.teaql.examples.secret;

import io.teaql.core.http.HttpRequestService;
import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.secret.VaultKv2SecretProvider;
import io.teaql.tools.http.HttpServiceProfile;
import io.teaql.tools.http.JdkHttpRequestService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class VaultKv2ExampleTest {
    private static final SecretReference REFERENCE = new SecretReference("payment", "api_key");
    private static final byte[] BODY = ("{\"data\":{\"data\":{\"value\":\"PRIVATE-VAULT-VALUE\"},"
            + "\"metadata\":{\"version\":7,\"destroyed\":false,\"deletion_time\":\"\"}}}")
            .getBytes(StandardCharsets.UTF_8);

    private DefaultUserContext context(int port, Duration timeout) {
        var profile = HttpServiceProfile.builder("vault", URI.create("http://127.0.0.1:" + port + "/"))
                .budgets(timeout, timeout, timeout).maxResponseBytes(64 * 1024).build();
        var provider = VaultKv2SecretProvider.builder("vault", "secret",
                (caller, reference) -> new SecretValue(
                        "PRIVATE-VAULT-TOKEN".getBytes(StandardCharsets.UTF_8), null, null),
                new SecretReference("bootstrap", "vault_token"))
                .bind(REFERENCE, "payment/api", "value").build();
        DefaultUserContext context = new DefaultUserContext(null).withSecretProvider(provider);
        context.putAttribute(HttpRequestService.class.getName(),
                new JdkHttpRequestService(List.of(profile)));
        return context;
    }

    @Test public void loopbackVaultPathTokenVersionAndTimeout() throws Exception {
        var successExecutor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(3000);
            var served = successExecutor.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(2000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.US_ASCII));
                    assertTrue(reader.readLine().startsWith("GET /v1/secret/data/payment/api HTTP/1.1"));
                    boolean tokenSeen = false;
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        if (line.toLowerCase(java.util.Locale.ROOT).startsWith("x-vault-token:")) {
                            tokenSeen = line.trim().endsWith("PRIVATE-VAULT-TOKEN");
                        }
                    }
                    assertTrue(tokenSeen);
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: "
                            + BODY.length + "\r\nConnection: close\r\n\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(BODY);
                }
                return null;
            });
            SecretValue value = context(server.getLocalPort(), Duration.ofSeconds(2))
                    .resolveSecret(REFERENCE, "read configured credential");
            assertEquals("PRIVATE-VAULT-VALUE", new String(value.exposeBytes(), StandardCharsets.UTF_8));
            assertEquals("vault-kv2:7", value.version());
            assertFalse(value.toString().contains("PRIVATE-VAULT-VALUE"));
            served.get(3, TimeUnit.SECONDS);
        } finally {
            successExecutor.shutdownNow();
        }

        var slowExecutor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(3000);
            var served = slowExecutor.submit(() -> {
                try (Socket ignored = server.accept()) {
                    Thread.sleep(250);
                }
                return null;
            });
            SecretFailure failure = assertThrows(SecretFailure.class,
                    () -> context(server.getLocalPort(), Duration.ofMillis(50))
                            .resolveSecret(REFERENCE, "read slow credential"));
            assertEquals(SecretFailure.Code.UNAVAILABLE, failure.code());
            assertNull(failure.getCause());
            assertFalse(failure.toString().contains("PRIVATE-VAULT"));
            served.get(3, TimeUnit.SECONDS);
        } finally {
            slowExecutor.shutdownNow();
        }
    }
}
