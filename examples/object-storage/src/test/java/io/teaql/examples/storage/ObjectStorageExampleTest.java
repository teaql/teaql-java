package io.teaql.examples.storage;

import io.teaql.core.storage.*;
import io.teaql.core.storage.ObjectStorage.*;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.storage.s3.S3ObjectStorage;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;

/** Opt-in live test; verify.sh always requires configuration, never silently skips. */
public class ObjectStorageExampleTest {
    @Test public void binaryStorageFromContext() throws Exception {
        org.junit.Assume.assumeTrue("Live S3 requires examples/object-storage/verify.sh", Boolean.getBoolean("teaql.s3.live"));
        int iterations = Integer.getInteger("teaql.s3.iterations", 1);
        if (iterations < 1 || iterations > 100) throw new IllegalArgumentException("iterations must be 1..100");
        for (int run = 0; run < iterations; run++) verifyOnce();
    }
    private void verifyOnce() throws Exception {
        URI endpoint = URI.create(required("TEAQL_S3_ENDPOINT"));
        String bucket = required("TEAQL_S3_BUCKET");
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
                required("TEAQL_S3_ACCESS_KEY"), required("TEAQL_S3_SECRET_KEY")));
        // Test infrastructure only. The runtime provider never creates a bucket.
        try (var admin = S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(credentials).forcePathStyle(true).build()) {
            try { admin.headBucket(b -> b.bucket(bucket)); }
            catch (software.amazon.awssdk.services.s3.model.NoSuchBucketException missing) { admin.createBucket(b -> b.bucket(bucket)); }
            catch (software.amazon.awssdk.services.s3.model.S3Exception failure) {
                if (failure.statusCode() != 404) throw failure;
                admin.createBucket(b -> b.bucket(bucket));
            }
        }
        try (var provider = S3ObjectStorage.connect(endpoint, Region.US_EAST_1, credentials, bucket, true)) {
            var context = new DefaultUserContext(null);
            context.putAttribute(ObjectStorageProvider.class.getName(), provider);
            var storage = context.objectStorage("verify binary object storage");
            var key = new Key("java/" + UUID.randomUUID() + "/二进制 space+%.bin");
            var emptyKey = new Key(key.value() + ".empty");
            var raceKey = new Key(key.value() + ".race");
            byte[] bytes = new byte[256 * 1024];
            new Random(42).nextBytes(bytes);
            var options = new PutOptions(bytes.length, "application/octet-stream", Map.of("fixture", "binary"), WriteMode.CREATE_ONLY);
            try {
                assertFalse(storage.exists(key));
                expect(StorageException.Code.NOT_FOUND, () -> storage.get(key));
                assertEquals(bytes.length, storage.putBytes(key, bytes, options).size());
                assertEquals("binary", storage.head(key).orElseThrow().customMetadata().get("fixture"));
                assertArrayEquals(bytes, storage.getBytes(key, bytes.length));
                expect(StorageException.Code.CONFLICT, () -> storage.putBytes(key, new byte[0], PutOptions.create(0)));
                assertArrayEquals(bytes, storage.getBytes(key, bytes.length));
                expect(StorageException.Code.TOO_LARGE, () -> storage.getBytes(key, bytes.length - 1));
                var url = storage.getReadUrl(key, new ReadUrlOptions(Duration.ofSeconds(30), Disposition.ATTACHMENT, "sample.bin"));
                assertFalse(url.toString().contains("X-Amz"));
                var http = HttpClient.newHttpClient();
                var downloaded = http.send(HttpRequest.newBuilder(url.url()).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, downloaded.statusCode());
                assertArrayEquals(bytes, downloaded.body());
                assertTrue(downloaded.headers().firstValue("Content-Disposition").orElseThrow().contains("sample.bin"));
                var expiring = storage.getReadUrl(key, new ReadUrlOptions(Duration.ofSeconds(1), null, null));
                Thread.sleep(2200);
                assertEquals(403, http.send(HttpRequest.newBuilder(expiring.url()).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode());
                storage.putBytes(emptyKey, new byte[0], PutOptions.create(0));
                assertArrayEquals(new byte[0], storage.getBytes(emptyKey, 0));
                expect(StorageException.Code.INVALID_INPUT, () -> storage.putBytes(emptyKey, new byte[]{1}, PutOptions.create(0)));
                storage.putBytes(key, new byte[]{4, 5}, new PutOptions(2, null, Map.of(), WriteMode.REPLACE));
                assertArrayEquals(new byte[]{4, 5}, storage.getBytes(key, 2));
                ExecutorService threads = Executors.newFixedThreadPool(2);
                try {
                    var tasks = new ArrayList<Callable<Boolean>>();
                    for (int i = 0; i < 2; i++) tasks.add(() -> {
                        var other = new DefaultUserContext(null);
                        other.putAttribute(ObjectStorageProvider.class.getName(), provider);
                        try { other.objectStorage("verify atomic create").putBytes(raceKey, new byte[]{7}, PutOptions.create(1)); return true; }
                        catch (StorageException error) { assertEquals(StorageException.Code.CONFLICT, error.code()); return false; }
                    });
                    var results = threads.invokeAll(tasks);
                    assertEquals(1, (results.get(0).get() ? 1 : 0) + (results.get(1).get() ? 1 : 0));
                } finally { threads.shutdownNow(); }
                // A missing bucket must not become a false exists result.
                try (var wrong = S3ObjectStorage.connect(endpoint, Region.US_EAST_1, credentials, "missing-" + UUID.randomUUID(), true)) {
                    var other = new DefaultUserContext(null);
                    other.putAttribute(ObjectStorageProvider.class.getName(), wrong);
                    expect(StorageException.Code.UNAVAILABLE, () -> other.objectStorage("verify missing bucket").exists(key));
                }
            } finally {
                storage.delete(key); storage.delete(key); storage.delete(emptyKey); storage.delete(raceKey);
            }
            assertFalse(storage.exists(key));
            System.out.println("PASS Java S3: binary, empty, metadata, URL download/expiry, atomic create, replace, bounded reads, missing bucket, repeated delete");
        }
    }
    static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Required test setting: " + name);
        return value;
    }
    static void expect(StorageException.Code code, Runnable work) {
        try { work.run(); fail("Expected " + code); }
        catch (StorageException error) {
            if (code != error.code()) {
                AssertionError failure = new AssertionError("Expected " + code + " but got " + error.code());
                failure.initCause(error); // Sanitized wrapper only; retains the exact provider call site.
                throw failure;
            }
        }
    }
}
