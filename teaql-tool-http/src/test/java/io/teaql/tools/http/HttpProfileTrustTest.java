package io.teaql.tools.http;

import io.teaql.core.UserContext;
import io.teaql.core.http.HttpRequestFailure;
import io.teaql.core.http.HttpRequestService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Dedicated private-CA proof; no Secret Provider or external service is involved. */
public class HttpProfileTrustTest {
    private static Path directory;
    private static final List<byte[]> roots = new ArrayList<>();

    private static void openssl(String... args) throws Exception {
        var command = new ArrayList<String>(); command.add("openssl"); command.addAll(List.of(args));
        Process child = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            assertTrue("TLS fixture generator exceeded budget", child.waitFor(30, TimeUnit.SECONDS));
            assertEquals("TLS fixture generator failed", 0, child.exitValue());
        } finally {
            if (child.isAlive()) { child.destroyForcibly(); child.waitFor(); }
        }
    }

    @BeforeClass public static void createCertificates() throws Exception {
        directory = Files.createTempDirectory("teaql-http-trust-");
        for (int i = 0; i < 4; i++) {
            openssl("req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                    "-subj", "/CN=TeaQL HTTP fixture root " + i,
                    "-addext", "basicConstraints=critical,CA:TRUE",
                    "-addext", "keyUsage=critical,keyCertSign,cRLSign",
                    "-keyout", "root-" + i + ".key", "-out", "root-" + i + ".pem");
            roots.add(Files.readAllBytes(directory.resolve("root-" + i + ".pem")));
        }
        openssl("req", "-new", "-newkey", "rsa:2048", "-nodes", "-subj", "/CN=HTTP fixture",
                "-addext", "subjectAltName=IP:127.0.0.1", "-keyout", "server.key", "-out", "server.csr");
        for (int i = 0; i < 2; i++) openssl("x509", "-req", "-days", "1", "-CAcreateserial",
                "-copy_extensions", "copy", "-in", "server.csr", "-CA", "root-" + i + ".pem",
                "-CAkey", "root-" + i + ".key", "-out", "server-" + i + ".pem");
        openssl("req", "-new", "-key", "server.key", "-subj", "/CN=HTTP wrong host fixture",
                "-addext", "subjectAltName=IP:127.0.0.2", "-out", "wrong-host.csr");
        openssl("x509", "-req", "-days", "1", "-CAcreateserial", "-copy_extensions", "copy",
                "-in", "wrong-host.csr", "-CA", "root-0.pem", "-CAkey", "root-0.key",
                "-out", "wrong-host.pem");
    }

    @AfterClass public static void removeOwnedPrivateKeys() throws Exception {
        if (directory == null) return;
        try (var files = Files.walk(directory)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
        }
    }

    private static HttpServiceProfile.Builder profile(String alias, URI uri) {
        return HttpServiceProfile.builder(alias, uri)
                .budgets(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(5));
    }

    private static UserContext context() {
        return (UserContext) Proxy.newProxyInstance(UserContext.class.getClassLoader(),
                new Class<?>[] { UserContext.class }, (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    return null;
                });
    }

    private static void success(JdkHttpRequestService service, String alias) {
        var response = service.execute(context(), HttpRequestService.Call.get(alias, "status",
                "load private upstream status", "verify profile-scoped HTTPS trust"));
        assertEquals(200, response.status()); assertArrayEquals("ok".getBytes(StandardCharsets.US_ASCII), response.body());
    }

    private static void rejected(JdkHttpRequestService service, String alias) {
        var failure = assertThrows(HttpRequestFailure.class, () -> success(service, alias));
        assertEquals(HttpRequestFailure.Code.UPSTREAM_FAILURE, failure.code()); assertNull(failure.getCause());
    }

    @Test public void rejectsInvalidTrustInputsAndAcceptsOneToFourDistinctRoots() {
        URI https = URI.create("https://127.0.0.1/");
        byte[] old = roots.get(0), next = roots.get(1);
        byte[] bundled = new byte[old.length + next.length];
        System.arraycopy(old, 0, bundled, 0, old.length); System.arraycopy(next, 0, bundled, old.length, next.length);
        var invalid = List.of(List.<byte[]>of(), List.of(new byte[0]), List.of(new byte[65537]),
                List.of("PRIVATE_ROOT_CANARY".getBytes(StandardCharsets.US_ASCII)), List.of(bundled),
                List.of(old, old), List.of(old, new String(old, StandardCharsets.US_ASCII)
                        .replace("\n", "\r\n").getBytes(StandardCharsets.US_ASCII)),
                List.of(old, next, roots.get(2), roots.get(3), old));
        for (var input : invalid) {
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> profile("upstream", https).trustedRootsPem(input));
            assertEquals("invalid trusted TLS root", failure.getMessage()); assertNull(failure.getCause());
        }
        assertThrows(IllegalArgumentException.class, () -> profile("upstream", https).trustedRootPem(null));
        assertThrows(IllegalArgumentException.class, () -> profile("upstream", https).trustedRootsPem(null));
        assertThrows(IllegalArgumentException.class, () -> profile("upstream", https).trustedRootsPem(Arrays.asList(old, null)));
        assertThrows(IllegalArgumentException.class,
                () -> profile("upstream", URI.create("http://127.0.0.1/")).trustedRootPem(old));
        for (int count = 1; count <= 4; count++) assertEquals(count,
                profile("upstream", https).trustedRootsPem(roots.subList(0, count)).build().trustedRoots().size());
        System.out.println("HTTP_TRUST_INPUT_PASS language=java bounded=1 malformed=1 duplicate=1 bundle=1 https_only=1");
    }

    @Test public void customRootsStayProfileLocalAndDoNotDisableHostnameChecks() throws Exception {
        try (var peer = new Peer("server-0.pem", 0); var wrong = new Peer("wrong-host.pem", 0)) {
            var service = new JdkHttpRequestService(List.of(profile("system", peer.uri()).build(),
                    profile("trusted", peer.uri()).trustedRootPem(roots.get(0)).build(),
                    profile("wrong_host", wrong.uri()).trustedRootPem(roots.get(0)).build()));
            rejected(service, "system"); assertEquals(0, peer.requests.get());
            success(service, "trusted"); assertEquals(1, peer.requests.get());
            rejected(service, "system"); assertEquals(1, peer.requests.get());
            rejected(service, "wrong_host"); assertEquals(0, wrong.requests.get());
            System.out.println("HTTP_TRUST_ISOLATION_PASS language=java system_reject=2 custom_success=1 hostname_reject=1");
        }
    }

    @Test public void overlappingRootsRotateAcrossBothLivePeersWithoutReset() throws Exception {
        try (var old = new Peer("server-0.pem", 0); var next = new Peer("server-1.pem", 1)) {
            var oldOnly = new JdkHttpRequestService(List.of(profile("old", old.uri()).trustedRootPem(roots.get(0)).build(),
                    profile("next", next.uri()).trustedRootPem(roots.get(0)).build()));
            success(oldOnly, "old"); rejected(oldOnly, "next"); assertEquals(0, next.requests.get());
            var overlap = new JdkHttpRequestService(List.of(profile("old", old.uri()).trustedRootsPem(roots.subList(0, 2)).build(),
                    profile("next", next.uri()).trustedRootsPem(roots.subList(0, 2)).build()));
            var newOnly = new JdkHttpRequestService(List.of(profile("old", old.uri()).trustedRootPem(roots.get(1)).build(),
                    profile("next", next.uri()).trustedRootPem(roots.get(1)).build()));
            for (int round = 1; round <= 2; round++) {
                success(overlap, "old"); success(overlap, "next"); rejected(newOnly, "old"); success(newOnly, "next");
                assertEquals(1 + round, old.requests.get()); assertEquals(2 * round, next.requests.get());
                System.out.println("HTTP_TRUST_ROTATION_PASS language=java round=" + round + " old_requests="
                        + old.requests.get() + " new_requests=" + next.requests.get());
            }
        }
    }

    @Test public void builtProfilesSnapshotInputsAndFailedReplacementIsAtomic() throws Exception {
        try (var peer = new Peer("server-0.pem", 0)) {
            byte[] copy = roots.get(0).clone(); var input = new ArrayList<>(List.of(copy));
            var builder = profile("trusted", peer.uri()).trustedRootsPem(input); var before = builder.build();
            input.clear(); Arrays.fill(copy, (byte) 0);
            assertThrows(IllegalArgumentException.class, () -> builder.trustedRootsPem(List.of(roots.get(1), new byte[0])));
            var afterFailure = builder.build(); builder.trustedRootPem(roots.get(1));
            success(new JdkHttpRequestService(List.of(before)), "trusted");
            success(new JdkHttpRequestService(List.of(afterFailure)), "trusted");
            rejected(new JdkHttpRequestService(List.of(builder.build())), "trusted");
            assertEquals(2, peer.requests.get());
        }
    }

    @Test public void profileLimitsAndDuplicateAliasesFailBeforeUse() {
        URI uri = URI.create("https://127.0.0.1/");
        for (Duration invalid : List.of(Duration.ZERO, Duration.ofNanos(-1), Duration.ofHours(1).plusNanos(1))) {
            assertThrows(IllegalArgumentException.class, () -> profile("valid", uri).budgets(invalid, Duration.ofSeconds(1), Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class, () -> profile("valid", uri).budgets(Duration.ofSeconds(1), invalid, Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class, () -> profile("valid", uri).budgets(Duration.ofSeconds(1), Duration.ofSeconds(1), invalid));
        }
        for (int bytes : List.of(0, 16 * 1024 * 1024 + 1)) assertThrows(IllegalArgumentException.class,
                () -> profile("valid", uri).maxResponseBytes(bytes).build());
        for (int attempts : List.of(0, 6)) assertThrows(IllegalArgumentException.class,
                () -> profile("valid", uri).retry(attempts, Duration.ofMillis(1), Set.of()).build());
        assertThrows(IllegalArgumentException.class, () -> profile("valid", uri).circuit(1, Duration.ofSeconds(1), 0).build());
        assertThrows(IllegalArgumentException.class, () -> new JdkHttpRequestService(List.of(profile("same", uri).build(), profile("same", uri).build())));
    }

    private static final class Peer implements AutoCloseable {
        final SSLServerSocket server;
        final Thread worker;
        final AtomicBoolean closed = new AtomicBoolean();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicInteger requests = new AtomicInteger();
        Peer(String certificateFile, int rootIndex) throws Exception {
            var factory = CertificateFactory.getInstance("X.509");
            var certificate = (X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(Files.readAllBytes(directory.resolve(certificateFile))));
            var root = (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(roots.get(rootIndex)));
            certificate.checkValidity(); certificate.verify(root.getPublicKey());
            String key = Files.readString(directory.resolve("server.key")).replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            var privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key)));
            var store = KeyStore.getInstance("PKCS12"); store.load(null, null);
            char[] password = "fixture-only".toCharArray();
            store.setKeyEntry("server", privateKey, password, new java.security.cert.Certificate[] {certificate, root});
            var managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); managers.init(store, password);
            var tls = SSLContext.getInstance("TLS"); tls.init(managers.getKeyManagers(), null, null);
            server = (SSLServerSocket) tls.getServerSocketFactory().createServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
            server.setSoTimeout(100);
            worker = new Thread(() -> {
                while (!closed.get()) {
                    try (var socket = (SSLSocket) server.accept()) {
                        socket.setSoTimeout(2000);
                        var reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                        String first = reader.readLine();
                        if (first == null) continue;
                        assertTrue(first.startsWith("GET /status HTTP/1.1"));
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {}
                        if (line == null) continue;
                        requests.incrementAndGet();
                        socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                    } catch (java.io.IOException rejectedOrStopped) {
                        // Unknown CA and hostname failures cannot enter HTTP.
                    } catch (Throwable unexpected) { error.compareAndSet(null, unexpected); return; }
                }
            }, "teaql-http-trust-peer");
            worker.start();
        }
        URI uri() { return URI.create("https://127.0.0.1:" + server.getLocalPort() + "/"); }
        @Override public void close() throws Exception {
            closed.set(true); server.close(); worker.join(5000);
            assertFalse("owned TLS peer did not stop", worker.isAlive());
            if (error.get() != null) throw new AssertionError("owned TLS peer failed", error.get());
        }
    }
}
