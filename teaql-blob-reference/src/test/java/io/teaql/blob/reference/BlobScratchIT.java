package io.teaql.blob.reference;

import io.teaql.blob.content.tika.TikaBlobContentVerifier;
import io.teaql.blob.staging.file.PrivateStagingArea;
import io.teaql.blob.store.file.FileBlobStore;
import io.teaql.core.blob.*;
import io.teaql.runtime.DefaultUserContext;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;
import static io.teaql.core.blob.BlobException.Code.*;
import static org.junit.Assert.*;

/** Actual child-process termination during public upload/confirmation, not a mock stage factory. */
public class BlobScratchIT {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private static final BlobFieldTarget TARGET=new BlobFieldTarget("Asset",null,"document");
    @Test public void killedUploadReleasesSqlAndNativeLeasesBeforeScopedAndGlobalCollection() throws Exception {
        try (FilesFixture u=new FilesFixture()) {
            var session=u.service.begin(u.f.context(),TARGET,new BlobUploadService.FileInfo("sample.pdf",null,null),"crash-write");
            ExecutorService pool=Executors.newFixedThreadPool(2); Process child=u.child("write",session.uploadId());
            try {
                ready(child,pool); Path job=stage(u.root.resolve("store/tmp"));
                Files.setLastModifiedTime(job,FileTime.fromMillis(1000));
                var active=u.files.stagingMaintenance(permission()).collect(u.admin(),Duration.ofSeconds(1),100,null);
                assertEquals(1,active.busy()); assertEquals(0,active.removed());
                u.f.clock.now=NOW.plusSeconds(5);
                Future<BlobMaintenanceService.Batch> scoped=pool.submit(() -> u.collector().runBatch(u.f.context()));
                assertThrows(TimeoutException.class,() -> scoped.get(150,TimeUnit.MILLISECONDS));
                child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS)); assertNotEquals(0,child.exitValue());
                assertEquals(1,scoped.get(10,TimeUnit.SECONDS).outcomes().size());
                assertEquals(BlobState.DELETED,u.f.find(session.reference()).state());
                assertTrue(Files.exists(job));
                assertEquals(BLOB_MAINTENANCE_DENIED,assertThrows(BlobException.class,
                        () -> u.files.stagingMaintenance(permission()).collect(u.f.context(),Duration.ofSeconds(1),100,null)).code());
                assertTrue(Files.exists(job));
                assertEquals(1,u.files.stagingMaintenance(permission()).collect(u.admin(),Duration.ofSeconds(1),100,null).removed());
                assertFalse(Files.exists(job)); assertTrue(u.files.stat(u.f.context(),u.f.find(session.reference()).storageLocation()).isEmpty());
            } finally { stop(child); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }
    @Test public void killedActualVerifierScratchCanBeCollectedWithoutDeletingStoredContentAndRetryBinds() throws Exception {
        try (FilesFixture u=new FilesFixture()) {
            var session=u.service.begin(u.f.context(),TARGET,new BlobUploadService.FileInfo("sample.pdf",null,null),"crash-verify");
            u.service.write(u.f.context(),session.uploadId(),new ByteArrayInputStream(u.bytes));
            ExecutorService pool=Executors.newSingleThreadExecutor(); Process child=u.child("verify",session.uploadId());
            try {
                ready(child,pool); Path job=stage(u.root.resolve("verify"));
                Files.writeString(job.resolve("parser-auxiliary.tmp"),"owned request auxiliary"); Files.setLastModifiedTime(job,FileTime.fromMillis(1000));
                assertEquals(1,u.verifier.stagingMaintenance(permission()).collect(u.admin(),Duration.ofSeconds(1),100,null).busy());
                assertEquals(BlobState.INITIATED,u.f.find(session.reference()).state());
                assertEquals(BLOB_UPLOAD_INCOMPLETE,assertThrows(BlobException.class,() -> u.f.create(42,session.reference())).code());
                child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS));
                assertEquals(1,u.verifier.stagingMaintenance(permission()).collect(u.admin(),Duration.ofSeconds(1),100,null).removed());
                assertFalse(Files.exists(job)); assertTrue(u.files.stat(u.f.context(),u.f.find(session.reference()).storageLocation()).isPresent());
                assertEquals(session.reference(),u.service.complete(u.f.context(),session.uploadId()));
                u.f.create(42,session.reference()); u.f.assertOracle(42,session.reference());
                u.f.clock.now=NOW.plusSeconds(100); assertTrue(u.collector().runBatch(u.f.context()).outcomes().isEmpty());
                var asset=u.f.load(42); asset.updateProperty("document",null); u.f.save(asset);
                u.f.clock.now=NOW.plusSeconds(103); assertEquals(1,u.collector().runBatch(u.f.context()).outcomes().size());
                assertEquals(BlobState.DELETED,u.f.find(session.reference()).state());
                assertTrue(u.files.stat(u.f.context(),u.f.find(session.reference()).storageLocation()).isEmpty());
            } finally { stop(child); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
        }
    }
    private static PrivateStagingArea.Permission permission() { return context -> Boolean.TRUE.equals(context.getAttribute("scratchAdmin")); }
    private static Path stage(Path root) throws IOException {
        try (var entries=Files.list(root)) { var jobs=entries.filter(path -> path.getFileName().toString().matches("stage-[0-9a-f]{32}")).toList(); assertEquals(1,jobs.size()); return jobs.get(0); }
    }
    private static void ready(Process child,ExecutorService pool) throws Exception {
        assertEquals("ready",pool.submit(() -> {
            var output=new BufferedReader(new InputStreamReader(child.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder diagnostic=new StringBuilder();
            for (int i=0;i<30;i++) { String line=output.readLine(); if (line==null) break; if (line.equals("ready")) return line; if (diagnostic.length()<1024) diagnostic.append(line).append('\n'); }
            String password=System.getenv("TEAQL_BLOB_TEST_PG_PASSWORD"); String detail=diagnostic.toString();
            if (password!=null && !password.isEmpty()) detail=detail.replace(password,"[redacted]");
            throw new IOException("Owned scratch probe did not reach staging: "+detail);
        }).get(15,TimeUnit.SECONDS));
    }
    private static void stop(Process child) throws Exception {
        if (child.isAlive()) child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS));
        child.getInputStream().close(); child.getErrorStream().close(); child.getOutputStream().close();
    }
    private final class FilesFixture implements AutoCloseable {
        final Fixture f;
        final Path root;
        final FileBlobStore files;
        final TikaBlobContentVerifier verifier;
        final BlobUploadService service;
        final byte[] bytes;
        FilesFixture() throws Exception {
            var policies=new BlobPolicies(BlobPolicies.Limits.defaults(),BlobPolicyProfile.named("default").uploadSessionTtl(Duration.ofSeconds(5))
                    .unboundRetention(Duration.ofSeconds(7)).unreferencedRetention(Duration.ofSeconds(3)).build(),Map.of(),Map.of());
            f=new Fixture(true,policies); root=temporary.newFolder().toPath(); files=new FileBlobStore(root.resolve("store")); files.initialize();
            verifier=new TikaBlobContentVerifier(root.resolve("verify"),TikaBlobContentVerifier.Limits.defaults());
            service=new BlobUploadService(f.catalog,files,verifier,policies,new BlobUploadAuthorization(context -> context.getAttribute("scope",BlobScope.class),
                    (context,target) -> true,(context,target,action,info) -> true),f.clock);
            bytes=Files.readAllBytes(Path.of(Objects.requireNonNull(System.getenv("TEAQL_BLOB_TEST_FIXTURES")),"sample.pdf"));
        }
        DefaultUserContext admin() { var context=f.context(); context.putAttribute("scratchAdmin",Boolean.TRUE); return context; }
        BlobMaintenanceService collector() { return new BlobMaintenanceService(f.catalog,files,context -> context.getAttribute("scope",BlobScope.class),(context,scope) -> true,f.clock); }
        Process child(String operation,String upload) throws IOException {
            String classpath=String.join(File.pathSeparator,System.getProperty("java.class.path",""),System.getProperty("jdk.module.path",""),Path.of("target/test-classes").toAbsolutePath().toString());
            var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-cp",classpath,BlobScratchProbe.class.getName(),root.toString(),operation,upload,NOW.toString());
            builder.environment().put("TEAQL_BLOB_CRASH_SCHEMA",f.schema); return builder.redirectErrorStream(true).start();
        }
        @Override public void close() { f.close(); }
    }
}
