package io.teaql.storage.s3;

import com.sun.net.httpserver.HttpServer;
import io.teaql.core.storage.*;
import io.teaql.core.storage.ObjectStorage.*;
import io.teaql.runtime.DefaultUserContext;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.core.retry.RetryPolicy;

public class S3ObjectStorageTest {
    @Test public void socketTimeoutIsSanitizedAndRetainsItsCategory() {
        var source = software.amazon.awssdk.core.exception.SdkClientException.builder()
                .message("PRIVATE-PROVIDER-CANARY")
                .cause(new java.net.SocketTimeoutException("PRIVATE-URL-CANARY")).build();
        var translated = S3ObjectStorage.translate(source);
        assertEquals(StorageException.Code.TIMEOUT, translated.code());
        assertNull(translated.getCause());
        assertFalse(translated.toString().contains("CANARY"));
    }
    @Test public void actualHttpErrorsAreNotAbsenceAndDoNotLeakProviderMessages() throws Exception {
        AtomicInteger status = new AtomicInteger(403);
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", request -> {
            calls.incrementAndGet();
            byte[] body = "<Error><Code>AccessDenied</Code><Message>PRIVATE-PROVIDER-CANARY</Message></Error>".getBytes(StandardCharsets.UTF_8);
            int code = status.get();
            if (request.getRequestURI().getPath().equals("/bucket") && code == 404) code = 200;
            request.getResponseHeaders().set("Content-Type", "application/xml");
            if (request.getRequestMethod().equals("HEAD")) request.sendResponseHeaders(code, -1);
            else { request.sendResponseHeaders(code, body.length); request.getResponseBody().write(body); }
            request.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "secret"));
        var config = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        try (var provider = new S3ObjectStorage(
                S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(credentials)
                    .serviceConfiguration(config).overrideConfiguration(b -> b.retryPolicy(RetryPolicy.none()).apiCallTimeout(Duration.ofSeconds(3))).build(),
                S3Presigner.builder().endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(credentials).serviceConfiguration(config).build(), "bucket")) {
            var context = new DefaultUserContext(null);
            context.putAttribute(ObjectStorageProvider.class.getName(), provider);
            var storage = context.objectStorage("verify provider error semantics"); var key = new Key("PRIVATE-KEY-CANARY");
            for (int code : new int[]{403, 500, 503, 408, 504, 501}) {
                status.set(code);
                try { storage.exists(key); fail("HTTP " + code + " must not be false"); }
                catch (StorageException error) {
                    var expected = switch (code) {
                        case 403 -> StorageException.Code.ACCESS_DENIED;
                        case 408, 504 -> StorageException.Code.TIMEOUT;
                        case 501 -> StorageException.Code.UNSUPPORTED;
                        default -> StorageException.Code.UNAVAILABLE;
                    };
                    assertEquals(expected, error.code());
                    assertNull(error.getCause());
                    assertFalse(error.toString().contains("CANARY"));
                }
            }
            status.set(404); assertFalse(storage.exists(key));
            status.set(200); expect(StorageException.Code.UNAVAILABLE, () -> storage.exists(key));
            int before = calls.get();
            expect(StorageException.Code.INVALID_INPUT, () -> storage.putBytes(key, new byte[]{1, 2}, PutOptions.create(1)));
            expect(StorageException.Code.INVALID_INPUT, () -> storage.putBytes(key, new byte[]{1}, PutOptions.create(2)));
            expect(StorageException.Code.IO, () -> storage.put(key, new InputStream() {
                @Override public int read() throws IOException { throw new IOException("PRIVATE-INPUT-CANARY"); }
            }, PutOptions.create(1)));
            assertEquals("invalid streams must not reach HTTP", before, calls.get());
            server.stop(0);
            expect(StorageException.Code.UNAVAILABLE, () -> storage.exists(key));
        } finally { server.stop(0); }
    }
    static void expect(StorageException.Code code, Runnable work) {
        try { work.run(); fail("Expected " + code); } catch (StorageException error) { assertEquals(code, error.code()); }
    }
}
