package io.teaql.blob.reference;

import io.teaql.blob.content.tika.TikaBlobContentVerifier;
import io.teaql.blob.store.s3.S3BlobStore;
import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import io.teaql.runtime.DefaultUserContext;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.blob.BlobRuntime.Maintenance.Status.*;
import static io.teaql.core.blob.BlobException.Code.*;
import static org.junit.Assert.*;

/** Actual PostgreSQL + versioned S3 + bounded Tika + native business participant. */
public class BlobS3IT {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private static final List<String> FORMATS=List.of("jpg","jpeg","png","mp4","mp3","doc","docx","xls","xlsx","pdf");
    private static final BlobFieldTarget TARGET=new BlobFieldTarget("Asset",null,"document");

    @Test public void tenFormatsAreVerifiedAndBoundInNativeSqlWithUsableFullUrls() throws Exception {
        try (Harness h=new Harness()) {
            long id=40;
            for (String extension:FORMATS) {
                var session=h.begin("real-"+extension,extension); byte[] bytes=file(extension);
                h.uploads.write(h.f.context(),session.uploadId(),new ByteArrayInputStream(bytes));
                assertEquals(BlobState.INITIATED,h.f.find(session.reference()).state());
                assertEquals(BLOB_UPLOAD_INCOMPLETE,assertThrows(BlobException.class,() -> h.f.create(900,session.reference())).code());
                assertEquals(session.reference(),h.uploads.complete(h.f.context(),session.uploadId()));
                var blob=h.f.find(session.reference()); assertEquals(BlobState.UPLOADED_UNBOUND,blob.state());
                h.f.create(++id,session.reference()); h.f.assertOracle(id,session.reference());
                assertEquals(BlobState.AVAILABLE,h.f.find(session.reference()).state());
                var request=new BlobStore.ReadRequest(blob.storageLocation(),blob.contentVersion(),blob.contentType(),blob.originalName(),
                        BlobPolicyProfile.Disposition.ATTACHMENT,Duration.ofSeconds(30),"private, no-store");
                var signed=h.store.signReadBatch(h.f.context(),List.of(request)).get(0);
                var response=(HttpURLConnection)signed.url().toURL().openConnection(); response.setConnectTimeout(5000); response.setReadTimeout(10000);
                try {
                    assertEquals(200,response.getResponseCode());
                    try (var body=response.getInputStream()) { assertArrayEquals(bytes,body.readAllBytes()); }
                    assertEquals(blob.contentType(),response.getHeaderField("Content-Type")); assertEquals("private, no-store",response.getHeaderField("Cache-Control"));
                } finally { response.disconnect(); }
                assertEquals(session.reference(),h.uploads.complete(h.f.context(),session.uploadId()));
            }
            assertEquals(10L,h.f.scalar("SELECT count(*) FROM teaql_blob_binding WHERE blob_key IS NOT NULL"));
            assertEquals(10L,h.f.scalar("SELECT count(*) FROM asset_data")); h.assertScratchEmpty();
        }
    }

    @Test public void rollbackCommittedAuditFailureReplacementAndLastUnbindRespectRetention() throws Exception {
        try (Harness h=new Harness()) {
            BlobRef first=h.upload("first"),next=h.upload("next"); h.f.create(42,first); h.f.create(43,first);
            var asset=h.f.load(42); asset.updateProperty("document",next); h.f.failure="after-apply";
            assertThrows(IllegalStateException.class,() -> h.f.save(asset)); h.f.assertOracle(42,first);
            assertEquals(BlobState.UPLOADED_UNBOUND,h.f.find(next).state());
            h.f.failure="audit"; assertEquals(io.teaql.core.TransactionCompletion.Outcome.COMMITTED,
                    assertThrows(io.teaql.core.TransactionCompletionException.class,() -> h.f.save(asset)).outcome()); h.f.assertOracle(42,next);
            assertEquals(BlobState.AVAILABLE,h.f.find(next).state()); h.f.failure="";
            assertNull(h.f.find(first).unreferencedAt());
            var last=h.f.load(43); last.updateProperty("document",null); h.f.save(last); h.f.assertOracle(43,null);
            h.f.clock.now=NOW.plusSeconds(19); assertTrue(h.maintenance(h.store).runBatch(h.context()).outcomes().isEmpty());
            h.f.clock.now=NOW.plusSeconds(20); var deleted=h.maintenance(h.store).runBatch(h.context());
            assertEquals(1,deleted.outcomes().size()); assertEquals(DELETED,deleted.outcomes().get(0).status()); h.assertDeleted(first);
            h.f.assertOracle(42,next); assertTrue(h.store.stat(h.f.context(),h.f.find(next).storageLocation()).isPresent());
            assertTrue(h.maintenance(h.store).runBatch(h.context()).outcomes().isEmpty()); h.assertScratchEmpty();
        }
    }

    @Test public void abandonedUnrecordedAndMissingUploadsAreSealedWithoutClaimingVerification() throws Exception {
        try (Harness h=new Harness()) {
            var written=h.begin("unrecorded","pdf"); var absent=h.begin("never-published","pdf");
            var raw=h.store.write(h.f.context(),h.store.createUpload(h.f.context(),written.uploadId()),new ByteArrayInputStream(file("pdf")),100000);
            assertNull(h.f.find(written.reference()).contentVersion());
            h.f.clock.now=NOW.plusSeconds(5); var batch=h.maintenance(h.store).runBatch(h.context());
            assertEquals(Set.of(DELETED,ALREADY_MISSING),new HashSet<>(batch.outcomes().stream().map(BlobMaintenanceService.Outcome::status).toList()));
            h.assertDeleted(written.reference()); h.assertDeleted(absent.reference());
            assertEquals(raw.contentVersion(),h.f.find(written.reference()).contentVersion());
            assertNull(h.f.find(written.reference()).contentType()); assertNull(h.f.find(written.reference()).completedAt());
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> h.store.write(h.f.context(),h.store.createUpload(h.f.context(),absent.uploadId()),new ByteArrayInputStream(new byte[]{1}),10)).code());
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> h.uploads.write(h.f.context(),written.uploadId(),new ByteArrayInputStream(file("pdf")))).code());
            h.assertScratchEmpty();
        }
    }

    @Test public void failuresAfterRetirementAndAfterPhysicalDeleteRecoverFromCommittedClaim() throws Exception {
        for (boolean afterDelete:List.of(false,true)) {
            try (Harness h=new Harness()) {
                BlobRef key=h.upload("failure"); var original=h.f.find(key); h.f.clock.now=NOW.plusSeconds(7);
                var failing=new BlobStoreDelegate(h.store) {
                    @Override public DeleteResult deleteIfVersion(UserContext context,String location,String version) {
                        assertFalse(h.f.sql.hasActiveTransaction()); assertEquals("deleting",h.f.otherSql.queryForList("SELECT state FROM teaql_blob_object WHERE blob_key=?",new Object[]{key.key()}).get(0).get("state"));
                        if (afterDelete) super.deleteIfVersion(context,location,version);
                        throw new IllegalStateException("Test failed storage response");
                    }
                };
                assertEquals(FAILED,h.maintenance(failing).runBatch(h.context()).outcomes().get(0).status());
                assertEquals(BlobState.DELETING,h.f.find(key).state()); assertNull(h.f.find(key).deletedAt());
                assertEquals("retired-v1",h.admin.headObject(b -> b.bucket(h.bucket).key(original.storageLocation())).metadata().get("teaql-blob-state"));
                var retry=h.maintenance(h.store).runBatch(h.context()); assertEquals(afterDelete?ALREADY_MISSING:DELETED,retry.outcomes().get(0).status());
                h.assertDeleted(key); h.assertScratchEmpty();
            }
        }
    }

    @Test public void staleCatalogVersionCannotDeleteExternallyReplacedSameByteGeneration() throws Exception {
        try (Harness h=new Harness()) {
            BlobRef key=h.upload("stale"); var original=h.f.find(key); byte[] bytes=file("pdf");
            var metadata=h.admin.headObject(b -> b.bucket(h.bucket).key(original.storageLocation()));
            var replacement=h.admin.putObject(b -> b.bucket(h.bucket).key(original.storageLocation()).metadata(metadata.metadata()),RequestBody.fromBytes(bytes));
            h.f.clock.now=NOW.plusSeconds(7); var result=h.maintenance(h.store).runBatch(h.context());
            assertEquals(VERSION_CONFLICT,result.outcomes().get(0).status()); assertEquals(BlobState.DELETING,h.f.find(key).state());
            assertEquals(original.contentVersion(),h.f.find(key).contentVersion()); assertNull(h.f.find(key).deletedAt());
            assertEquals(replacement.versionId(),h.admin.headObject(b -> b.bucket(h.bucket).key(original.storageLocation())).versionId());
            assertArrayEquals(bytes,h.admin.getObjectAsBytes(b -> b.bucket(h.bucket).key(original.storageLocation()).versionId(original.contentVersion())).asByteArray());
        }
    }

    @Test public void revokedMaintenanceAfterSealKeepsPayloadAndRequiresFreshAuthorization() throws Exception {
        try (Harness h=new Harness()) {
            BlobRef key=h.upload("revoke"); var original=h.f.find(key); var context=h.context(); h.f.clock.now=NOW.plusSeconds(7); AtomicBoolean first=new AtomicBoolean(true);
            var revoking=new BlobStoreDelegate(h.store) {
                @Override public Optional<StoredContent> sealForDeletion(UserContext actor,String location,String version) {
                    var content=super.sealForDeletion(actor,location,version); if (first.getAndSet(false)) actor.putAttribute("maintain",Boolean.FALSE); return content;
                }
            };
            assertEquals(BLOB_MAINTENANCE_DENIED,assertThrows(BlobException.class,() -> h.maintenance(revoking).runBatch(context)).code());
            assertEquals(BlobState.DELETING,h.f.find(key).state());
            assertArrayEquals(file("pdf"),h.admin.getObjectAsBytes(b -> b.bucket(h.bucket).key(original.storageLocation()).versionId(original.contentVersion())).asByteArray());
            assertEquals(DELETED,h.maintenance(h.store).runBatch(h.context()).outcomes().get(0).status()); h.assertDeleted(key);
        }
    }

    @Test public void publicModuleProvidesAuthorizedFullS3UrlsForNativeCommittedBindings() throws Exception {
        try (Harness h=new Harness()) {
            var scopes=(BlobBindingAuthorization.ScopeProvider)actor -> actor.getAttribute("scope",BlobScope.class);
            var module=BlobReferenceModule.builder().catalog(h.f.catalog).store(h.store).clock(Clock.systemUTC())
                    .verifier(new TikaBlobContentVerifier(h.root.resolve("module-parser"),TikaBlobContentVerifier.Limits.defaults()))
                    .uploadAuthorization(new BlobUploadAuthorization(scopes,(actor,target) -> true,(actor,target,action,info) -> true))
                    .bindingAuthorization(new BlobBindingAuthorization(scopes,(actor,target,kind) -> true,(actor,target,blob) -> true))
                    .readAuthorization(new BlobReadAuthorization(scopes,(actor,target) -> true,(actor,target,blob) -> true))
                    .maintenancePermission((actor,scope) -> false).build();
            try (var runtime=module.installInto(io.teaql.runtime.TeaQLRuntime.builder().metadata(h.f.runtime.getMetadata())
                    .registry(h.f.runtime.getRegistry()).telemetry(io.teaql.runtime.RuntimeTelemetry.NOOP).executionLogging(false)).build()) {
                runtime.install(io.teaql.core.RuntimeModule.of().withRequiredCapabilities("blob.v1")); runtime.validateReadiness();
                var actor=new DefaultUserContext(runtime); actor.putAttribute("scope",SCOPE); BlobRuntime blobs=actor.capability(BlobRuntime.class);
                var session=blobs.upload().begin(actor,TARGET,new BlobRuntime.Uploads.FileInfo("application.pdf",null,null),"module-native");
                blobs.upload().write(actor,session.uploadId(),new ByteArrayInputStream(file("pdf"))); BlobRef key=blobs.upload().complete(actor,session.uploadId());
                var entity=h.f.newAsset(42,key); runtime.saveGraph(actor,entity); h.f.assertOracle(42,key);
                Instant deadline=Instant.now().plusSeconds(15);
                var read=blobs.reads().readBatch(actor,List.of(new BlobRuntime.Reads.Request(new BlobFieldTarget("Asset",42L,"document"),key)),
                        new BlobRuntime.Reads.Options(Duration.ofSeconds(20),deadline,false)).get(0);
                assertFalse(read.expiresAt().isAfter(deadline)); assertTrue(read.url().getRawQuery().contains("versionId="));
                var connection=(HttpURLConnection)read.url().toURL().openConnection(); connection.setConnectTimeout(5000); connection.setReadTimeout(5000); connection.setRequestProperty("Range","bytes=2-7");
                try (var bytes=connection.getInputStream()) { assertEquals(206,connection.getResponseCode()); assertArrayEquals(Arrays.copyOfRange(file("pdf"),2,8),bytes.readAllBytes()); }
                finally { connection.disconnect(); }
                assertEquals(key,entity.document); assertTrue(entity.getEntityMutationLedger().currentChangeSet().isEmpty()); h.assertScratchEmpty();
            }
        }
    }

    private static byte[] file(String extension) throws IOException { return Files.readAllBytes(Path.of(required("TEAQL_BLOB_TEST_FIXTURES")).resolve("sample."+extension)); }
    private static String required(String name) { String value=System.getenv(name); if (value==null || value.isBlank()) throw new IllegalStateException("Missing test configuration: "+name); return value; }
    private final class Harness implements AutoCloseable {
        final String bucket=required("TEAQL_BLOB_S3_BUCKET"),prefix="sql-integration/"+UUID.randomUUID()+"/";
        final Path root=temporary.getRoot().toPath().resolve(UUID.randomUUID().toString());
        final Fixture f;
        final S3Client admin;
        final S3BlobStore store;
        final BlobUploadService uploads;
        Harness() throws Exception {
            var credentials=StaticCredentialsProvider.create(AwsBasicCredentials.create(required("TEAQL_BLOB_S3_ACCESS_KEY"),required("TEAQL_BLOB_S3_SECRET_KEY")));
            URI endpoint=URI.create(required("TEAQL_BLOB_S3_ENDPOINT"));
            admin=S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(credentials).forcePathStyle(true).httpClientBuilder(Apache5HttpClient.builder())
                    .overrideConfiguration(options -> options.apiCallTimeout(Duration.ofSeconds(30))).build();
            try { admin.headBucket(b -> b.bucket(bucket)); } catch (S3Exception missing) { if (missing.statusCode()!=404) throw missing; admin.createBucket(b -> b.bucket(bucket)); }
            // Explicit test provisioning, outside production adapter readiness.
            admin.putBucketVersioning(b -> b.bucket(bucket).versioningConfiguration(v -> v.status(BucketVersioningStatus.ENABLED)));
            store=S3BlobStore.connect(endpoint,null,Region.US_EAST_1,credentials,bucket,prefix,root.resolve("s3-staging"),Duration.ofMinutes(10),true); store.validateReady();
            var policies=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").uploadSessionTtl(Duration.ofSeconds(5))
                    .unboundRetention(Duration.ofSeconds(7)).unreferencedRetention(Duration.ofSeconds(11)).minimumRetention(Duration.ofSeconds(20)).build(),Map.of(),Map.of());
            f=new Fixture(true,policies,FORMATS); var verifier=new TikaBlobContentVerifier(root.resolve("parser-staging"),TikaBlobContentVerifier.Limits.defaults());
            uploads=new BlobUploadService(f.catalog,store,verifier,policies,new BlobUploadAuthorization(
                    actor -> actor.getAttribute("scope",BlobScope.class),(actor,target) -> !Boolean.FALSE.equals(actor.getAttribute("field")),
                    (actor,target,action,info) -> !Boolean.FALSE.equals(actor.getAttribute("policy"))),f.clock);
        }
        DefaultUserContext context() { var context=f.context(); context.putAttribute("maintain",Boolean.TRUE); return context; }
        BlobUploadService.Session begin(String request,String extension) { return uploads.begin(f.context(),TARGET,new BlobUploadService.FileInfo("application."+extension,null,"text/plain"),request); }
        BlobRef upload(String request) throws IOException { var session=begin(request,"pdf"); uploads.write(f.context(),session.uploadId(),new ByteArrayInputStream(file("pdf"))); return uploads.complete(f.context(),session.uploadId()); }
        BlobMaintenanceService maintenance(BlobStore transport) { return new BlobMaintenanceService(f.catalog,transport,actor -> actor.getAttribute("scope",BlobScope.class),(actor,scope) -> Boolean.TRUE.equals(actor.getAttribute("maintain")),f.clock); }
        void assertDeleted(BlobRef reference) {
            var blob=f.find(reference); assertEquals(BlobState.DELETED,blob.state()); assertNotNull(blob.deletedAt()); assertTrue(store.stat(f.context(),blob.storageLocation()).isEmpty());
            assertEquals("deleted",f.otherSql.queryForList("SELECT state FROM teaql_blob_object WHERE blob_key=?",new Object[]{reference.key()}).get(0).get("state"));
            assertEquals("retired-v1",admin.headObject(b -> b.bucket(bucket).key(blob.storageLocation())).metadata().get("teaql-blob-state"));
            if (blob.contentVersion()!=null) assertEquals(404,assertThrows(S3Exception.class,() -> admin.headObject(b -> b.bucket(bucket).key(blob.storageLocation()).versionId(blob.contentVersion()))).statusCode());
        }
        void assertScratchEmpty() throws IOException {
            for (String name:List.of("s3-staging","parser-staging")) {
                Path directory=root.resolve(name); if (Files.exists(directory)) try (var entries=Files.list(directory)) { assertEquals(Set.of("staging.format","staging.guard"),entries.map(p -> p.getFileName().toString()).collect(java.util.stream.Collectors.toSet())); }
            }
        }
        @Override public void close() {
            try { f.close(); } finally {
                try { store.close(); } finally {
                    try {
                        for (var page:admin.listObjectVersionsPaginator(b -> b.bucket(bucket).prefix(prefix))) {
                            for (var version:page.versions()) admin.deleteObject(b -> b.bucket(bucket).key(version.key()).versionId(version.versionId()));
                            for (var marker:page.deleteMarkers()) admin.deleteObject(b -> b.bucket(bucket).key(marker.key()).versionId(marker.versionId()));
                        }
                    } finally { admin.close(); }
                }
            }
        }
    }
}
