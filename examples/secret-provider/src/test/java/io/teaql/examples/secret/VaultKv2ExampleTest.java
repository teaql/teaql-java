package io.teaql.examples.secret;

import io.teaql.core.http.HttpRequestService;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.secret.SecretFailure;
import io.teaql.core.secret.SecretReference;
import io.teaql.core.secret.SecretValue;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.RuntimeTelemetry;
import io.teaql.runtime.TeaQLRuntime;
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
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
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

    private DefaultUserContext tlsContext(int port, byte[] trustedRoot, List<String> captured) {
        var builder = HttpServiceProfile.builder("vault", URI.create("https://127.0.0.1:" + port + "/"))
                .budgets(Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(3));
        if (trustedRoot != null) builder.trustedRootPem(trustedRoot);
        var provider = VaultKv2SecretProvider.builder("vault", "secret",
                (caller, reference) -> new SecretValue(
                        "PRIVATE-VAULT-TOKEN".getBytes(StandardCharsets.UTF_8), null, null),
                new SecretReference("bootstrap", "vault_token"))
                .bind(REFERENCE, "payment/api", "value").build();
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
        var requestPath = directory.resolve("server.csr");
        var serialPath = directory.resolve("root.srl");
        var certificatePath = directory.resolve("server.pem");
        var keyPath = directory.resolve("server.key");
        try {
            openssl("req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                    "-subj", "/CN=TeaQL test root", "-addext", "basicConstraints=critical,CA:TRUE",
                    "-addext", "keyUsage=critical,keyCertSign,cRLSign",
                    "-keyout", rootKeyPath.toString(), "-out", rootPath.toString());
            openssl("req", "-new", "-newkey", "rsa:2048", "-nodes", "-subj", "/CN=127.0.0.1",
                    "-addext", "subjectAltName=IP:127.0.0.1",
                    "-keyout", keyPath.toString(), "-out", requestPath.toString());
            openssl("x509", "-req", "-days", "1", "-CAcreateserial", "-copy_extensions", "copy",
                    "-in", requestPath.toString(), "-CA", rootPath.toString(),
                    "-CAkey", rootKeyPath.toString(), "-out", certificatePath.toString());
            byte[] rootPem = Files.readAllBytes(rootPath);
            Certificate certificate;
            Certificate root;
            try (var source = Files.newInputStream(certificatePath)) {
                certificate = CertificateFactory.getInstance("X.509").generateCertificate(source);
            }
            try (var source = Files.newInputStream(rootPath)) {
                root = CertificateFactory.getInstance("X.509").generateCertificate(source);
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

            var executor = Executors.newSingleThreadExecutor();
            try (SSLServerSocket server = (SSLServerSocket) tls.getServerSocketFactory()
                    .createServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
                server.setSoTimeout(3000);
                var served = executor.submit(() -> {
                    for (int connection = 0; connection < 2; connection++) {
                        try (SSLSocket socket = (SSLSocket) server.accept()) {
                            socket.setSoTimeout(2000);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                    socket.getInputStream(), StandardCharsets.US_ASCII));
                            String firstLine = reader.readLine();
                            if (connection == 0) {
                                assertNull("untrusted peer received an HTTP request", firstLine);
                                continue;
                            }
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
                        } catch (javax.net.ssl.SSLException expectedUntrusted) {
                            if (connection != 0) throw expectedUntrusted;
                        }
                    }
                    return null;
                });
                SecretFailure untrusted = assertThrows(SecretFailure.class,
                        () -> tlsContext(server.getLocalPort(), null, captured)
                                .resolveSecret(REFERENCE, "read configured credential"));
                assertEquals(SecretFailure.Code.UNAVAILABLE, untrusted.code());
                assertNull(untrusted.getCause());
                SecretValue value = tlsContext(server.getLocalPort(), rootPem, captured)
                        .resolveSecret(REFERENCE, "read configured credential");
                assertEquals("PRIVATE-VAULT-VALUE", new String(value.exposeBytes(), StandardCharsets.UTF_8));
                assertEquals("vault-kv2:7", value.version());
                served.get(5, TimeUnit.SECONDS);
                assertTrue(captured.toString().contains("start:Operation"));
                assertTrue(captured.toString().contains("success:"));
                assertTrue(captured.toString().contains("failure:"));
                assertEquals(2, logs.size());
                String diagnostics = captured + "\n" + logs;
                assertFalse(diagnostics.contains("PRIVATE-VAULT-TOKEN"));
                assertFalse(diagnostics.contains("PRIVATE-VAULT-VALUE"));
            } finally {
                executor.shutdownNow();
            }
        } finally {
            logger.removeHandler(handler);
            Files.deleteIfExists(keyPath);
            Files.deleteIfExists(certificatePath);
            Files.deleteIfExists(requestPath);
            Files.deleteIfExists(serialPath);
            Files.deleteIfExists(rootKeyPath);
            Files.deleteIfExists(rootPath);
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
