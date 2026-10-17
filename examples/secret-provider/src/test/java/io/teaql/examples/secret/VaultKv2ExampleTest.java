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
