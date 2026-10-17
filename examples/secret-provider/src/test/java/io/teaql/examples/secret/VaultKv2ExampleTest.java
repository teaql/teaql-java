package io.teaql.examples.secret;

import io.teaql.core.http.HttpRequestService;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretProvider;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.RuntimeTelemetry;
import io.teaql.runtime.TeaQLRuntime;
import io.teaql.runtime.secret.BoundSecretProvider;
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
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.Test;
import org.junit.Assume;
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

    private DefaultUserContext tlsContext(int port, byte[] trustedRoot, List<String> captured) {
        SecretProvider bootstrap = (caller, reference) -> new SecretValue(
                "PRIVATE-VAULT-TOKEN".getBytes(StandardCharsets.UTF_8), null, null);
        return configuredTlsContext(URI.create("https://127.0.0.1:" + port + "/"), trustedRoot,
                bootstrap, "payment/api", captured);
    }

    private DefaultUserContext configuredTlsContext(URI endpoint, byte[] trustedRoot,
                                                    SecretProvider bootstrap, String path,
                                                    List<String> captured) {
        var builder = HttpServiceProfile.builder("vault", endpoint)
                .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8));
        if (trustedRoot != null) builder.trustedRootPem(trustedRoot);
        var provider = VaultKv2SecretProvider.builder("vault", "secret", bootstrap,
                new SecretReference("bootstrap", "vault_token"))
                .bind(REFERENCE, path, "value").build();
        RuntimeTelemetry telemetry = operation -> {
            captured.add("start:" + operation);
            return new RuntimeTelemetry.Scope() {
                @Override public void success(Map<String, Object> attributes) {
                    captured.add("success:" + attributes);
                }
                @Override public void failure(Throwable error) {
                    captured.add("failure:" + error);
                }
                @Override public void failureWithAttributes(Throwable error, Map<String, Object> attributes) {
                    captured.add("failure:" + error + ":" + attributes);
                }
            };
        };
        TeaQLRuntime runtime = TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .telemetry(telemetry).build();
        DefaultUserContext context = new DefaultUserContext(runtime).withSecretProvider(provider);
        context.putAttribute(HttpRequestService.class.getName(),
                new JdkHttpRequestService(List.of(builder.build())));
        return context;
    }

    @Test public void liveVaultKv2OverTlsChecksSuccessDeniedMissingAndUnknownCa() throws Exception {
        String endpointText = System.getenv("TEAQL_TEST_VAULT_URL");
        Assume.assumeTrue("live Vault fixture is opt-in", endpointText != null);
        URI endpoint = URI.create(endpointText);
        assertEquals("https", endpoint.getScheme());
        assertEquals("127.0.0.1", endpoint.getHost());
        byte[] root = Files.readAllBytes(java.nio.file.Path.of(
                System.getenv("TEAQL_TEST_VAULT_CA_PEM")));
        String expected = System.getenv("TEAQL_TEST_VAULT_EXPECTED_VALUE");
        String version = System.getenv("TEAQL_TEST_VAULT_EXPECTED_VERSION");
        List<String> captured = new CopyOnWriteArrayList<>();
        var tokenReference = new SecretReference("bootstrap", "vault_token");
        var token = BoundSecretProvider.builder(null)
                .environment(tokenReference, "TEAQL_TEST_VAULT_TOKEN", null, null).build();
        SecretValue value = configuredTlsContext(endpoint, root, token, "payment/api", captured)
                .resolveSecret(REFERENCE, "verify live Vault credential");
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), value.exposeBytes());
        assertEquals("vault-kv2:" + version, value.version());

        var wrongToken = BoundSecretProvider.builder(null)
                .environment(tokenReference, "TEAQL_TEST_VAULT_BAD_TOKEN", null, null).build();
        SecretFailure denied = assertThrows(SecretFailure.class,
                () -> configuredTlsContext(endpoint, root, wrongToken, "payment/api", captured)
                        .resolveSecret(REFERENCE, "verify denied Vault credential"));
        assertEquals(SecretFailure.Code.DENIED, denied.code());
        SecretFailure missing = assertThrows(SecretFailure.class,
                () -> configuredTlsContext(endpoint, root, token, "payment/absent", captured)
                        .resolveSecret(REFERENCE, "verify missing Vault credential"));
        assertEquals(SecretFailure.Code.MISSING, missing.code());
        SecretFailure untrusted = assertThrows(SecretFailure.class,
                () -> configuredTlsContext(endpoint, null, token, "payment/api", captured)
                        .resolveSecret(REFERENCE, "verify untrusted Vault certificate"));
        assertEquals(SecretFailure.Code.UNAVAILABLE, untrusted.code());
        assertNull(untrusted.getCause());

        String diagnostics = captured.toString();
        assertTrue(diagnostics.contains("success:"));
        assertTrue(diagnostics.contains("failure:"));
        assertFalse(diagnostics.contains(System.getenv("TEAQL_TEST_VAULT_TOKEN")));
        assertFalse(diagnostics.contains(System.getenv("TEAQL_TEST_VAULT_BAD_TOKEN")));
        assertFalse(diagnostics.contains(expected));
        System.out.println("VAULT_LIVE_PASS success=1 denied=1 missing=1 unknown_ca=1 diagnostics=1");
    }

    private static void openssl(String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = "openssl";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        assertEquals("local TLS fixture generation failed", 0, process.waitFor());
    }

    @Test public void trustedTlsVaultSucceedsAndUntrustedCertificateSendsNoRequest() throws Exception {
        List<String> captured = new CopyOnWriteArrayList<>();
        List<String> logs = new CopyOnWriteArrayList<>();
        Logger logger = Logger.getLogger(JdkHttpRequestService.class.getName());
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { logs.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(handler);
        var directory = Files.createTempDirectory("teaql-vault-tls-");
        var rootPath = directory.resolve("root.pem");
        var rootKeyPath = directory.resolve("root.key");
        var nextRootPath = directory.resolve("next-root.pem");
        var nextRootKeyPath = directory.resolve("next-root.key");
        var requestPath = directory.resolve("server.csr");
        var serialPath = directory.resolve("root.srl");
        var nextSerialPath = directory.resolve("next-root.srl");
        var certificatePath = directory.resolve("server.pem");
        var nextCertificatePath = directory.resolve("next-server.pem");
        var keyPath = directory.resolve("server.key");
        try {
            openssl("req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                    "-subj", "/CN=TeaQL test root", "-addext", "basicConstraints=critical,CA:TRUE",
                    "-addext", "keyUsage=critical,keyCertSign,cRLSign",
                    "-keyout", rootKeyPath.toString(), "-out", rootPath.toString());
            openssl("req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                    "-subj", "/CN=TeaQL next test root", "-addext", "basicConstraints=critical,CA:TRUE",
                    "-addext", "keyUsage=critical,keyCertSign,cRLSign",
                    "-keyout", nextRootKeyPath.toString(), "-out", nextRootPath.toString());
            openssl("req", "-new", "-newkey", "rsa:2048", "-nodes", "-subj", "/CN=127.0.0.1",
                    "-addext", "subjectAltName=IP:127.0.0.1",
                    "-keyout", keyPath.toString(), "-out", requestPath.toString());
            openssl("x509", "-req", "-days", "1", "-CAcreateserial", "-copy_extensions", "copy",
                    "-in", requestPath.toString(), "-CA", rootPath.toString(),
                    "-CAkey", rootKeyPath.toString(), "-out", certificatePath.toString());
            openssl("x509", "-req", "-days", "1", "-CAcreateserial", "-copy_extensions", "copy",
                    "-in", requestPath.toString(), "-CA", nextRootPath.toString(),
                    "-CAkey", nextRootKeyPath.toString(), "-out", nextCertificatePath.toString());
            byte[] rootPem = Files.readAllBytes(rootPath);
            byte[] nextRootPem = Files.readAllBytes(nextRootPath);
            URI configuredEndpoint = URI.create("https://127.0.0.1:8443/");
            assertThrows(IllegalArgumentException.class,
                    () -> HttpServiceProfile.builder("vault", configuredEndpoint)
                            .trustedRootsPem(List.of(rootPem, rootPem)));
            assertThrows(IllegalArgumentException.class,
                    () -> HttpServiceProfile.builder("vault", configuredEndpoint)
                            .trustedRootsPem(List.of(rootPem, nextRootPem, rootPem,
                                    nextRootPem, rootPem)));
            byte[] bundledRoots = new byte[rootPem.length + nextRootPem.length];
            System.arraycopy(rootPem, 0, bundledRoots, 0, rootPem.length);
            System.arraycopy(nextRootPem, 0, bundledRoots, rootPem.length, nextRootPem.length);
            assertThrows(IllegalArgumentException.class,
                    () -> HttpServiceProfile.builder("vault", configuredEndpoint)
                            .trustedRootPem(bundledRoots));
            Certificate certificate;
            Certificate root;
            Certificate nextCertificate;
            Certificate nextRoot;
            try (var source = Files.newInputStream(certificatePath)) {
                certificate = CertificateFactory.getInstance("X.509").generateCertificate(source);
            }
            try (var source = Files.newInputStream(rootPath)) {
                root = CertificateFactory.getInstance("X.509").generateCertificate(source);
            }
            try (var source = Files.newInputStream(nextCertificatePath)) {
                nextCertificate = CertificateFactory.getInstance("X.509").generateCertificate(source);
            }
            try (var source = Files.newInputStream(nextRootPath)) {
                nextRoot = CertificateFactory.getInstance("X.509").generateCertificate(source);
            }
            String keyText = Files.readString(keyPath);
            String encoded = keyText.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(null, null);
            char[] password = "fixture-only".toCharArray();
            store.setKeyEntry("server", key, password, new Certificate[] {certificate, root});
            KeyManagerFactory managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            managers.init(store, password);
            SSLContext tls = SSLContext.getInstance("TLS");
            tls.init(managers.getKeyManagers(), null, null);
            KeyStore nextStore = KeyStore.getInstance("PKCS12");
            nextStore.load(null, null);
            nextStore.setKeyEntry("server", key, password, new Certificate[] {nextCertificate, nextRoot});
            KeyManagerFactory nextManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            nextManagers.init(nextStore, password);
            SSLContext nextTls = SSLContext.getInstance("TLS");
            nextTls.init(nextManagers.getKeyManagers(), null, null);

            var executor = Executors.newCachedThreadPool();
            AtomicInteger receivedRequests = new AtomicInteger();
            AtomicInteger acceptedConnections = new AtomicInteger();
            List<String> fixtureEvents = new CopyOnWriteArrayList<>();
            try (SSLServerSocket server = (SSLServerSocket) tls.getServerSocketFactory()
                    .createServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
                server.setSoTimeout(250);
                CompletableFuture<Void> served = new CompletableFuture<>();
                var acceptor = executor.submit(() -> {
                    while (!served.isDone()) {
                        try {
                            SSLSocket socket = (SSLSocket) server.accept();
                            int connection = acceptedConnections.incrementAndGet();
                            fixtureEvents.add("accepted");
                            executor.execute(() -> {
                                try (socket) {
                                    socket.setSoTimeout(8000);
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                                            socket.getInputStream(), StandardCharsets.US_ASCII));
                                    String firstLine = reader.readLine();
                                    if (firstLine == null) {
                                        fixtureEvents.add("closed-before-http");
                                        return;
                                    }
                                    fixtureEvents.add("received-http");
                                    receivedRequests.incrementAndGet();
                                    assertTrue(firstLine.startsWith("GET /v1/secret/data/payment/api HTTP/1.1"));
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
                                    fixtureEvents.add("responded");
                                } catch (javax.net.ssl.SSLException | java.net.SocketTimeoutException
                                        | java.net.SocketException incompleteHandshake) {
                                    fixtureEvents.add("incomplete-handshake:" + incompleteHandshake.getClass().getSimpleName());
                                    // Failed TLS connections may still occupy a worker, but
                                    // cannot block acceptance of the trusted connection.
                                    if (connection == 4) served.complete(null);
                                } catch (Throwable error) {
                                    fixtureEvents.add("fixture-error:" + error.getClass().getSimpleName());
                                    served.completeExceptionally(error);
                                }
                            });
                        } catch (java.net.SocketTimeoutException awaitingConnection) {
                            // Recheck whether the trusted request has finished.
                        } catch (Throwable error) {
                            served.completeExceptionally(error);
                        }
                    }
                    return null;
                });
                DefaultUserContext context = tlsContext(server.getLocalPort(), null, captured);
                SecretFailure untrusted = assertThrows(SecretFailure.class,
                        () -> context.resolveSecret(REFERENCE, "read configured credential"));
                assertEquals(SecretFailure.Code.UNAVAILABLE, untrusted.code());
                assertNull(untrusted.getCause());
                assertEquals("untrusted peer sent an HTTP request", 0, receivedRequests.get());
                var trustedProfile = HttpServiceProfile.builder("vault",
                        URI.create("https://127.0.0.1:" + server.getLocalPort() + "/"))
                        .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                        .trustedRootPem(rootPem).build();
                context.putAttribute(HttpRequestService.class.getName(),
                        new JdkHttpRequestService(List.of(trustedProfile)));
                SecretValue value;
                try {
                    value = context.resolveSecret(REFERENCE, "read configured credential");
                } catch (SecretFailure failure) {
                    throw new AssertionError("trusted TLS failed; fixture=" + fixtureEvents, failure);
                }
                assertEquals("PRIVATE-VAULT-VALUE", new String(value.exposeBytes(), StandardCharsets.UTF_8));
                assertEquals("vault-kv2:7", value.version());
                var overlapProfile = HttpServiceProfile.builder("vault",
                        URI.create("https://127.0.0.1:" + server.getLocalPort() + "/"))
                        .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                        .trustedRootsPem(List.of(rootPem, nextRootPem)).build();
                context.putAttribute(HttpRequestService.class.getName(),
                        new JdkHttpRequestService(List.of(overlapProfile)));
                SecretValue overlap = context.resolveSecret(REFERENCE, "read during TLS root overlap");
                assertEquals("vault-kv2:7", overlap.version());
                var removedTrustProfile = HttpServiceProfile.builder("vault",
                        URI.create("https://127.0.0.1:" + server.getLocalPort() + "/"))
                        .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                        .trustedRootPem(nextRootPem)
                        .build();
                context.putAttribute(HttpRequestService.class.getName(),
                        new JdkHttpRequestService(List.of(removedTrustProfile)));
                SecretFailure trustRemoved = assertThrows(SecretFailure.class,
                        () -> context.resolveSecret(REFERENCE, "reject removed TLS trust"));
                assertEquals(SecretFailure.Code.UNAVAILABLE, trustRemoved.code());
                assertNull(trustRemoved.getCause());
                served.get(20, TimeUnit.SECONDS);
                acceptor.get(20, TimeUnit.SECONDS);
                assertEquals("trusted requests must be handled twice", 2, receivedRequests.get());
                assertTrue(captured.toString().contains("start:Operation"));
                assertTrue(captured.toString().contains("success:"));
                assertTrue(captured.toString().contains("failure:"));
                assertEquals(4, logs.size());
                String diagnostics = captured + "\n" + logs;
                assertFalse(diagnostics.contains("PRIVATE-VAULT-TOKEN"));
                assertFalse(diagnostics.contains("PRIVATE-VAULT-VALUE"));

                byte[] nextBody = new String(BODY, StandardCharsets.UTF_8)
                        .replace("\"version\":7", "\"version\":8")
                        .getBytes(StandardCharsets.UTF_8);
                try (SSLServerSocket nextServer = (SSLServerSocket) nextTls.getServerSocketFactory()
                        .createServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
                    nextServer.setSoTimeout(5000);
                    var nextServed = executor.submit(() -> {
                        for (int request = 0; request < 2; request++) {
                            try (SSLSocket socket = (SSLSocket) nextServer.accept()) {
                                socket.setSoTimeout(5000);
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
                                        + nextBody.length + "\r\nConnection: close\r\n\r\n")
                                        .getBytes(StandardCharsets.US_ASCII));
                                socket.getOutputStream().write(nextBody);
                            }
                        }
                        return null;
                    });
                    var overlapNextProfile = HttpServiceProfile.builder("vault",
                            URI.create("https://127.0.0.1:" + nextServer.getLocalPort() + "/"))
                            .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                            .trustedRootsPem(List.of(rootPem, nextRootPem)).build();
                    context.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(overlapNextProfile)));
                    assertEquals("vault-kv2:8",
                            context.resolveSecret(REFERENCE, "read next CA during overlap").version());
                    var nextOnlyProfile = HttpServiceProfile.builder("vault",
                            URI.create("https://127.0.0.1:" + nextServer.getLocalPort() + "/"))
                            .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                            .trustedRootPem(nextRootPem).build();
                    context.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(nextOnlyProfile)));
                    assertEquals("vault-kv2:8",
                            context.resolveSecret(REFERENCE, "read next CA after retirement").version());
                    nextServed.get(20, TimeUnit.SECONDS);
                }
                assertEquals(6, logs.size());
                String completeDiagnostics = captured + "\n" + logs;
                assertFalse(completeDiagnostics.contains("PRIVATE-VAULT-TOKEN"));
                assertFalse(completeDiagnostics.contains("PRIVATE-VAULT-VALUE"));

                // Replacing the Context service governs new calls, but an already
                // started call owns its old client until its bounded response ends.
                try (SSLServerSocket heldServer = (SSLServerSocket) tls.getServerSocketFactory()
                        .createServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
                    heldServer.setSoTimeout(8000);
                    CompletableFuture<Void> requestArrived = new CompletableFuture<>();
                    CompletableFuture<Void> releaseResponse = new CompletableFuture<>();
                    CompletableFuture<Void> oldRequestFinished = new CompletableFuture<>();
                    AtomicInteger heldHttpRequests = new AtomicInteger();
                    var heldServerTask = executor.submit(() -> {
                        SSLSocket oldSocket = (SSLSocket) heldServer.accept();
                        executor.execute(() -> {
                            try (oldSocket) {
                                oldSocket.setSoTimeout(8000);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(
                                        oldSocket.getInputStream(), StandardCharsets.US_ASCII));
                                assertTrue(reader.readLine().startsWith(
                                        "GET /v1/secret/data/payment/api HTTP/1.1"));
                                String line;
                                while ((line = reader.readLine()) != null && !line.isEmpty()) {}
                                heldHttpRequests.incrementAndGet();
                                requestArrived.complete(null);
                                releaseResponse.get(8, TimeUnit.SECONDS);
                                oldSocket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: "
                                        + BODY.length + "\r\nConnection: close\r\n\r\n")
                                        .getBytes(StandardCharsets.US_ASCII));
                                oldSocket.getOutputStream().write(BODY);
                                oldRequestFinished.complete(null);
                            } catch (Throwable failure) {
                                requestArrived.completeExceptionally(failure);
                                oldRequestFinished.completeExceptionally(failure);
                            }
                        });
                        try (SSLSocket removed = (SSLSocket) heldServer.accept()) {
                            removed.setSoTimeout(8000);
                            try {
                                String line = new BufferedReader(new InputStreamReader(
                                        removed.getInputStream(), StandardCharsets.US_ASCII)).readLine();
                                if (line != null) {
                                    heldHttpRequests.incrementAndGet();
                                    throw new AssertionError("retired CA received a new HTTP request");
                                }
                            } catch (java.io.IOException expected) {
                                // The new client rejects the old CA before any HTTP request.
                            }
                        }
                        return null;
                    });
                    URI heldEndpoint = URI.create("https://127.0.0.1:"
                            + heldServer.getLocalPort() + "/");
                    var oldOnly = HttpServiceProfile.builder("vault", heldEndpoint)
                            .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                            .trustedRootPem(rootPem).build();
                    context.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(oldOnly)));
                    var inFlight = executor.submit(() -> context.resolveSecret(
                            REFERENCE, "read before TLS root retirement"));
                    try {
                        requestArrived.get(8, TimeUnit.SECONDS);
                        var newOnly = HttpServiceProfile.builder("vault", heldEndpoint)
                                .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                                .trustedRootPem(nextRootPem).build();
                        context.putAttribute(HttpRequestService.class.getName(),
                                new JdkHttpRequestService(List.of(newOnly)));
                        SecretFailure rejected = assertThrows(SecretFailure.class,
                                () -> context.resolveSecret(REFERENCE, "reject retired CA on new call"));
                        assertEquals(SecretFailure.Code.UNAVAILABLE, rejected.code());
                        assertEquals(1, heldHttpRequests.get());
                    } finally {
                        releaseResponse.complete(null);
                    }
                    assertEquals("vault-kv2:7", inFlight.get(8, TimeUnit.SECONDS).version());
                    oldRequestFinished.get(8, TimeUnit.SECONDS);
                    heldServerTask.get(8, TimeUnit.SECONDS);
                    assertEquals(1, heldHttpRequests.get());
                }
                String inFlightDiagnostics = captured + "\n" + logs;
                assertFalse(inFlightDiagnostics.contains("PRIVATE-VAULT-TOKEN"));
                assertFalse(inFlightDiagnostics.contains("PRIVATE-VAULT-VALUE"));

                // Two independent Contexts model mixed old/new certificates
                // while client instances are upgraded at different times.
                try (SSLServerSocket oldServer = (SSLServerSocket) tls.getServerSocketFactory()
                        .createServerSocket(0, 3, InetAddress.getLoopbackAddress());
                     SSLServerSocket newServer = (SSLServerSocket) nextTls.getServerSocketFactory()
                        .createServerSocket(0, 4, InetAddress.getLoopbackAddress())) {
                    oldServer.setSoTimeout(10000);
                    newServer.setSoTimeout(10000);
                    AtomicInteger oldRequests = new AtomicInteger();
                    AtomicInteger newRequests = new AtomicInteger();
                    var oldServed = executor.submit(() -> {
                        serveRolloverTls(oldServer, 2, true, BODY, oldRequests);
                        return null;
                    });
                    var newServed = executor.submit(() -> {
                        serveRolloverTls(newServer, 4, false, nextBody, newRequests);
                        return null;
                    });
                    List<String> oldEvents = new CopyOnWriteArrayList<>();
                    List<String> newEvents = new CopyOnWriteArrayList<>();
                    DefaultUserContext oldInstance = tlsContext(oldServer.getLocalPort(), rootPem,
                            oldEvents);
                    DefaultUserContext newInstance = tlsContext(newServer.getLocalPort(), nextRootPem,
                            newEvents);
                    URI oldEndpoint = URI.create("https://127.0.0.1:" + oldServer.getLocalPort() + "/");
                    URI newEndpoint = URI.create("https://127.0.0.1:" + newServer.getLocalPort() + "/");
                    var newOverlap = HttpServiceProfile.builder("vault", newEndpoint)
                            .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                            .trustedRootsPem(List.of(rootPem, nextRootPem)).build();
                    newInstance.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(newOverlap)));
                    var oldFirst = executor.submit(() -> oldInstance.resolveSecret(
                            REFERENCE, "old instance before overlap"));
                    var newFirst = executor.submit(() -> newInstance.resolveSecret(
                            REFERENCE, "new instance during overlap"));
                    assertEquals("vault-kv2:7", oldFirst.get(8, TimeUnit.SECONDS).version());
                    assertEquals("vault-kv2:8", newFirst.get(8, TimeUnit.SECONDS).version());

                    var oldOverlap = HttpServiceProfile.builder("vault", oldEndpoint)
                            .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                            .trustedRootsPem(List.of(rootPem, nextRootPem)).build();
                    oldInstance.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(oldOverlap)));
                    var oldSecond = executor.submit(() -> oldInstance.resolveSecret(
                            REFERENCE, "old instance during overlap"));
                    var newSecond = executor.submit(() -> newInstance.resolveSecret(
                            REFERENCE, "new instance remains on overlap"));
                    assertEquals("vault-kv2:7", oldSecond.get(8, TimeUnit.SECONDS).version());
                    assertEquals("vault-kv2:8", newSecond.get(8, TimeUnit.SECONDS).version());

                    var oldNewOnly = HttpServiceProfile.builder("vault", oldEndpoint)
                            .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                            .trustedRootPem(nextRootPem).build();
                    var newNewOnly = HttpServiceProfile.builder("vault", newEndpoint)
                            .budgets(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(8))
                            .trustedRootPem(nextRootPem).build();
                    oldInstance.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(oldNewOnly)));
                    newInstance.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(newNewOnly)));
                    var newThird = executor.submit(() -> newInstance.resolveSecret(
                            REFERENCE, "new route after CA retirement"));
                    SecretFailure oldRejected = assertThrows(SecretFailure.class,
                            () -> oldInstance.resolveSecret(REFERENCE, "old route after CA retirement"));
                    assertEquals(SecretFailure.Code.UNAVAILABLE, oldRejected.code());
                    assertEquals("vault-kv2:8", newThird.get(8, TimeUnit.SECONDS).version());
                    assertEquals(2, oldRequests.get());

                    oldInstance.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(newNewOnly)));
                    assertEquals("vault-kv2:8", oldInstance.resolveSecret(
                            REFERENCE, "old instance after route migration").version());
                    oldServed.get(10, TimeUnit.SECONDS);
                    newServed.get(10, TimeUnit.SECONDS);
                    assertEquals(2, oldRequests.get());
                    assertEquals(4, newRequests.get());
                    for (List<String> events : List.of(oldEvents, newEvents)) {
                        assertFalse(events.toString().contains("PRIVATE-VAULT-TOKEN"));
                        assertFalse(events.toString().contains("PRIVATE-VAULT-VALUE"));
                    }
                }

                // A response held beyond the whole-call budget must release
                // the old Context call before a new trust profile is used.
                try (SSLServerSocket stalledServer = (SSLServerSocket) tls.getServerSocketFactory()
                        .createServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
                    stalledServer.setSoTimeout(8000);
                    CompletableFuture<Void> requestArrived = new CompletableFuture<>();
                    CompletableFuture<Void> releaseResponse = new CompletableFuture<>();
                    AtomicInteger stalledRequests = new AtomicInteger();
                    var stalledServerTask = executor.submit(() -> {
                        try (SSLSocket socket = (SSLSocket) stalledServer.accept()) {
                            socket.setSoTimeout(8000);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                    socket.getInputStream(), StandardCharsets.US_ASCII));
                            assertTrue(reader.readLine().startsWith(
                                    "GET /v1/secret/data/payment/api HTTP/1.1"));
                            String line;
                            while ((line = reader.readLine()) != null && !line.isEmpty()) {}
                            stalledRequests.incrementAndGet();
                            requestArrived.complete(null);
                            releaseResponse.get(8, TimeUnit.SECONDS);
                        }
                        stalledServer.setSoTimeout(300);
                        try (SSLSocket unexpected = (SSLSocket) stalledServer.accept()) {
                            throw new AssertionError("stalled Vault read started another connection");
                        } catch (java.net.SocketTimeoutException expected) {
                            return null;
                        }
                    });
                    URI stalledEndpoint = URI.create("https://127.0.0.1:"
                            + stalledServer.getLocalPort() + "/");
                    var shortOld = HttpServiceProfile.builder("vault", stalledEndpoint)
                            .budgets(Duration.ofSeconds(2), Duration.ofSeconds(2),
                                    Duration.ofMillis(900))
                            .trustedRootPem(rootPem).build();
                    context.putAttribute(HttpRequestService.class.getName(),
                            new JdkHttpRequestService(List.of(shortOld)));
                    long startedAt = System.nanoTime();
                    var stalled = executor.submit(() -> context.resolveSecret(
                            REFERENCE, "bound stalled old-CA read"));
                    try {
                        requestArrived.get(5, TimeUnit.SECONDS);
                        var shortNew = HttpServiceProfile.builder("vault", stalledEndpoint)
                                .budgets(Duration.ofSeconds(2), Duration.ofSeconds(2),
                                        Duration.ofMillis(900))
                                .trustedRootPem(nextRootPem).build();
                        context.putAttribute(HttpRequestService.class.getName(),
                                new JdkHttpRequestService(List.of(shortNew)));
                        var failure = assertThrows(java.util.concurrent.ExecutionException.class,
                                () -> stalled.get(4, TimeUnit.SECONDS));
                        assertTrue(failure.getCause() instanceof SecretFailure);
                        assertEquals(SecretFailure.Code.UNAVAILABLE,
                                ((SecretFailure) failure.getCause()).code());
                        assertTrue(Duration.ofNanos(System.nanoTime() - startedAt)
                                .compareTo(Duration.ofSeconds(4)) < 0);
                        assertEquals(1, stalledRequests.get());
                    } finally {
                        releaseResponse.complete(null);
                    }
                    stalledServerTask.get(5, TimeUnit.SECONDS);
                }
                String budgetDiagnostics = captured + "\n" + logs;
                assertFalse(budgetDiagnostics.contains("PRIVATE-VAULT-TOKEN"));
                assertFalse(budgetDiagnostics.contains("PRIVATE-VAULT-VALUE"));
            } finally {
                executor.shutdownNow();
            }
        } finally {
            logger.removeHandler(handler);
            Files.deleteIfExists(keyPath);
            Files.deleteIfExists(certificatePath);
            Files.deleteIfExists(nextCertificatePath);
            Files.deleteIfExists(requestPath);
            Files.deleteIfExists(serialPath);
            Files.deleteIfExists(nextSerialPath);
            Files.deleteIfExists(rootKeyPath);
            Files.deleteIfExists(rootPath);
            Files.deleteIfExists(nextRootKeyPath);
            Files.deleteIfExists(nextRootPath);
            Files.deleteIfExists(directory);
        }
    }

    private static void serveRolloverTls(SSLServerSocket server, int successfulCalls,
                                         boolean rejectLast, byte[] body,
                                         AtomicInteger receivedRequests) throws Exception {
        int attempts = successfulCalls + (rejectLast ? 1 : 0);
        for (int attempt = 0; attempt < attempts; attempt++) {
            try (SSLSocket socket = (SSLSocket) server.accept()) {
                socket.setSoTimeout(8000);
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.US_ASCII));
                    String firstLine = reader.readLine();
                    if (rejectLast && attempt == successfulCalls) {
                        assertNull("retired CA received an HTTP request", firstLine);
                        continue;
                    }
                    assertNotNull("trusted rollover request missing", firstLine);
                    assertTrue(firstLine.startsWith("GET /v1/secret/data/payment/api HTTP/1.1"));
                    boolean tokenSeen = false;
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        if (line.toLowerCase(java.util.Locale.ROOT).startsWith("x-vault-token:")) {
                            tokenSeen = line.trim().endsWith("PRIVATE-VAULT-TOKEN");
                        }
                    }
                    assertTrue(tokenSeen);
                    receivedRequests.incrementAndGet();
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: "
                            + body.length + "\r\nConnection: close\r\n\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(body);
                } catch (java.io.IOException incompleteHandshake) {
                    if (!rejectLast || attempt != successfulCalls) {
                        throw incompleteHandshake;
                    }
                }
            }
        }
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
