package io.teaql.blob.store.s3;

import io.teaql.core.blob.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.interceptor.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import static io.teaql.core.blob.BlobException.Code.*;
import static org.junit.Assert.*;

/** Real task-owned versioned backend. Signed URLs never enter assertions, argv or logs. */
public class S3BlobStoreIT {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private static final Duration TTL=Duration.ofMinutes(10);

    @Test public void passiveConstructionAndExplicitReadinessDoNotCreateScratchOrAdministerBucket() throws Exception {
        try (Harness h=new Harness(false,null,null)) {
            var target=h.store.createUpload(null,"passive"); assertFalse(Files.exists(h.scratch));
            assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,() -> h.store.stat(null,target.location())).code());
            h.store.validateReady(); assertFalse(Files.exists(h.scratch));
            assertFalse(h.store.capabilities().directUpload()); assertFalse(h.store.capabilities().contextCheckedReads());
            assertTrue(h.store.capabilities().signedReads()); assertTrue(h.store.capabilities().versionConditionalDelete());
            assertEquals(5L*1024*1024*1024,h.store.maximumWriteBytes());
            assertEquals(BLOB_REFERENCE_INVALID,assertThrows(BlobException.class,() -> h.store.stat(null,"../outside")).code());
        }
    }

    @Test public void tenRealFilesRetainExactBytesDigestVersionAndRetryIdentity() throws Exception {
        try (Harness h=new Harness()) {
            for (String extension:List.of("jpg","jpeg","png","mp4","mp3","doc","docx","xls","xlsx","pdf")) {
                byte[] bytes=Files.readAllBytes(Path.of(required("TEAQL_BLOB_TEST_FIXTURES")).resolve("sample."+extension));
                var target=h.store.createUpload(null,"real_"+extension); var content=h.write(target,bytes);
                assertEquals(bytes.length,content.sizeBytes());
                assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),content.sha256());
                assertEquals(content,h.store.stat(null,target.location()).orElseThrow()); assertEquals(content,h.write(target,bytes));
                try (var range=h.store.openRange(null,target.location(),content.contentVersion(),0,bytes.length)) { assertArrayEquals(bytes,range.stream().readAllBytes()); }
                assertFalse(content.toString().contains(target.location()));
            }
            h.assertScratchEmpty();
        }
    }

    @Test public void boundedInputFailurePublishesNothingAndNeverClosesCallerStream() throws Exception {
        try (Harness h=new Harness()) {
            var target=h.store.createUpload(null,"bounded"); var source=new OwnedInput(new byte[65538]);
            assertEquals(BLOB_SIZE_EXCEEDED,assertThrows(BlobException.class,() -> h.store.write(null,target,source,65536)).code());
            assertEquals(65537,source.consumed); assertFalse(source.closed); assertTrue(h.store.stat(null,target.location()).isEmpty());
            InputStream broken=new InputStream() { @Override public int read() throws IOException { throw new IOException("test failure"); } };
            assertEquals(BLOB_STORE_FAILED,assertThrows(BlobException.class,() -> h.store.write(null,target,broken,100)).code());
            var empty=h.write(target,new byte[0]);
            try (var read=h.store.openRange(null,target.location(),empty.contentVersion(),0,0)) { assertEquals(-1,read.stream().read()); }
            h.assertScratchEmpty();
        }
    }

    @Test public void concurrentImmutableWritersReturnOneGenerationAndChangedRetryConflicts() throws Exception {
        try (Harness h=new Harness()) {
            var target=h.store.createUpload(null,"writers"); byte[] bytes={1,2,3}; ExecutorService executor=Executors.newFixedThreadPool(2);
            try {
                var start=new CountDownLatch(1);
                Callable<BlobStore.StoredContent> work=() -> { assertTrue(start.await(10,TimeUnit.SECONDS)); return h.write(target,bytes); };
                var first=executor.submit(work); var second=executor.submit(work); start.countDown();
                assertEquals(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
            } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(10,TimeUnit.SECONDS)); }
            assertEquals(BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,() -> h.write(target,new byte[]{9})).code());
            var versions=h.backend.client.listObjectVersions(b -> b.bucket(h.backend.bucket).prefix(target.location()));
            assertEquals(1,versions.versions().size()); h.assertScratchEmpty();
        }
    }

    @Test public void versionBoundReadsAndUrlsDoNotFollowReplacementAndStaleDeleteProtectsIt() throws Exception {
        try (Harness h=new Harness()) {
            var target=h.store.createUpload(null,"generations"); byte[] bytes={1,2,3,4}; var original=h.write(target,bytes);
            var signed=h.store.signReadBatch(null,List.of(read(original,TTL))).get(0);
            var replacement=h.replace(target,new byte[]{9,8,7,6});
            assertNotEquals(original.contentVersion(),replacement.versionId());
            try (var read=h.store.openRange(null,target.location(),original.contentVersion(),1,2)) { assertArrayEquals(new byte[]{2,3},read.stream().readAllBytes()); }
            assertEquals(BlobStore.DeleteResult.VERSION_MISMATCH,h.store.deleteIfVersion(null,target.location(),original.contentVersion()));
            assertEquals(replacement.versionId(),h.backend.client.headObject(b -> b.bucket(h.backend.bucket).key(target.location())).versionId());
            assertArrayEquals(bytes,h.backend.client.getObjectAsBytes(b -> b.bucket(h.backend.bucket).key(target.location()).versionId(original.contentVersion())).asByteArray());
            HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            var old=http.send(HttpRequest.newBuilder(signed.url()).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200,old.statusCode()); assertArrayEquals(bytes,old.body());
            assertEquals(BLOB_NOT_FOUND,assertThrows(BlobException.class,() -> h.store.signReadBatch(null,List.of(read(original,TTL)))).code());
            h.backend.client.deleteObject(b -> b.bucket(h.backend.bucket).key(target.location()).versionId(original.contentVersion()));
            var removed=http.send(HttpRequest.newBuilder(signed.url()).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.discarding());
            assertEquals(404,removed.statusCode());
            assertEquals(replacement.versionId(),h.backend.client.headObject(b -> b.bucket(h.backend.bucket).key(target.location())).versionId());
        }
    }

    @Test public void retirementLeavesPersistentFenceDeletesOnlyPinnedPayloadAndRetries() throws Exception {
        try (Harness h=new Harness()) {
            var target=h.store.createUpload(null,"retire"); var content=h.write(target,new byte[]{1,2,3});
            assertEquals(content,h.store.sealForDeletion(null,target.location(),content.contentVersion()).orElseThrow());
            assertTrue(h.store.stat(null,target.location()).isEmpty());
            assertEquals(BlobStore.DeleteResult.DELETED,h.store.deleteIfVersion(null,target.location(),content.contentVersion()));
            assertEquals(BlobStore.DeleteResult.ALREADY_MISSING,h.store.deleteIfVersion(null,target.location(),content.contentVersion()));
            assertEquals(404,assertThrows(S3Exception.class,() -> h.backend.client.headObject(b -> b.bucket(h.backend.bucket).key(target.location()).versionId(content.contentVersion()))).statusCode());
            var marker=h.backend.client.headObject(b -> b.bucket(h.backend.bucket).key(target.location()));
            assertEquals("retired-v1",marker.metadata().get("teaql-blob-state"));
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> h.write(target,new byte[]{1,2,3})).code());
            assertEquals(BLOB_NOT_FOUND,assertThrows(BlobException.class,() -> h.store.signReadBatch(null,List.of(read(content,TTL)))).code());
            var absent=h.store.createUpload(null,"never-published"); assertTrue(h.store.sealForDeletion(null,absent.location(),null).isEmpty());
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> h.write(absent,new byte[]{4})).code());
        }
    }

    @Test public void sameEtagReplacementBetweenHeadAndConditionalRetirementIsPreserved() throws Exception {
        AtomicReference<Runnable> replace=new AtomicReference<>(); AtomicBoolean fired=new AtomicBoolean();
        ExecutionInterceptor race=new ExecutionInterceptor() {
            @Override public void beforeTransmission(Context.BeforeTransmission context,ExecutionAttributes attributes) {
                if (context.request() instanceof PutObjectRequest put && "retired-v1".equals(put.metadata().get("teaql-blob-state")) && fired.compareAndSet(false,true)) replace.get().run();
            }
        };
        try (Harness h=new Harness(true,race,null)) {
            var target=h.store.createUpload(null,"same_etag_race"); byte[] bytes={8,9}; var original=h.write(target,bytes); AtomicReference<String> foreign=new AtomicReference<>();
            replace.set(() -> foreign.set(h.replace(target,bytes).versionId()));
            assertEquals(BlobStore.DeleteResult.VERSION_MISMATCH,h.store.deleteIfVersion(null,target.location(),original.contentVersion()));
            assertTrue(fired.get()); assertNotEquals(original.contentVersion(),foreign.get());
            assertEquals(foreign.get(),h.backend.client.headObject(b -> b.bucket(h.backend.bucket).key(target.location())).versionId());
            assertArrayEquals(bytes,h.backend.client.getObjectAsBytes(b -> b.bucket(h.backend.bucket).key(target.location()).versionId(original.contentVersion())).asByteArray());
            assertEquals(2,h.backend.client.listObjectVersions(b -> b.bucket(h.backend.bucket).prefix(target.location())).versions().size());
        }
    }

    @Test public void delayedRemotePublisherCannotResurrectASealedMissingUpload() throws Exception {
        CountDownLatch waiting=new CountDownLatch(1),resume=new CountDownLatch(1);
        ExecutionInterceptor delay=new ExecutionInterceptor() {
            @Override public void beforeTransmission(Context.BeforeTransmission context,ExecutionAttributes attributes) {
                if (context.request() instanceof PutObjectRequest put && "content-v1".equals(put.metadata().get("teaql-blob-state"))) {
                    waiting.countDown();
                    try { if (!resume.await(15,TimeUnit.SECONDS)) throw new IllegalStateException("Test publisher deadline"); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("Test publisher interrupted"); }
                }
            }
        };
        try (Harness h=new Harness(true,delay,null)) {
            var target=h.store.createUpload(null,"late"); ExecutorService executor=Executors.newSingleThreadExecutor();
            try {
                Future<BlobException.Code> late=executor.submit(() -> assertThrows(BlobException.class,() -> h.write(target,new byte[]{7})).code());
                assertTrue(waiting.await(10,TimeUnit.SECONDS)); assertTrue(h.store.sealForDeletion(null,target.location(),null).isEmpty());
                resume.countDown(); assertEquals(BLOB_UPLOAD_EXPIRED,late.get(20,TimeUnit.SECONDS));
                assertTrue(h.store.stat(null,target.location()).isEmpty());
                var versions=h.backend.client.listObjectVersions(b -> b.bucket(h.backend.bucket).prefix(target.location()));
                assertEquals(1,versions.versions().size());
                assertEquals("retired-v1",h.backend.client.headObject(b -> b.bucket(h.backend.bucket).key(target.location())).metadata().get("teaql-blob-state"));
            } finally { resume.countDown(); executor.shutdownNow(); assertTrue(executor.awaitTermination(10,TimeUnit.SECONDS)); }
            h.assertScratchEmpty();
        }
    }

    @Test public void actualSignedUrlWorksWithHttpCurlRangeHeadersAndExpiresWithoutDeletion() throws Exception {
        try (Harness h=new Harness()) {
            byte[] bytes=Files.readAllBytes(Path.of(required("TEAQL_BLOB_TEST_FIXTURES")).resolve("sample.mp4")); var content=h.write(h.store.createUpload(null,"http"),bytes);
            var signed=h.store.signReadBatch(null,List.of(read(content,Duration.ofSeconds(30)))).get(0);
            assertTrue(signed.expiresAt().isAfter(Instant.now())); assertFalse(signed.toString().contains("X-Amz"));
            HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            var response=http.send(HttpRequest.newBuilder(signed.url()).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200,response.statusCode()); assertArrayEquals(bytes,response.body());
            assertEquals("video/mp4",response.headers().firstValue("content-type").orElseThrow());
            assertTrue(response.headers().firstValue("content-disposition").orElseThrow().contains("filename*=UTF-8''%E7%94%B3%E8%AF%B7.mp4"));
            assertEquals("private, max-age=30",response.headers().firstValue("cache-control").orElseThrow());
            var range=http.send(HttpRequest.newBuilder(signed.url()).timeout(Duration.ofSeconds(10)).header("Range","bytes=2-9").GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(206,range.statusCode()); assertArrayEquals(Arrays.copyOfRange(bytes,2,10),range.body());
            Path curlBody=temporary.newFile().toPath();
            Process curl=new ProcessBuilder("curl","--silent","--show-error","--config","-").redirectOutput(curlBody.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try {
                try (var input=curl.getOutputStream()) { input.write(("url = \""+signed.url().toASCIIString()+"\"\nrange = \"3-11\"\n").getBytes(StandardCharsets.UTF_8)); }
                assertTrue(curl.waitFor(10,TimeUnit.SECONDS)); assertEquals(0,curl.exitValue()); assertArrayEquals(Arrays.copyOfRange(bytes,3,12),Files.readAllBytes(curlBody));
            } finally { if (curl.isAlive()) { curl.destroyForcibly(); assertTrue(curl.waitFor(10,TimeUnit.SECONDS)); } Files.deleteIfExists(curlBody); }
            var shortRead=h.store.signReadBatch(null,List.of(read(content,Duration.ofSeconds(2)))).get(0);
            while (Instant.now().isBefore(shortRead.expiresAt().plusMillis(1100))) Thread.sleep(100);
            var expired=http.send(HttpRequest.newBuilder(shortRead.url()).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.discarding());
            assertTrue("Expired bearer must be rejected",expired.statusCode()==400 || expired.statusCode()==403);
            assertEquals(content,h.store.stat(null,content.location()).orElseThrow());
        }
    }

    @Test public void credentialExpiryCapsUrlAndUnknownSessionExpiryFailsExplicitly() throws Exception {
        AtomicReference<AwsCredentials> signing=new AtomicReference<>(basic());
        try (Harness h=new Harness(true,null,signing::get)) {
            var content=h.write(h.store.createUpload(null,"expiry"),new byte[]{1}); Instant expiry=Instant.now().plusSeconds(40);
            signing.set(new ExpiringCredentials(basic().accessKeyId(),basic().secretAccessKey(),expiry));
            var signed=h.store.signReadBatch(null,List.of(read(content,Duration.ofDays(1)))).get(0);
            assertFalse(signed.expiresAt().isAfter(expiry)); assertTrue(signed.expiresAt().isAfter(Instant.now()));
            signing.set(AwsSessionCredentials.create(basic().accessKeyId(),basic().secretAccessKey(),"unknown-test-session"));
            assertEquals(BLOB_SIGN_FAILED,assertThrows(BlobException.class,() -> h.store.signReadBatch(null,List.of(read(content,TTL)))).code());
            signing.set(new ExpiringCredentials(basic().accessKeyId(),basic().secretAccessKey(),Instant.now().minusSeconds(1)));
            assertEquals(BLOB_SIGN_FAILED,assertThrows(BlobException.class,() -> h.store.signReadBatch(null,List.of(read(content,TTL)))).code());
        }
    }

    @Test public void signingUsesOneCredentialSnapshotEvenWhenTheProviderRotates() throws Exception {
        AtomicInteger resolutions=new AtomicInteger();
        AwsCredentialsProvider rotating=() -> resolutions.getAndIncrement()==0?basic():AwsBasicCredentials.create("changed-test-identity","changed-test-secret");
        try (Harness h=new Harness(true,null,rotating)) {
            var content=h.write(h.store.createUpload(null,"rotation"),new byte[]{1,2,3});
            var signed=h.store.signReadBatch(null,List.of(read(content,TTL))).get(0);
            assertEquals(1,resolutions.get());
            var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(
                    HttpRequest.newBuilder(signed.url()).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200,response.statusCode()); assertArrayEquals(new byte[]{1,2,3},response.body());
        }
    }

    @Test public void absentObjectsAndMissingBucketHaveDifferentFailureSemantics() throws Exception {
        try (Harness h=new Harness()) {
            assertTrue(h.store.stat(null,h.store.createUpload(null,"missing").location()).isEmpty());
            var content=h.write(h.store.createUpload(null,"removed-version"),new byte[]{1});
            h.backend.client.deleteObject(b -> b.bucket(h.backend.bucket).key(content.location()).versionId(content.contentVersion()));
            assertEquals(BLOB_NOT_FOUND,assertThrows(BlobException.class,() -> h.store.openRange(null,content.location(),content.contentVersion(),0,0)).code());
            assertEquals(BLOB_REFERENCE_INVALID,assertThrows(BlobException.class,() -> h.store.openRange(null,h.store.createUpload(null,"missing").location(),"null",0,0)).code());
        }
        String bucket="teaql-blob-empty-"+UUID.randomUUID();
        try (var backend=new S3BackendIT.Fixture()) {
            backend.client.createBucket(b -> b.bucket(bucket));
            try (S3BlobStore store=S3BlobStore.connect(endpoint(),null,Region.US_EAST_1,StaticCredentialsProvider.create(basic()),bucket,"owned/",temporary.getRoot().toPath().resolve("unversioned"),TTL,true)) {
                assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,store::validateReady).code());
                assertNull(backend.client.getBucketVersioning(b -> b.bucket(bucket)).status());
                backend.client.putBucketVersioning(b -> b.bucket(bucket).versioningConfiguration(v -> v.status(BucketVersioningStatus.ENABLED)));
                store.validateReady(); backend.client.deleteBucket(b -> b.bucket(bucket));
                assertEquals(BLOB_STORE_FAILED,assertThrows(BlobException.class,() -> store.stat(null,store.createUpload(null,"missing").location())).code());
            } finally {
                try { backend.client.deleteBucket(b -> b.bucket(bucket)); } catch (S3Exception gone) { if (gone.statusCode()!=404) throw gone; }
            }
        }
    }

    private static BlobStore.ReadRequest read(BlobStore.StoredContent content,Duration ttl) {
        return new BlobStore.ReadRequest(content.location(),content.contentVersion(),"video/mp4","申请.mp4",BlobPolicyProfile.Disposition.INLINE,ttl,"private, max-age=3600");
    }
    private static AwsBasicCredentials basic() { return AwsBasicCredentials.create(required("TEAQL_BLOB_S3_ACCESS_KEY"),required("TEAQL_BLOB_S3_SECRET_KEY")); }
    private record ExpiringCredentials(String accessKeyId,String secretAccessKey,Instant expiry) implements AwsCredentials {
        @Override public Optional<Instant> expirationTime() { return Optional.of(expiry); }
        @Override public String toString() { return "TestCredentials[redacted]"; }
    }
    private static URI endpoint() { return URI.create(required("TEAQL_BLOB_S3_ENDPOINT")); }
    private static String required(String name) { String value=System.getenv(name); if (value==null || value.isBlank()) throw new IllegalStateException("Missing test configuration: "+name); return value; }
    private final class Harness implements AutoCloseable {
        final S3BackendIT.Fixture backend=new S3BackendIT.Fixture();
        final Path scratch=temporary.getRoot().toPath().resolve("scratch-"+UUID.randomUUID());
        final S3BlobStore store;
        Harness() { this(true,null,null); }
        Harness(boolean ready,ExecutionInterceptor interceptor,AwsCredentialsProvider signingCredentials) {
            var client=S3Client.builder().endpointOverride(endpoint()).region(Region.US_EAST_1).credentialsProvider(StaticCredentialsProvider.create(basic())).forcePathStyle(true)
                    .httpClientBuilder(Apache5HttpClient.builder().connectionTimeout(Duration.ofSeconds(5)).socketTimeout(Duration.ofSeconds(15)))
                    .overrideConfiguration(configuration -> { configuration.apiCallTimeout(Duration.ofSeconds(30)); if (interceptor!=null) configuration.addExecutionInterceptor(interceptor); }).build();
            var identity=signingCredentials==null?StaticCredentialsProvider.create(basic()):signingCredentials;
            var presigner=S3Presigner.builder().endpointOverride(endpoint()).region(Region.US_EAST_1).credentialsProvider(identity).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
            store=new S3BlobStore(client,presigner,identity,backend.bucket,backend.prefix,scratch,TTL);
            if (ready) store.validateReady();
        }
        BlobStore.StoredContent write(BlobStore.UploadTarget target,byte[] bytes) { return store.write(null,target,new ByteArrayInputStream(bytes),Math.max(1,bytes.length)); }
        PutObjectResponse replace(BlobStore.UploadTarget target,byte[] bytes) {
            var original=backend.client.headObject(b -> b.bucket(backend.bucket).key(target.location()));
            return backend.client.putObject(b -> b.bucket(backend.bucket).key(target.location()).metadata(original.metadata()).contentType(original.contentType()),RequestBody.fromBytes(bytes));
        }
        void assertScratchEmpty() throws IOException {
            if (Files.exists(scratch)) try (var files=Files.list(scratch)) { assertEquals(Set.of("staging.format","staging.guard"),files.map(p -> p.getFileName().toString()).collect(java.util.stream.Collectors.toSet())); }
        }
        @Override public void close() { try { store.close(); } finally { backend.close(); } }
    }
    private static final class OwnedInput extends ByteArrayInputStream {
        int consumed; boolean closed;
        OwnedInput(byte[] bytes) { super(bytes); }
        @Override public synchronized int read(byte[] bytes,int offset,int length) { int n=super.read(bytes,offset,length); if (n>0) consumed+=n; return n; }
        @Override public void close() { closed=true; }
    }
}
