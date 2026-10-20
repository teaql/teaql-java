package io.teaql.examples.secret;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import com.sun.net.httpserver.HttpServer;
import io.teaql.core.http.HttpRequestService;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import io.teaql.opentelemetry.StandardOpenTelemetryFactory;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.secret.VaultKv2SecretProvider;
import io.teaql.tools.http.HttpServiceProfile;
import io.teaql.tools.http.JdkHttpRequestService;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/** Opt-in Vault -> standard OTLP probe; backend readback is checked by conformance. */
public class SecretOpenObserveProbeTest {
    private static final String TOKEN = "PRIVATE_VAULT_TOKEN_CANARY";
    private static final String VALUE = "PRIVATE_VAULT_VALUE_CANARY";

    @Test public void exportedVaultTraceDoesNotContainCredentialOrResponseValue() throws Exception {
        assumeTrue("true".equals(System.getenv("TEAQL_SECRET_LIVE_OTLP")));
        URI endpoint = URI.create(System.getenv("TEAQL_TELEMETRY_OTLP_ENDPOINT"));
        assertEquals("127.0.0.1", endpoint.getHost());
        String serviceName = System.getenv("TEAQL_TELEMETRY_SERVICE_NAME");
        assertNotNull(serviceName);
        String outcome = System.getenv().getOrDefault("TEAQL_SECRET_PROBE_OUTCOME", "success");
        assertTrue("unsupported probe outcome", List.of("success", "denied", "missing",
                "malformed", "expired", "timeout").contains(outcome));
        var telemetry = new StandardOpenTelemetryFactory().create(serviceName, endpoint);
        var carrier = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        server.createContext("/v1/secret/data/payment/api", exchange -> {
            assertEquals(TOKEN, exchange.getRequestHeaders().getFirst("X-Vault-Token"));
            carrier.set(exchange.getRequestHeaders().getFirst("traceparent"));
            if (outcome.equals("timeout")) {
                try {
                    Thread.sleep(1800);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
                return;
            }
            String response = switch (outcome) {
                case "denied", "missing" -> "{\"errors\":[\"" + VALUE + "\"]}";
                case "malformed" -> "{\"data\":\"" + VALUE + "\",";
                case "expired" -> "{\"data\":{\"data\":{\"value\":\"" + VALUE
                        + "\"},\"metadata\":{\"version\":7,\"destroyed\":false,\"deletion_time\":\"2020-01-01T00:00:00Z\"}}}";
                default -> "{\"data\":{\"data\":{\"value\":\"" + VALUE
                        + "\"},\"metadata\":{\"version\":7,\"destroyed\":false,\"deletion_time\":\"\"}}}";
            };
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(outcome.equals("denied") ? 403 : outcome.equals("missing") ? 404 : 200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                    .telemetry(telemetry).build();
            var reference = new SecretReference("payment", "api_key");
            var provider = VaultKv2SecretProvider.builder("vault", "secret",
                    (caller, ignored) -> new SecretValue(TOKEN.getBytes(StandardCharsets.UTF_8), null, null),
                    new SecretReference("bootstrap", "vault_token"))
                    .bind(reference, "payment/api", "value").build();
            var context = new DefaultUserContext(runtime).withSecretProvider(provider);
            var profileBuilder = HttpServiceProfile.builder("vault",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
            if (outcome.equals("timeout")) {
                profileBuilder.budgets(Duration.ofSeconds(2), Duration.ofMillis(700),
                        Duration.ofSeconds(3));
            }
            var profile = profileBuilder.build();
            context.putAttribute(HttpRequestService.class.getName(),
                    new JdkHttpRequestService(List.of(profile)));
            if (outcome.equals("success")) {
                assertEquals(VALUE, new String(context.resolveSecret(reference, "verify Vault adapter")
                        .exposeBytes(), StandardCharsets.UTF_8));
            } else {
                SecretFailure.Code expected = switch (outcome) {
                    case "denied" -> SecretFailure.Code.DENIED;
                    case "missing" -> SecretFailure.Code.MISSING;
                    case "expired" -> SecretFailure.Code.EXPIRED;
                    default -> SecretFailure.Code.UNAVAILABLE;
                };
                assertEquals(expected, assertThrows(SecretFailure.class,
                        () -> context.resolveSecret(reference, "verify Vault adapter")).code());
            }
            assertNotNull(carrier.get());
            assertTrue(carrier.get().startsWith("00-") && carrier.get().length() == 55);
            telemetry.flush();
            System.out.println("SECRET_OTLP_PROBE_PASS outcome=" + outcome
                    + " traceparent=" + carrier.get());
        } finally {
            server.stop(0);
            telemetry.shutdown();
        }
    }
}
