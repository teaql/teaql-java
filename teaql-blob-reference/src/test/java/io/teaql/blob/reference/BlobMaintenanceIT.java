package io.teaql.blob.reference;

import io.teaql.blob.content.tika.TikaBlobContentVerifier;
import io.teaql.blob.store.file.FileBlobStore;
import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import io.teaql.runtime.DefaultUserContext;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.blob.BlobException.Code.*;
import static io.teaql.blob.reference.BlobMaintenanceService.Status.*;
import static org.junit.Assert.*;

/** Explicit real database + immutable file provider collection and failure-recovery gate. */
public class BlobMaintenanceIT {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private static final BlobFieldTarget TARGET=new BlobFieldTarget("Asset",null,"document");
    private static BlobPolicies policies() {
        return new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default")
                .uploadSessionTtl(Duration.ofSeconds(5)).unboundRetention(Duration.ofSeconds(7))
                .unreferencedRetention(Duration.ofSeconds(11)).minimumRetention(Duration.ofSeconds(20)).build(),Map.of(),Map.of());
    }

    @Test public void uploadUnboundAndLastUnbindDeadlinesRemainIndependentAndReplayable() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            var empty=u.begin("empty"); var written=u.begin("written"); u.write(written);
            BlobRef unbound=u.upload("unbound"),shared=u.upload("shared"),old=u.upload("old"),replacement=u.upload("replacement");
            u.f.create(42,shared); u.f.create(43,shared); var asset=u.f.create(44,old);
            asset.updateProperty("document",replacement); u.f.save(asset);
            assertTrue(u.maintenance().runBatch(u.context()).outcomes().isEmpty());
            u.f.clock.now=NOW.plusSeconds(5);
            var first=u.maintenance().runBatch(u.context()); assertEquals(2,first.outcomes().size());
            assertEquals(Set.of(DELETED,ALREADY_MISSING),new HashSet<>(first.outcomes().stream().map(BlobMaintenanceService.Outcome::status).toList()));
            u.assertDeleted(empty.reference()); u.assertDeleted(written.reference());
            assertEquals(BlobState.UPLOADED_UNBOUND,u.f.find(unbound).state());
            u.f.clock.now=NOW.plusSeconds(7); assertEquals(1,u.maintenance().runBatch(u.context()).outcomes().size()); u.assertDeleted(unbound);
            u.f.clock.now=NOW.plusSeconds(19); assertTrue(u.maintenance().runBatch(u.context()).outcomes().isEmpty());
            assertEquals(NOW.plusSeconds(20),u.f.find(old).retentionFloorUntil());
            u.f.clock.now=NOW.plusSeconds(20); assertEquals(1,u.maintenance().runBatch(u.context()).outcomes().size()); u.assertDeleted(old);
            var firstShared=u.f.load(42); firstShared.updateProperty("document",null); u.f.save(firstShared);
            assertNull(u.f.find(shared).unreferencedAt());
            var lastShared=u.f.load(43); lastShared.updateProperty("document",null); u.f.save(lastShared);
            assertEquals(NOW.plusSeconds(20),u.f.find(shared).unreferencedAt());
            u.f.clock.now=NOW.plusSeconds(30); assertTrue(u.maintenance().runBatch(u.context()).outcomes().isEmpty());
            u.f.clock.now=NOW.plusSeconds(31); assertEquals(1,u.maintenance().runBatch(u.context()).outcomes().size()); u.assertDeleted(shared);
            Instant deletedAt=u.f.find(shared).deletedAt();
            u.f.clock.now=NOW.plusSeconds(1000000); assertTrue(u.maintenance().runBatch(u.context()).outcomes().isEmpty());
            assertEquals(deletedAt,u.f.find(shared).deletedAt()); u.f.assertOracle(44,replacement); assertTrue(u.exists(replacement));
            u.assertNoStages();
        }
    }

    @Test public void committedClaimAndFailuresBeforeOrAfterPhysicalDeleteRecoverWithoutRetargeting() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            BlobRef before=u.upload("before"),after=u.upload("after"); String beforeLocation=u.f.find(before).storageLocation();
            u.f.clock.now=NOW.plusSeconds(7);
            var faulty=new BlobStoreDelegate(u.files) {
                @Override public DeleteResult deleteIfVersion(UserContext context,String location,String version) {
                    assertFalse(u.f.sql.hasActiveTransaction());
                    assertEquals("deleting",u.f.otherSql.queryForList("SELECT state FROM teaql_blob_object WHERE storage_location=?",new Object[]{location}).get(0).get("state"));
                    if (location.equals(beforeLocation)) throw new IllegalStateException("PRIVATE-DELETE-CANARY");
                    super.deleteIfVersion(context,location,version); throw new IllegalStateException("PRIVATE-AFTER-DELETE-CANARY");
                }
            };
            var failed=u.maintenance(faulty,100,u.f.clock).runBatch(u.context()); assertEquals(2,failed.outcomes().size());
            for (var outcome:failed.outcomes()) { assertEquals(FAILED,outcome.status()); assertEquals(BLOB_STORE_FAILED,outcome.error()); assertFalse(outcome.toString().contains("CANARY")); }
            assertTrue(u.exists(before)); assertFalse(u.exists(after));
            assertEquals(BlobState.DELETING,u.f.find(before).state()); assertEquals(BlobState.DELETING,u.f.find(after).state());
            assertNull(u.f.find(before).deletedAt());
            var recovered=u.maintenance().runBatch(u.context()); assertEquals(2,recovered.outcomes().size());
            u.assertDeleted(before); u.assertDeleted(after); u.assertNoStages();
        }
    }

    @Test public void separateJvmHaltsBeforeAndAfterDeleteRecoverFromDurableCatalogState() throws Exception {
        for (String phase:List.of("before","after")) {
            try (FixtureWithFiles u=new FixtureWithFiles()) {
                BlobRef key=u.upload("crash"); u.f.clock.now=NOW.plusSeconds(7);
                String classpath=String.join(File.pathSeparator,System.getProperty("java.class.path",""),System.getProperty("jdk.module.path",""),Path.of("target/test-classes").toAbsolutePath().toString());
                var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-cp",classpath,
                        BlobMaintenanceCrashProbe.class.getName(),u.root.resolve("store").toString(),phase,u.f.clock.instant().toString());
                builder.environment().put("TEAQL_BLOB_CRASH_SCHEMA",u.f.schema);
                builder.redirectErrorStream(true).redirectOutput(u.root.resolve("crash-probe.log").toFile());
                Process child=builder.start();
                try {
                    assertTrue("Owned crash probe did not terminate",child.waitFor(20,TimeUnit.SECONDS));
                    if (child.exitValue()!=(phase.equals("before")?73:74)) {
                        String diagnostic=Files.readString(u.root.resolve("crash-probe.log"));
                        String password=System.getenv("TEAQL_BLOB_TEST_PG_PASSWORD");
                        if (password!=null && !password.isEmpty()) diagnostic=diagnostic.replace(password,"[redacted]");
                        fail("Owned crash probe failed before injection: "+diagnostic.substring(0,Math.min(2048,diagnostic.length())));
                    }
                }
                finally { if (child.isAlive()) child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS)); }
                assertEquals(BlobState.DELETING,u.f.find(key).state()); assertNull(u.f.find(key).deletedAt());
                assertEquals(phase.equals("before"),u.exists(key));
                assertEquals(1,u.maintenance().runBatch(u.context()).outcomes().size()); u.assertDeleted(key); u.assertNoStages();
            }
        }
    }

    @Test public void missingAndUnrecordedAbandonedContentAreCollectedWithoutClaimingVerification() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            var session=u.begin("unrecorded"); var blob=u.f.find(session.reference());
            var raw=u.files.write(u.f.context(),u.files.createUpload(u.f.context(),session.uploadId()),new ByteArrayInputStream(u.bytes),100000);
            assertNull(blob.contentVersion()); u.f.clock.now=NOW.plusSeconds(5);
            var result=u.maintenance().runBatch(u.context()); assertEquals(DELETED,result.outcomes().get(0).status());
            var deleted=u.f.find(session.reference()); assertEquals(raw.contentVersion(),deleted.contentVersion());
            assertNull(deleted.contentType()); assertNull(deleted.completedAt()); u.assertDeleted(session.reference());
            assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> u.write(session)).code());
            u.assertNoStages();
        }
    }

    @Test public void versionConflictsStayPinnedAndScopedContinuationAvoidsStarvingLaterObjects() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            for (int i=0;i<3;i++) u.upload("cursor-"+i);
            List<BlobRef> keys=u.f.otherSql.queryForList("SELECT blob_key FROM teaql_blob_object ORDER BY blob_key",new Object[0])
                    .stream().map(row -> BlobRef.of((String)row.get("blob_key"))).toList();
            BlobRef conflict=keys.get(0); var original=u.f.find(conflict);
            assertEquals(BlobStore.DeleteResult.DELETED,u.files.deleteIfVersion(u.f.context(),original.storageLocation(),original.contentVersion()));
            var changed=u.bytes.clone(); changed[changed.length-1]^=1;
            var replacement=u.files.write(u.f.context(),u.files.createUpload(u.f.context(),original.uploadId()),new ByteArrayInputStream(changed),100000);
            u.f.clock.now=NOW.plusSeconds(7); var maintenance=u.maintenance(u.files,1,u.f.clock);
            var first=maintenance.runBatch(u.context()); assertEquals(VERSION_CONFLICT,first.outcomes().get(0).status()); assertNotNull(first.next());
            var second=maintenance.runBatch(u.context(),first.next()); assertEquals(DELETED,second.outcomes().get(0).status());
            var third=maintenance.runBatch(u.context(),second.next()); assertEquals(DELETED,third.outcomes().get(0).status());
            assertTrue(maintenance.runBatch(u.context(),third.next()).outcomes().isEmpty());
            var repeat=maintenance.runBatch(u.context()); assertEquals(VERSION_CONFLICT,repeat.outcomes().get(0).status());
            assertEquals(original.contentVersion(),u.f.find(conflict).contentVersion()); assertNull(u.f.find(conflict).deletedAt());
            assertEquals(replacement.contentVersion(),u.files.stat(u.f.context(),original.storageLocation()).orElseThrow().contentVersion());
            var other=u.context(); other.putAttribute("scope",new BlobScope("test","tenant-b"));
            assertEquals(BLOB_MAINTENANCE_DENIED,assertThrows(BlobException.class,() -> maintenance.runBatch(other,first.next())).code());
            assertTrue(maintenance.runBatch(other).outcomes().isEmpty()); u.assertNoStages();
        }
    }

    @Test public void authorizationCapabilitiesAndOuterTransactionsRejectBeforeDeletion() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            BlobRef key=u.upload("denied"); u.f.clock.now=NOW.plusSeconds(7); AtomicInteger calls=new AtomicInteger();
            var observed=new BlobStoreDelegate(u.files) {
                @Override public DeleteResult deleteIfVersion(UserContext context,String location,String version) { calls.incrementAndGet(); return super.deleteIfVersion(context,location,version); }
            };
            var service=u.maintenance(observed,100,u.f.clock);
            assertEquals(BLOB_MAINTENANCE_DENIED,assertThrows(BlobException.class,() -> service.runBatch(u.f.context())).code());
            u.f.sql.executeInTransaction(() -> assertEquals(BLOB_TRANSACTION_REQUIRED,assertThrows(BlobException.class,() -> service.runBatch(u.context())).code()));
            var unsupported=new BlobStoreDelegate(observed) {
                @Override public Capabilities capabilities() { return new Capabilities(false,true,false,false,false,Duration.ZERO); }
            };
            assertEquals(BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,() -> u.maintenance(unsupported,100,u.f.clock).runBatch(u.context())).code());
            assertEquals(0,calls.get()); assertEquals(BlobState.UPLOADED_UNBOUND,u.f.find(key).state()); assertTrue(u.exists(key));
            service.runBatch(u.context()); assertEquals(1,calls.get()); u.assertDeleted(key);
        }
    }

    @Test public void revocationAfterStatAndAfterDeleteLeavesRecoverableClaim() throws Exception {
        for (boolean afterDelete:List.of(false,true)) {
            try (FixtureWithFiles u=new FixtureWithFiles()) {
                var session=u.begin("revoke"); var blob=u.f.find(session.reference());
                u.files.write(u.f.context(),u.files.createUpload(u.f.context(),session.uploadId()),new ByteArrayInputStream(u.bytes),100000);
                u.f.clock.now=NOW.plusSeconds(5); var context=u.context();
                var revoking=new BlobStoreDelegate(u.files) {
                    @Override public Optional<StoredContent> sealForDeletion(UserContext actor,String location,String expectedVersion) {
                        var value=super.sealForDeletion(actor,location,expectedVersion); if (!afterDelete) actor.putAttribute("maintain",Boolean.FALSE); return value;
                    }
                    @Override public DeleteResult deleteIfVersion(UserContext actor,String location,String version) {
                        var value=super.deleteIfVersion(actor,location,version); actor.putAttribute("maintain",Boolean.FALSE); return value;
                    }
                };
                assertEquals(BLOB_MAINTENANCE_DENIED,assertThrows(BlobException.class,() -> u.maintenance(revoking,100,u.f.clock).runBatch(context)).code());
                assertEquals(BlobState.DELETING,u.f.find(blob.reference()).state()); assertNull(u.f.find(blob.reference()).deletedAt());
                assertEquals(!afterDelete,u.exists(blob.reference()));
                u.maintenance().runBatch(u.context()); u.assertDeleted(blob.reference());
            }
        }
    }

    @Test public void catalogFailureAfterPhysicalDeleteRetriesOnlyTheAcknowledgement() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            BlobRef key=u.upload("ack-failed"); u.f.clock.now=NOW.plusSeconds(7);
            var fault=new BlobStoreDelegate(u.files) {
                @Override public DeleteResult deleteIfVersion(UserContext context,String location,String version) {
                    var result=super.deleteIfVersion(context,location,version);
                    u.f.sql.update("UPDATE teaql_blob_schema SET schema_version=99",new Object[0]); return result;
                }
            };
            var failed=u.maintenance(fault,100,u.f.clock).runBatch(u.context()); assertEquals(BLOB_SCHEMA_MISSING,failed.outcomes().get(0).error());
            u.f.sql.update("UPDATE teaql_blob_schema SET schema_version=1",new Object[0]);
            assertEquals(BlobState.DELETING,u.f.find(key).state()); assertFalse(u.exists(key));
            assertEquals(ALREADY_MISSING,u.maintenance().runBatch(u.context()).outcomes().get(0).status()); u.assertDeleted(key);
        }
    }

    @Test public void collectionWaitsForInFlightPublicationAndCleansAnExpiredUnrecordedWrite() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            var session=u.begin("in-flight"); CountDownLatch published=new CountDownLatch(1),release=new CountDownLatch(1);
            var slow=new BlobStoreDelegate(u.files) {
                @Override public StoredContent write(UserContext context,UploadTarget target,InputStream source,long maximum) {
                    var result=super.write(context,target,source,maximum); published.countDown(); await(release); return result;
                }
            };
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                Future<?> writing=pool.submit(() -> u.uploads(slow).write(u.f.context(),session.uploadId(),new ByteArrayInputStream(u.bytes)));
                assertTrue(published.await(5,TimeUnit.SECONDS)); u.f.clock.now=NOW.plusSeconds(5);
                Future<BlobMaintenanceService.Batch> collecting=pool.submit(() -> u.maintenance().runBatch(u.context()));
                assertThrows(TimeoutException.class,() -> collecting.get(150,TimeUnit.MILLISECONDS));
                release.countDown(); var failed=assertThrows(ExecutionException.class,() -> writing.get(10,TimeUnit.SECONDS));
                assertEquals(BLOB_UPLOAD_EXPIRED,((BlobException)failed.getCause()).code());
                assertEquals(DELETED,collecting.get(10,TimeUnit.SECONDS).outcomes().get(0).status()); u.assertDeleted(session.reference());
                assertEquals(BLOB_UPLOAD_EXPIRED,assertThrows(BlobException.class,() -> u.write(session)).code()); u.assertNoStages();
            } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void aCommittedDeletingClaimRejectsNewBindingBeforePhysicalIoCompletes() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            BlobRef key=u.upload("binding-race"); CountDownLatch deleting=new CountDownLatch(1),release=new CountDownLatch(1);
            var slow=new BlobStoreDelegate(u.files) {
                @Override public DeleteResult deleteIfVersion(UserContext context,String location,String version) { deleting.countDown(); await(release); return super.deleteIfVersion(context,location,version); }
            };
            ExecutorService pool=Executors.newSingleThreadExecutor();
            try {
                Future<?> collecting=pool.submit(() -> u.maintenance(slow,100,Clock.fixed(NOW.plusSeconds(7),ZoneOffset.UTC)).runBatch(u.context()));
                assertTrue(deleting.await(5,TimeUnit.SECONDS));
                assertEquals(BLOB_REFERENCE_EXPIRED,assertThrows(BlobException.class,() -> u.f.create(42,key)).code());
                assertEquals(0L,u.f.scalar("SELECT count(*) FROM asset_data"));
                assertEquals(0L,u.f.scalar("SELECT count(*) FROM teaql_blob_binding"));
                release.countDown(); collecting.get(10,TimeUnit.SECONDS); u.assertDeleted(key); u.assertNoStages();
            } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void simultaneousCollectorsDeleteOneGenerationAndShareIdempotentAcknowledgement() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            BlobRef key=u.upload("two-collectors"); u.f.clock.now=NOW.plusSeconds(7);
            CountDownLatch deleting=new CountDownLatch(2),release=new CountDownLatch(1); AtomicInteger physical=new AtomicInteger();
            var observed=new BlobStoreDelegate(u.files) {
                @Override public DeleteResult deleteIfVersion(UserContext context,String location,String version) {
                    deleting.countDown(); await(release); var result=super.deleteIfVersion(context,location,version);
                    if (result==DeleteResult.DELETED) physical.incrementAndGet(); return result;
                }
            };
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                Future<BlobMaintenanceService.Batch> first=pool.submit(() -> u.maintenance(observed,100,u.f.clock).runBatch(u.context()));
                Future<BlobMaintenanceService.Batch> second=pool.submit(() -> u.maintenance(observed,100,u.f.clock).runBatch(u.context()));
                assertTrue(deleting.await(5,TimeUnit.SECONDS)); release.countDown();
                var results=List.of(first.get(10,TimeUnit.SECONDS).outcomes().get(0).status(),second.get(10,TimeUnit.SECONDS).outcomes().get(0).status());
                assertEquals(Set.of(DELETED,ALREADY_MISSING),new HashSet<>(results)); assertEquals(1,physical.get());
                u.assertDeleted(key); assertTrue(u.maintenance().runBatch(u.context()).outcomes().isEmpty()); u.assertNoStages();
            } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void hostExplicitlySchedulesBoundedCollectionAndOwnsItsWorkerLifecycle() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            BlobRef key=u.upload("host-timer"); u.f.clock.now=NOW.plusSeconds(7); var service=u.maintenance();
            assertEquals(BlobState.UPLOADED_UNBOUND,u.f.find(key).state()); assertTrue(u.exists(key));
            ScheduledExecutorService host=Executors.newSingleThreadScheduledExecutor(); CountDownLatch finished=new CountDownLatch(1);
            AtomicReference<RuntimeException> failed=new AtomicReference<>();
            ScheduledFuture<?> job=host.scheduleWithFixedDelay(() -> {
                try { if (service.runBatch(u.context()).outcomes().stream().anyMatch(outcome -> outcome.status()==DELETED)) finished.countDown(); }
                catch (RuntimeException failure) { failed.set(failure); finished.countDown(); }
            },0,50,TimeUnit.MILLISECONDS);
            try { assertTrue(finished.await(5,TimeUnit.SECONDS)); assertNull(failed.get()); u.assertDeleted(key); }
            finally { job.cancel(false); host.shutdownNow(); assertTrue(host.awaitTermination(5,TimeUnit.SECONDS)); }
            u.assertNoStages();
        }
    }

    @Test public void completionThatCommitsDuringCandidateScanGetsItsOwnUnboundDeadline() throws Exception {
        try (FixtureWithFiles u=new FixtureWithFiles()) {
            var session=u.begin("complete-race"); u.write(session); u.f.clock.now=NOW.plusSeconds(4);
            CountDownLatch completed=new CountDownLatch(1),release=new CountDownLatch(1); ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                Future<?> confirming=pool.submit(() -> u.f.sql.executeInTransaction(() -> {
                    assertEquals(session.reference(),u.uploads(u.files).complete(u.f.context(),session.uploadId()));
                    completed.countDown(); await(release);
                }));
                assertTrue(completed.await(5,TimeUnit.SECONDS));
                assertEquals(BlobState.INITIATED,u.f.find(session.reference()).state());
                Future<BlobMaintenanceService.Batch> collecting=pool.submit(() -> u.maintenance(u.files,100,Clock.fixed(NOW.plusSeconds(5),ZoneOffset.UTC)).runBatch(u.context()));
                assertThrows(TimeoutException.class,() -> collecting.get(150,TimeUnit.MILLISECONDS));
                release.countDown(); confirming.get(10,TimeUnit.SECONDS);
                assertEquals(NOT_ELIGIBLE,collecting.get(10,TimeUnit.SECONDS).outcomes().get(0).status());
                assertEquals(BlobState.UPLOADED_UNBOUND,u.f.find(session.reference()).state());
                assertEquals(NOW.plusSeconds(4),u.f.find(session.reference()).completedAt()); assertTrue(u.exists(session.reference()));
                u.f.clock.now=NOW.plusSeconds(11); u.maintenance().runBatch(u.context()); u.assertDeleted(session.reference());
            } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }

    private static void await(CountDownLatch latch) {
        try { if (!latch.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("owned gate timed out"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("owned gate interrupted"); }
    }
    private final class FixtureWithFiles implements AutoCloseable {
        final Fixture f;
        final Path root;
        final FileBlobStore files;
        final TikaBlobContentVerifier verifier;
        final byte[] bytes;
        FixtureWithFiles() throws Exception {
            root=temporary.newFolder().toPath(); files=new FileBlobStore(root.resolve("store")); files.initialize();
            verifier=new TikaBlobContentVerifier(root.resolve("staging"),TikaBlobContentVerifier.Limits.defaults());
            bytes=Files.readAllBytes(Path.of(Objects.requireNonNull(System.getenv("TEAQL_BLOB_TEST_FIXTURES")),"sample.pdf"));
            f=new Fixture(true,policies());
        }
        DefaultUserContext context() { var context=f.context(); context.putAttribute("maintain",Boolean.TRUE); return context; }
        BlobUploadService uploads(BlobStore store) {
            return new BlobUploadService(f.catalog,store,verifier,f.policies,new BlobUploadAuthorization(
                    context -> context.getAttribute("scope",BlobScope.class),(context,target) -> true,(context,target,action,info) -> true),f.clock);
        }
        BlobUploadService.Session begin(String request) { return uploads(files).begin(f.context(),TARGET,new BlobUploadService.FileInfo("sample.pdf",null,null),request); }
        void write(BlobUploadService.Session session) { uploads(files).write(f.context(),session.uploadId(),new ByteArrayInputStream(bytes)); }
        BlobRef upload(String request) { var session=begin(request); write(session); return uploads(files).complete(f.context(),session.uploadId()); }
        BlobMaintenanceService maintenance() { return maintenance(files,100,f.clock); }
        BlobMaintenanceService maintenance(BlobStore store,int batchSize,Clock clock) {
            return new BlobMaintenanceService(f.catalog,store,context -> context.getAttribute("scope",BlobScope.class),
                    (context,scope) -> Boolean.TRUE.equals(context.getAttribute("maintain")),clock,batchSize);
        }
        boolean exists(BlobRef key) { return files.stat(f.context(),f.find(key).storageLocation()).isPresent(); }
        void assertDeleted(BlobRef key) {
            assertEquals(BlobState.DELETED,f.find(key).state()); assertNotNull(f.find(key).deletedAt()); assertFalse(exists(key));
            assertEquals("deleted",f.otherSql.queryForList("SELECT state FROM teaql_blob_object WHERE blob_key=?",new Object[]{key.key()}).get(0).get("state"));
        }
        void assertNoStages() throws IOException {
            try (var paths=Files.list(root.resolve("store/tmp"))) { var names=paths.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet()); assertTrue(names.isEmpty() || names.equals(Set.of("staging.format","staging.guard"))); }
            if (Files.exists(root.resolve("staging"))) try (var paths=Files.list(root.resolve("staging"))) { var names=paths.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet()); assertTrue(names.isEmpty() || names.equals(Set.of("staging.format","staging.guard"))); }
        }
        @Override public void close() { f.close(); }
    }
}
