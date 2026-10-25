package io.teaql.blob.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.blob.content.tika.TikaBlobContentVerifier;
import io.teaql.blob.store.file.FileBlobStore;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.core.blob.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.blob.BlobException.Code.*;
import static org.junit.Assert.*;

/** Explicit real-PostgreSQL + private file store + actual content verifier integration gate. */
public class BlobUploadIT {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private static final List<String> EXTENSIONS=List.of("jpg","jpeg","png","mp4","mp3","doc","docx","xls","xlsx","pdf");
    private static final BlobFieldTarget TARGET=new BlobFieldTarget("Asset",null,"document");

    @Test public void allTenActualFormatsRequireConfirmationBeforeNativeSqlBinding() throws Exception {
        try (UploadFixture u=new UploadFixture(BlobPolicies.defaults(),EXTENSIONS)) {
            long id=41;
            for (String extension:EXTENSIONS) {
                id++; byte[] bytes=file(extension); DefaultUserContext context=u.f.context();
                var session=u.service.begin(context,TARGET,new BlobUploadService.FileInfo("C:\\client\\application."+extension,1L,"text/plain"),"all-"+extension);
                BlobRef key=session.reference();
                assertEquals(BlobState.INITIATED,u.f.find(key).state());
                assertEquals("application."+extension,u.f.find(key).originalName());
                assertNull(session.directUploadUrl()); assertNull(session.directUrlExpiresAt());
                var asset=u.f.newAsset(id,key);
                assertEquals(BLOB_UPLOAD_INCOMPLETE,assertThrows(BlobException.class,() -> u.f.save(asset)).code());
                var source=new OwnedInput(bytes);
                assertEquals(bytes.length,u.service.write(context,session.uploadId(),source).sizeBytes()); assertFalse(source.closed);
                assertEquals(BlobState.INITIATED,u.f.find(key).state()); assertNull(u.f.find(key).contentType());
                assertEquals(BLOB_UPLOAD_INCOMPLETE,assertThrows(BlobException.class,() -> u.f.save(asset)).code());
                assertEquals(key,u.service.complete(context,session.uploadId()));
                var verified=u.f.find(key);
                assertEquals(BlobState.UPLOADED_UNBOUND,verified.state());
                assertEquals(Long.valueOf(bytes.length),verified.sizeBytes());
                assertEquals(BlobFieldSpec.of("Asset","document",List.of(extension),true,"default").allowedContentTypes().first(),verified.contentType());
                assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),verified.sha256());
                u.f.save(asset); u.f.assertOracle(id,key);
                assertEquals(BlobState.AVAILABLE,u.f.find(key).state());
                assertEquals(key,u.service.complete(u.f.context(),session.uploadId()));
            }
            assertEquals(10L,u.f.scalar("SELECT count(*) FROM teaql_blob_binding WHERE blob_key IS NOT NULL"));
            assertEquals(10L,u.f.scalar("SELECT count(*) FROM asset_data"));
            u.assertStagesEmpty();
        }
    }

    @Test public void sharedUploadCasesUseActualBoundedBytesAndContainers() throws Throwable {
        var cases=new ObjectMapper().readTree(fixtures().getParent().resolve("upload-cases.json").toFile()).path("cases");
        assertEquals(7,cases.size());
        for (var test:cases) {
            List<String> extensions=new ArrayList<>(); test.path("allowedExtensions").forEach(value -> extensions.add(value.asText()));
            BlobPolicies policies=policies(BlobPolicyProfile.named("default").maxSizeBytes(test.path("maxSizeBytes").asLong()).build());
            try (UploadFixture u=new UploadFixture(policies,extensions)) {
                byte[] bytes=Files.readAllBytes(fixtures().getParent().resolve(test.path("path").asText()));
                var info=new BlobUploadService.FileInfo(test.path("fileName").asText(),null,test.path("declaredContentType").asText());
                BlobUploadService.Session[] begun=new BlobUploadService.Session[1];
                org.junit.function.ThrowingRunnable upload=() -> {
                    begun[0]=u.service.begin(u.f.context(),TARGET,info,test.path("id").asText());
                    u.service.write(u.f.context(),begun[0].uploadId(),new ByteArrayInputStream(bytes));
                    u.service.complete(u.f.context(),begun[0].uploadId());
                };
                if (test.has("error")) {
                    assertEquals(test.path("id").asText(),test.path("error").asText(),assertThrows(BlobException.class,upload).code().name());
                    if (begun[0]!=null) assertEquals(BlobState.INITIATED,u.f.find(begun[0].reference()).state());
                } else {
                    upload.run(); assertEquals(BlobState.UPLOADED_UNBOUND,u.f.find(begun[0].reference()).state());
                    assertEquals(Long.valueOf(bytes.length),u.f.find(begun[0].reference()).sizeBytes());
                }
                assertEquals(0L,u.f.scalar("SELECT count(*) FROM teaql_blob_binding")); u.assertStagesEmpty();
            }
        }
    }

    @Test public void retriesPreserveKeyVersionPolicyAndOriginalExpiry() throws Exception {
        try (UploadFixture u=new UploadFixture(BlobPolicies.defaults(),List.of("pdf"))) {
            var context=u.f.context(); byte[] bytes=file("pdf");
            var info=new BlobUploadService.FileInfo("/client/申请.pdf",null," APPLICATION/PDF ");
            var first=u.service.begin(context,TARGET,info,"retry");
            u.f.clock.now=NOW.plusSeconds(30);
            var retry=u.service.begin(u.f.context(),TARGET,new BlobUploadService.FileInfo("申请.pdf",null,"application/pdf"),"retry");
            assertEquals(first,retry); assertEquals(1L,u.f.scalar("SELECT count(*) FROM teaql_blob_object"));
            assertEquals(BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,() -> u.service.begin(context,TARGET,
                    new BlobUploadService.FileInfo("changed.pdf",null,"application/pdf"),"retry")).code());
            assertEquals(BLOB_UPLOAD_INCOMPLETE,assertThrows(BlobException.class,() -> u.service.complete(context,first.uploadId())).code());
            u.service.write(context,first.uploadId(),new ByteArrayInputStream(bytes));
            String version=u.f.find(first.reference()).contentVersion();
            u.service.write(u.f.context(),first.uploadId(),new ByteArrayInputStream(bytes));
            assertEquals(version,u.f.find(first.reference()).contentVersion());
            byte[] changed=bytes.clone(); changed[changed.length-1]^=1;
            assertEquals(BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,
                    () -> u.service.write(context,first.uploadId(),new ByteArrayInputStream(changed))).code());
            assertEquals(first.reference(),u.service.complete(context,first.uploadId()));
            var complete=u.f.find(first.reference());
            u.f.clock.now=first.uploadExpiresAt().plusSeconds(1);
            assertEquals(first.reference(),u.service.complete(u.f.context(),first.uploadId()));
            assertEquals(complete.completedAt(),u.f.find(first.reference()).completedAt());
            assertEquals(first.uploadExpiresAt(),u.f.find(first.reference()).uploadExpiresAt());
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,
                    () -> u.service.write(context,first.uploadId(),new ByteArrayInputStream(bytes))).code());
            u.assertStagesEmpty();
        }
    }

    @Test public void eachStageRequiresCurrentScopeFieldPermissionAndAdditionalPolicy() throws Exception {
        try (UploadFixture u=new UploadFixture(BlobPolicies.defaults(),List.of("pdf"))) {
            byte[] bytes=file("pdf"); var info=new BlobUploadService.FileInfo("application.pdf",null,null);
            for (String denial:List.of("field","policy")) {
                var context=u.f.context(); context.putAttribute(denial,Boolean.FALSE);
                assertEquals(BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> u.service.begin(context,TARGET,info,"denied-"+denial)).code());
            }
            assertEquals(0L,u.f.scalar("SELECT count(*) FROM teaql_blob_object"));
            var session=u.service.begin(u.f.context(),TARGET,info,"auth");
            for (String denial:List.of("scope","field","policy")) {
                var context=denied(u.f.context(),denial); var source=new OwnedInput(bytes);
                assertEquals(BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> u.service.write(context,session.uploadId(),source)).code());
                assertEquals(0,source.reads); assertFalse(source.closed);
            }
            assertTrue(u.files.stat(u.f.context(),u.f.find(session.reference()).storageLocation()).isEmpty());
            u.service.write(u.f.context(),session.uploadId(),new ByteArrayInputStream(bytes));
            for (String denial:List.of("scope","field","policy")) {
                var context=denied(u.f.context(),denial);
                assertEquals(BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> u.service.complete(context,session.uploadId())).code());
                assertEquals(BlobState.INITIATED,u.f.find(session.reference()).state());
            }
            assertEquals(session.reference(),u.service.complete(u.f.context(),session.uploadId()));
            assertEquals(BLOB_BIND_DENIED,assertThrows(BlobException.class,
                    () -> u.service.complete(denied(u.f.context(),"field"),session.uploadId())).code());
            u.assertStagesEmpty();
        }
    }

    @Test public void revocationAndScopeChangesDuringVerificationNeverPromoteCatalog() throws Exception {
        try (UploadFixture u=new UploadFixture(BlobPolicies.defaults(),List.of("pdf"))) {
            byte[] bytes=file("pdf");
            for (String denial:List.of("scope","field","policy")) {
                var context=u.f.context(); var session=u.begin("revoke-"+denial);
                u.service.write(context,session.uploadId(),new ByteArrayInputStream(bytes));
                var revoking=u.service((source,extension,maximum,empty) -> {
                    var result=u.verifier.verify(source,extension,maximum,empty); denied(context,denial); return result;
                });
                assertEquals(BLOB_BIND_DENIED,assertThrows(BlobException.class,() -> revoking.complete(context,session.uploadId())).code());
                assertEquals(BlobState.INITIATED,u.f.find(session.reference()).state()); assertNull(u.f.find(session.reference()).completedAt());
                assertEquals(session.reference(),u.service.complete(u.f.context(),session.uploadId()));
            }
            u.assertStagesEmpty();
        }
    }

    @Test public void expiryBeforeAndDuringWriteOrVerificationLeavesNoBindableObject() throws Exception {
        BlobPolicies policies=policies(BlobPolicyProfile.named("default").uploadSessionTtl(Duration.ofSeconds(5)).build());
        try (UploadFixture u=new UploadFixture(policies,List.of("pdf"))) {
            byte[] bytes=file("pdf"); var context=u.f.context();
            var before=u.begin("before"); u.f.clock.now=before.uploadExpiresAt(); var unread=new OwnedInput(bytes);
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> u.service.write(context,before.uploadId(),unread)).code()); assertEquals(0,unread.reads);
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> u.service.complete(context,before.uploadId())).code());
            u.f.clock.now=NOW; var during=u.begin("during-write");
            var slow=new ByteArrayInputStream(bytes) {
                @Override public synchronized int read(byte[] buffer,int offset,int length) {
                    int n=super.read(buffer,offset,length); u.f.clock.now=during.uploadExpiresAt(); return n;
                }
            };
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> u.service.write(context,during.uploadId(),slow)).code());
            assertTrue(u.files.stat(context,u.f.find(during.reference()).storageLocation()).isEmpty());
            u.f.clock.now=NOW; var confirming=u.begin("during-confirm"); u.service.write(context,confirming.uploadId(),new ByteArrayInputStream(bytes));
            var slowVerifier=u.service((source,extension,maximum,empty) -> {
                var result=u.verifier.verify(source,extension,maximum,empty); u.f.clock.now=confirming.uploadExpiresAt(); return result;
            });
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> slowVerifier.complete(context,confirming.uploadId())).code());
            assertEquals(BlobState.INITIATED,u.f.find(confirming.reference()).state());
            assertEquals(0L,u.f.scalar("SELECT count(*) FROM teaql_blob_binding")); u.assertStagesEmpty();
        }
    }

    @Test public void rejectedIoAndEmptyContentKeepInitiatedAndDoNotLeakDetails() throws Exception {
        try (UploadFixture u=new UploadFixture(BlobPolicies.defaults(),List.of("pdf"))) {
            var context=u.f.context(); var failed=u.begin("io");
            var failure=assertThrows(BlobException.class,() -> u.service.write(context,failed.uploadId(),new InputStream() {
                @Override public int read() throws IOException { throw new IOException("PRIVATE-IO-CANARY"); }
            }));
            assertEquals(BLOB_STORE_FAILED,failure.code()); assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("CANARY"));
            assertTrue(u.files.stat(context,u.f.find(failed.reference()).storageLocation()).isEmpty());
            var empty=u.begin("empty"); u.service.write(context,empty.uploadId(),new ByteArrayInputStream(new byte[0]));
            assertEquals(BLOB_SIZE_EXCEEDED,assertThrows(BlobException.class,() -> u.service.complete(context,empty.uploadId())).code());
            assertEquals(BlobState.INITIATED,u.f.find(empty.reference()).state());
            assertEquals(0L,u.f.scalar("SELECT count(*) FROM teaql_blob_binding")); u.assertStagesEmpty();
        }
    }

    @Test public void concurrentBeginWriteAndConfirmRetriesHaveOneImmutableCatalogObject() throws Exception {
        try (UploadFixture u=new UploadFixture(BlobPolicies.defaults(),List.of("pdf"))) {
            ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1); byte[] bytes=file("pdf");
            try {
                List<Future<BlobUploadService.Session>> begun=new ArrayList<>();
                for (int i=0;i<2;i++) begun.add(pool.submit(() -> { assertTrue(start.await(5,TimeUnit.SECONDS)); return u.begin("concurrent"); }));
                start.countDown(); var first=begun.get(0).get(15,TimeUnit.SECONDS); assertEquals(first,begun.get(1).get(15,TimeUnit.SECONDS));
                List<Future<BlobRef>> completed=new ArrayList<>();
                for (int i=0;i<2;i++) completed.add(pool.submit(() -> {
                    u.service.write(u.f.context(),first.uploadId(),new ByteArrayInputStream(bytes));
                    return u.service.complete(u.f.context(),first.uploadId());
                }));
                for (var result:completed) assertEquals(first.reference(),result.get(20,TimeUnit.SECONDS));
                assertEquals(1L,u.f.scalar("SELECT count(*) FROM teaql_blob_object"));
                u.f.create(42,first.reference()); u.f.assertOracle(42,first.reference()); u.assertStagesEmpty();
            } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void profileSnapshotPersistsWhileCurrentDeploymentLimitsStillApply() throws Exception {
        try (UploadFixture u=new UploadFixture(BlobPolicies.defaults(),List.of("pdf"))) {
            byte[] bytes=file("pdf"); var original=u.begin("snapshot");
            BlobPolicies tighterProfile=policies(BlobPolicyProfile.named("default").version(2).maxSizeBytes(10).build());
            var changed=u.service(u.verifier,tighterProfile);
            assertEquals(original,changed.begin(u.f.context(),TARGET,new BlobUploadService.FileInfo("application.pdf",null,null),"snapshot"));
            changed.write(u.f.context(),original.uploadId(),new ByteArrayInputStream(bytes));
            assertEquals(original.reference(),changed.complete(u.f.context(),original.uploadId()));
            assertEquals(1,u.f.find(original.reference()).policy().version());
            assertEquals(BlobPolicies.DEFAULT_MAX_SIZE_BYTES,u.f.find(original.reference()).policy().maxSizeBytes());
            var limits=new BlobPolicies.Limits(10,Duration.ofHours(1),BlobPolicies.FORMATS,Set.of());
            var reduced=u.service(u.verifier,new BlobPolicies(limits,BlobPolicyProfile.named("default").maxSizeBytes(10).build(),Map.of(),Map.of()));
            assertEquals(BLOB_SIZE_EXCEEDED,assertThrows(BlobException.class,() -> reduced.complete(u.f.context(),original.uploadId())).code());
            var disabled=new BlobPolicies.Limits(BlobPolicies.DEFAULT_MAX_SIZE_BYTES,Duration.ofHours(1),Set.of("png"),Set.of());
            var forbidden=u.service(u.verifier,new BlobPolicies(disabled,BlobPolicyProfile.named("default").build(),Map.of(),Map.of()));
            assertEquals(BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,() -> forbidden.complete(u.f.context(),original.uploadId())).code());
            assertEquals(BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,() -> forbidden.write(u.f.context(),original.uploadId(),new ByteArrayInputStream(bytes))).code());
            u.assertStagesEmpty();
        }
    }

    @Test public void changedStorageGenerationCannotCompleteOrReplayTheOriginalContent() throws Exception {
        try (UploadFixture u=new UploadFixture(BlobPolicies.defaults(),List.of("pdf"))) {
            var context=u.f.context(); byte[] bytes=file("pdf"),changed=bytes.clone(); changed[changed.length-1]^=1;
            for (boolean verified:List.of(false,true)) {
                var session=u.begin("generation-"+verified); u.service.write(context,session.uploadId(),new ByteArrayInputStream(bytes));
                if (verified) u.service.complete(context,session.uploadId());
                var original=u.f.find(session.reference());
                assertEquals(BlobStore.DeleteResult.DELETED,u.files.deleteIfVersion(context,original.storageLocation(),original.contentVersion()));
                var replacement=u.files.write(context,u.files.createUpload(context,session.uploadId()),new ByteArrayInputStream(changed),100000);
                assertNotEquals(original.contentVersion(),replacement.contentVersion());
                assertEquals(BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,() -> u.service.complete(context,session.uploadId())).code());
                assertEquals(original.state(),u.f.find(session.reference()).state());
                assertEquals(original.contentVersion(),u.f.find(session.reference()).contentVersion());
            }
            assertEquals(0L,u.f.scalar("SELECT count(*) FROM teaql_blob_binding")); u.assertStagesEmpty();
        }
    }

    private static DefaultUserContext denied(DefaultUserContext context,String denial) {
        if (denial.equals("scope")) context.putAttribute("scope",new BlobScope("test","tenant-b")); else context.putAttribute(denial,Boolean.FALSE);
        return context;
    }
    private static BlobPolicies policies(BlobPolicyProfile profile) { return new BlobPolicies(BlobPolicies.Limits.defaults(),profile,Map.of(),Map.of()); }
    private static Path fixtures() {
        String path=System.getenv("TEAQL_BLOB_TEST_FIXTURES"); if (path==null) throw new IllegalStateException("Real fixtures missing"); return Path.of(path);
    }
    private static byte[] file(String extension) throws IOException { return Files.readAllBytes(fixtures().resolve("sample."+extension)); }
    private static final class OwnedInput extends ByteArrayInputStream {
        boolean closed; int reads;
        OwnedInput(byte[] bytes) { super(bytes); }
        @Override public synchronized int read(byte[] bytes,int offset,int length) { reads++; return super.read(bytes,offset,length); }
        @Override public void close() { closed=true; }
    }
    private final class UploadFixture implements AutoCloseable {
        final Fixture f;
        final Path root;
        final FileBlobStore files;
        final TikaBlobContentVerifier verifier;
        final BlobUploadService service;
        UploadFixture(BlobPolicies policies,List<String> extensions) throws Exception {
            root=temporary.newFolder().toPath(); files=new FileBlobStore(root.resolve("objects")); files.initialize();
            verifier=new TikaBlobContentVerifier(root.resolve("staging"),TikaBlobContentVerifier.Limits.defaults());
            f=new Fixture(true,policies,extensions); service=service(verifier);
        }
        BlobUploadService service(BlobContentVerifier selected) { return service(selected,f.policies); }
        BlobUploadService service(BlobContentVerifier selected,BlobPolicies policies) {
            return new BlobUploadService(f.catalog,files,selected,policies,new BlobUploadAuthorization(
                    context -> context.getAttribute("scope",BlobScope.class),
                    (context,target) -> !Boolean.FALSE.equals(context.getAttribute("field")),
                    (context,target,action,info) -> !Boolean.FALSE.equals(context.getAttribute("policy"))),f.clock);
        }
        BlobUploadService.Session begin(String request) { return service.begin(f.context(),TARGET,new BlobUploadService.FileInfo("application.pdf",null,null),request); }
        void assertStagesEmpty() throws IOException {
            if (Files.exists(root.resolve("staging"))) try (var paths=Files.list(root.resolve("staging"))) { assertEquals(0,paths.count()); }
            try (var paths=Files.list(root.resolve("objects/tmp"))) { assertEquals(0,paths.count()); }
        }
        @Override public void close() { f.close(); }
    }
}
