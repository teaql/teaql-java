package io.teaql.runtime.secret;

import io.teaql.core.http.HttpRequestFailure;
import io.teaql.core.http.HttpRequestService;
import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretProvider;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import io.teaql.runtime.DefaultUserContext;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class VaultKv2SecretProviderTest {
    private static final SecretReference REFERENCE = new SecretReference("payments", "api_key");
    private static final SecretReference TOKEN_REFERENCE = new SecretReference("bootstrap", "vault_token");
    private static final String BODY = "{\"data\":{\"data\":{\"value\":\"PRIVATE-VAULT-VALUE\"},"
            + "\"metadata\":{\"version\":7,\"destroyed\":false,\"deletion_time\":\"\"}}}";
    private static final SecretProvider TOKEN = (context, reference) ->
            new SecretValue("PRIVATE-VAULT-TOKEN".getBytes(StandardCharsets.UTF_8), null, null);

    private VaultKv2SecretProvider provider() {
        return VaultKv2SecretProvider.builder("vault", "secret", TOKEN, TOKEN_REFERENCE)
                .bind(REFERENCE, "payments/api", "value").build();
    }

    private DefaultUserContext context(VaultKv2SecretProvider provider, HttpRequestService http) {
        DefaultUserContext context = new DefaultUserContext(null).withSecretProvider(provider);
        context.putAttribute(HttpRequestService.class.getName(), new HttpRequestService() {
            @Override public boolean profileIsEncryptedOrLoopback(String profile) {
                return "vault".equals(profile);
            }
            @Override public Response execute(io.teaql.core.UserContext caller, Call call) {
                return http.execute(caller, call);
            }
        });
        return context;
    }

    @Test public void insecureProfileCannotResolveBootstrapTokenOrSendRequest() {
        AtomicInteger tokenResolutions = new AtomicInteger();
        AtomicInteger httpCalls = new AtomicInteger();
        SecretProvider token = (caller, reference) -> {
            tokenResolutions.incrementAndGet();
            return new SecretValue("PRIVATE-VAULT-TOKEN".getBytes(StandardCharsets.UTF_8), null, null);
        };
        VaultKv2SecretProvider provider = VaultKv2SecretProvider.builder(
                "vault", "secret", token, TOKEN_REFERENCE)
                .bind(REFERENCE, "payments/api", "value").build();
        DefaultUserContext context = new DefaultUserContext(null).withSecretProvider(provider);
        context.putAttribute(HttpRequestService.class.getName(), (HttpRequestService) (caller, call) -> {
            httpCalls.incrementAndGet();
            return new HttpRequestService.Response(200, BODY.getBytes(StandardCharsets.UTF_8), 1);
        });
        assertCode(SecretFailure.Code.UNAVAILABLE,
                () -> context.resolveSecret(REFERENCE, "read payment credential"));
        assertEquals(0, tokenResolutions.get());
        assertEquals(0, httpCalls.get());
    }

    @Test public void contextResolutionUsesBoundPathAndMasksDiagnostics() {
        AtomicReference<HttpRequestService.Call> captured = new AtomicReference<>();
        HttpRequestService http = (context, call) -> {
            captured.set(call);
            return new HttpRequestService.Response(200, BODY.getBytes(StandardCharsets.UTF_8), 1);
        };
        DefaultUserContext context = context(provider(), http);
        SecretValue value = context.resolveSecret(REFERENCE, "read payment credential");
        assertEquals("PRIVATE-VAULT-VALUE", new String(value.exposeBytes(), StandardCharsets.UTF_8));
        assertEquals("vault-kv2:7", value.version());
        assertFalse(value.toString().contains("PRIVATE-VAULT-VALUE"));
        HttpRequestService.Call call = captured.get();
        assertEquals("vault", call.profile());
        assertEquals("v1/secret/data/payments/api", call.relativePath());
        assertEquals("PRIVATE-VAULT-TOKEN", call.headers().get("X-Vault-Token"));
        assertEquals(HttpRequestService.RetryMode.NONE, call.retryMode());
        assertFalse(call.toString().contains("PRIVATE-VAULT-TOKEN"));
        assertCode(SecretFailure.Code.UNKNOWN_BINDING,
                () -> context.resolveSecret(new SecretReference("payments", "unknown"), "read"));
    }

    @Test public void bootstrapCredentialRotationIsReadOnEveryCallAndExpiryFailsBeforeTransport() {
        AtomicReference<String> currentToken = new AtomicReference<>("PRIVATE-VAULT-TOKEN-A");
        AtomicInteger tokenResolutions = new AtomicInteger();
        AtomicInteger httpCalls = new AtomicInteger();
        SecretProvider rotatingToken = (caller, reference) -> {
            tokenResolutions.incrementAndGet();
            if ("expired".equals(currentToken.get())) {
                throw new SecretFailure(SecretFailure.Code.EXPIRED);
            }
            return new SecretValue(currentToken.get().getBytes(StandardCharsets.UTF_8), null, null);
        };
        VaultKv2SecretProvider provider = VaultKv2SecretProvider.builder(
                "vault", "secret", rotatingToken, TOKEN_REFERENCE)
                .bind(REFERENCE, "payments/api", "value").build();
        HttpRequestService http = (caller, call) -> {
            httpCalls.incrementAndGet();
            String token = call.headers().get("X-Vault-Token");
            String body = switch (token) {
                case "PRIVATE-VAULT-TOKEN-A" -> BODY;
                case "PRIVATE-VAULT-TOKEN-B" -> BODY.replace("\"version\":7", "\"version\":8")
                        .replace("PRIVATE-VAULT-VALUE", "ROTATED-VAULT-VALUE");
                default -> throw new AssertionError("unexpected bootstrap token");
            };
            return new HttpRequestService.Response(200, body.getBytes(StandardCharsets.UTF_8), 1);
        };
        DefaultUserContext context = context(provider, http);
        assertEquals("vault-kv2:7", context.resolveSecret(REFERENCE, "read before rotation").version());
        currentToken.set("PRIVATE-VAULT-TOKEN-B");
        SecretValue rotated = context.resolveSecret(REFERENCE, "read after rotation");
        assertEquals("vault-kv2:8", rotated.version());
        assertEquals("ROTATED-VAULT-VALUE",
                new String(rotated.exposeBytes(), StandardCharsets.UTF_8));
        assertEquals(2, tokenResolutions.get());
        assertEquals(2, httpCalls.get());

        currentToken.set("expired");
        assertCode(SecretFailure.Code.EXPIRED,
                () -> context.resolveSecret(REFERENCE, "reject expired bootstrap token"));
        assertEquals(3, tokenResolutions.get());
        assertEquals("expired token must not reach HTTP", 2, httpCalls.get());
    }

    @Test public void sharedProviderKeepsConcurrentContextHttpRequestsIsolated() throws Exception {
        SecretReference alpha = new SecretReference("payments", "alpha");
        SecretReference beta = new SecretReference("payments", "beta");
        SecretProvider perContextToken = (caller, reference) -> {
            String actor = (String) caller.getAttribute("test.actor");
            String token = "alpha".equals(actor) ? "PRIVATE-VAULT-TOKEN-A"
                    : "PRIVATE-VAULT-TOKEN-B";
            return new SecretValue(token.getBytes(StandardCharsets.UTF_8), null, null);
        };
        VaultKv2SecretProvider sharedProvider = VaultKv2SecretProvider.builder(
                "vault", "secret", perContextToken, TOKEN_REFERENCE)
                .bind(alpha, "payments/alpha", "value")
                .bind(beta, "payments/beta", "value")
                .build();
        CountDownLatch bothInHttp = new CountDownLatch(2);
        ConcurrentLinkedQueue<String> observed = new ConcurrentLinkedQueue<>();
        HttpRequestService http = (caller, call) -> {
            String actor = (String) caller.getAttribute("test.actor");
            String suffix = "alpha".equals(actor) ? "A" : "B";
            assertEquals("v1/secret/data/payments/" + actor, call.relativePath());
            assertEquals("PRIVATE-VAULT-TOKEN-" + suffix,
                    call.headers().get("X-Vault-Token"));
            bothInHttp.countDown();
            try {
                assertTrue("both Context requests must overlap",
                        bothInHttp.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
            assertEquals("v1/secret/data/payments/" + actor, call.relativePath());
            assertEquals("PRIVATE-VAULT-TOKEN-" + suffix,
                    call.headers().get("X-Vault-Token"));
            observed.add(actor + ":" + suffix);
            String body = BODY.replace("PRIVATE-VAULT-VALUE", "VALUE-" + suffix);
            return new HttpRequestService.Response(200,
                    body.getBytes(StandardCharsets.UTF_8), 1);
        };
        DefaultUserContext contextA = context(sharedProvider, http);
        DefaultUserContext contextB = context(sharedProvider, http);
        contextA.putAttribute("test.actor", "alpha");
        contextB.putAttribute("test.actor", "beta");

        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<SecretValue> first = workers.submit(
                    () -> contextA.resolveSecret(alpha, "read alpha credential"));
            Future<SecretValue> second = workers.submit(
                    () -> contextB.resolveSecret(beta, "read beta credential"));
            assertEquals("VALUE-A", new String(first.get(10, TimeUnit.SECONDS).exposeBytes(),
                    StandardCharsets.UTF_8));
            assertEquals("VALUE-B", new String(second.get(10, TimeUnit.SECONDS).exposeBytes(),
                    StandardCharsets.UTF_8));
        } finally {
            workers.shutdownNow();
        }
        assertEquals(2, observed.size());
        assertTrue(observed.contains("alpha:A"));
        assertTrue(observed.contains("beta:B"));
    }

    @Test public void failuresAreSafeAndDistinct() {
        for (int status : new int[] {403, 404, 500}) {
            HttpRequestService http = (context, call) -> new HttpRequestService.Response(
                    status, "PRIVATE-VAULT-VALUE".getBytes(StandardCharsets.UTF_8), 1);
            SecretFailure.Code expected = status == 403 ? SecretFailure.Code.DENIED
                    : status == 404 ? SecretFailure.Code.MISSING : SecretFailure.Code.UNAVAILABLE;
            assertCode(expected, () -> context(provider(), http).resolveSecret(REFERENCE, "read"));
        }
        HttpRequestService invalid = (context, call) -> new HttpRequestService.Response(200,
                "PRIVATE-VAULT-VALUE".getBytes(StandardCharsets.UTF_8), 1);
        assertCode(SecretFailure.Code.UNAVAILABLE,
                () -> context(provider(), invalid).resolveSecret(REFERENCE, "read"));
        HttpRequestService timeout = (context, call) -> {
            throw new HttpRequestFailure(HttpRequestFailure.Code.TIMEOUT);
        };
        assertCode(SecretFailure.Code.UNAVAILABLE,
                () -> context(provider(), timeout).resolveSecret(REFERENCE, "read"));
        String deleted = BODY.replace("\"deletion_time\":\"\"",
                "\"deletion_time\":\"2020-01-01T00:00:00Z\"");
        HttpRequestService expired = (context, call) -> new HttpRequestService.Response(200,
                deleted.getBytes(StandardCharsets.UTF_8), 1);
        assertCode(SecretFailure.Code.EXPIRED,
                () -> context(provider(), expired).resolveSecret(REFERENCE, "read"));
        String destroyedBody = BODY.replace("\"destroyed\":false", "\"destroyed\":true");
        HttpRequestService destroyed = (context, call) -> new HttpRequestService.Response(200,
                destroyedBody.getBytes(StandardCharsets.UTF_8), 1);
        assertCode(SecretFailure.Code.MISSING,
                () -> context(provider(), destroyed).resolveSecret(REFERENCE, "read"));
    }

    @Test public void untrustedPathsAreRejectedAtBindingTime() {
        for (String path : new String[] {"../key", "/key", "key?version=1", "key%2Fother",
                "key\\other", "a//b"}) {
            assertCode(SecretFailure.Code.INVALID_INPUT,
                    () -> VaultKv2SecretProvider.builder("vault", "secret", TOKEN, TOKEN_REFERENCE)
                            .bind(REFERENCE, path, "value"));
        }
    }

    private static void assertCode(SecretFailure.Code expected, Runnable action) {
        SecretFailure failure = assertThrows(SecretFailure.class, action::run);
        assertEquals(expected, failure.code());
        assertNull(failure.getCause());
        assertFalse(failure.toString().contains("PRIVATE-VAULT"));
    }
}
