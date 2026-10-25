package io.teaql.blob.staging.file;

import io.teaql.core.blob.BlobException;
import io.teaql.runtime.DefaultUserContext;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static io.teaql.core.blob.BlobException.Code.*;
import static org.junit.Assert.*;

public class PrivateStagingAreaTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private Path root() { return temporary.getRoot().toPath().resolve("scratch"); }
    private PrivateStagingArea area() { return new PrivateStagingArea(root()); }
    private static DefaultUserContext context() { var context=new DefaultUserContext(null); context.putAttribute("scratchAdmin",Boolean.TRUE); return context; }
    private static PrivateStagingArea.Permission permission() { return current -> Boolean.TRUE.equals(current.getAttribute("scratchAdmin")); }
    private static void old(Path path) throws IOException { Files.setLastModifiedTime(path,FileTime.fromMillis(1000)); }
    private Path orphan(char suffix) throws IOException {
        Path path=Files.createDirectory(root().resolve("stage-"+String.valueOf(suffix).repeat(32)),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Files.writeString(path.resolve("content.bin"),"private input"); Files.writeString(path.resolve("lease"),""); old(path); return path;
    }
    private void provision() { try (var ignored=area().open()) { } }
    private void assertEmpty() throws IOException {
        try (var entries=Files.list(root())) {
            assertEquals(Set.of("staging.guard","staging.format"),entries.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet()));
        }
    }
    @Test public void passiveAssemblyPrivateJobsAndAllAuxiliaryFilesCloseTogether() throws Exception {
        var area=area(); assertFalse(Files.exists(root())); Path directory;
        try (var job=area.open()) {
            directory=job.directory(); assertEquals(PosixFilePermissions.fromString("rwx------"),Files.getPosixFilePermissions(directory));
            assertEquals(PosixFilePermissions.fromString("rw-------"),Files.getPosixFilePermissions(job.content()));
            Files.writeString(job.content(),"input"); Files.writeString(directory.resolve("parser-cache.tmp"),"auxiliary");
            Files.createDirectory(directory.resolve("package")); Files.writeString(directory.resolve("package/part.tmp"),"nested");
        }
        assertFalse(Files.exists(directory)); assertEmpty();
        try (var job=new PrivateStagingArea(root()).open()) { assertTrue(Files.exists(job.content())); } assertEmpty();
    }
    @Test public void candidateCursorAdvancesPastActiveAndFreshJobsAndBindsTheConfiguredArea() throws Exception {
        provision(); Path first=orphan('0'),second=orphan('1'),third=orphan('2'),fresh=orphan('3'); Files.setLastModifiedTime(fresh,FileTime.from(Instant.now().plusSeconds(10)));
        var area=area();
        try (var active=area.open()) {
            old(active.directory()); int removed=0,busy=0,freshCount=0; PrivateStagingArea.Cursor cursor=null; int batches=0;
            do {
                var batch=area.maintenance(permission()).collect(context(),Duration.ofSeconds(1),2,cursor);
                removed+=batch.removed(); busy+=batch.busy(); freshCount+=batch.fresh(); assertEquals(0,batch.failed());
                if (batch.next()!=null) {
                    var other=new PrivateStagingArea(temporary.getRoot().toPath().resolve("other"));
                    assertEquals(BLOB_MAINTENANCE_DENIED,assertThrows(BlobException.class,() -> other.maintenance(permission()).collect(context(),Duration.ofSeconds(1),2,batch.next())).code());
                }
                cursor=batch.next(); assertTrue(++batches<6);
            } while (cursor!=null);
            assertEquals(3,removed); assertEquals(1,busy); assertEquals(1,freshCount);
            assertTrue(Files.exists(active.content())); assertTrue(Files.exists(fresh));
        }
        assertFalse(Files.exists(first)); assertFalse(Files.exists(second)); assertFalse(Files.exists(third));
        old(fresh); assertEquals(1,area.maintenance(permission()).collect(context(),Duration.ofSeconds(1),100,null).removed()); assertEmpty();
    }
    @Test public void permissionFailureAndRevocationAfterClaimNeverDeleteScratch() throws Exception {
        var denied=new DefaultUserContext(null);
        assertEquals(BLOB_MAINTENANCE_DENIED,assertThrows(BlobException.class,() -> area().maintenance(permission()).collect(denied,Duration.ofSeconds(1),100,null)).code());
        assertFalse(Files.exists(root())); provision(); Path orphan=orphan('4');
        AtomicInteger calls=new AtomicInteger(); var janitor=area().maintenance(current -> calls.incrementAndGet()<3);
        assertEquals(BLOB_MAINTENANCE_DENIED,assertThrows(BlobException.class,() -> janitor.collect(context(),Duration.ofSeconds(1),100,null)).code());
        assertEquals("private input",Files.readString(orphan.resolve("content.bin"))); assertTrue(Files.exists(orphan));
        assertEquals(1,area().maintenance(permission()).collect(context(),Duration.ofSeconds(1),100,null).removed()); assertEmpty();
    }
    @Test public void invalidRootsAndSymlinksNeverDeleteOutsideTheDedicatedStage() throws Exception {
        Path outside=temporary.newFolder().toPath(); Path canary=Files.writeString(outside.resolve("canary"),"outside");
        Files.createSymbolicLink(root(),outside);
        assertEquals(BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> area().open()).code());
        Files.delete(root()); provision(); Path orphan=orphan('5'); Files.createSymbolicLink(orphan.resolve("escape"),canary); old(orphan);
        var result=area().maintenance(permission()).collect(context(),Duration.ofSeconds(1),100,null);
        assertEquals(1,result.failed()); assertEquals(0,result.removed()); assertEquals("outside",Files.readString(canary));
        assertTrue(Files.isSymbolicLink(orphan.resolve("escape")));
    }
    @Test public void privateRootProvisioningRefusesUnknownFilesAndCorruptMarkers() throws Exception {
        Files.createDirectory(root(),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path unknown=Files.writeString(root().resolve("user-data"),"keep");
        assertEquals(BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> area().open()).code()); assertEquals("keep",Files.readString(unknown));
        Files.delete(unknown); provision(); Files.writeString(root().resolve("staging.format"),"wrong");
        assertEquals(BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> area().open()).code());
    }
    @Test public void excessiveDepthAndEntryBudgetLeaveRecoverableJobsAndAllowLaterWork() throws Exception {
        provision(); Path deep=orphan('6'),nested=deep;
        for (int i=0;i<18;i++) nested=Files.createDirectory(nested.resolve("nested")); old(deep);
        Path large=orphan('7');
        for (int i=0;i<10001;i++) Files.createFile(large.resolve("part-"+i)); old(large);
        Path good=orphan('8'); var janitor=area().maintenance(permission());
        var batch=janitor.collect(context(),Duration.ofSeconds(1),100,null);
        assertEquals(2,batch.failed()); assertEquals(1,batch.removed()); assertTrue(Files.exists(deep)); assertTrue(Files.exists(large)); assertFalse(Files.exists(good));
        old(large); var retry=janitor.collect(context(),Duration.ofSeconds(1),100,null); assertEquals(1,retry.removed()); assertFalse(Files.exists(large));
    }
    @Test public void twoCollectorsShareOneOsLeaseWithoutDoubleRemoval() throws Exception {
        provision(); orphan('9'); ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<PrivateStagingArea.Batch>> batches=new ArrayList<>();
            for (int i=0;i<2;i++) batches.add(pool.submit(() -> { assertTrue(start.await(5,TimeUnit.SECONDS)); return area().maintenance(permission()).collect(context(),Duration.ofSeconds(1),100,null); }));
            start.countDown(); int removed=0;
            for (var future:batches) { var result=future.get(10,TimeUnit.SECONDS); removed+=result.removed(); assertEquals(0,result.failed()); }
            assertEquals(1,removed); assertEmpty();
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
    }
    @Test public void liveSeparateJvmLeaseProtectsOldScratchAndForcedExitMakesItCollectable() throws Exception {
        String classpath=String.join(File.pathSeparator,System.getProperty("java.class.path",""),System.getProperty("jdk.module.path",""),Path.of("target/test-classes").toAbsolutePath().toString());
        Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-cp",classpath,Probe.class.getName(),root().toString()).redirectErrorStream(true).start();
        ExecutorService reader=Executors.newSingleThreadExecutor();
        try (var output=new BufferedReader(new InputStreamReader(child.getInputStream(),java.nio.charset.StandardCharsets.UTF_8))) {
            String name=reader.submit(output::readLine).get(10,TimeUnit.SECONDS); assertNotNull(name); assertTrue(name,name.matches("stage-[0-9a-f]{32}"));
            assertTrue(child.isAlive()); var active=area().maintenance(permission()).collect(context(),Duration.ofSeconds(1),100,null);
            assertEquals(1,active.busy()); assertEquals(0,active.removed()); assertTrue(Files.exists(root().resolve(name).resolve("content.bin")));
            child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS)); assertNotEquals(0,child.exitValue());
            var collected=area().maintenance(permission()).collect(context(),Duration.ofSeconds(1),100,null); assertEquals(1,collected.removed()); assertEmpty();
        } finally { if (child.isAlive()) child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS)); reader.shutdownNow(); assertTrue(reader.awaitTermination(5,TimeUnit.SECONDS)); }
    }
    public static final class Probe {
        public static void main(String[] arguments) throws Exception {
            try (var job=new PrivateStagingArea(Path.of(arguments[0])).open()) {
                Files.writeString(job.content(),"input"); Files.writeString(job.directory().resolve("auxiliary.tmp"),"parser auxiliary"); old(job.directory());
                System.out.println(job.directory().getFileName()); System.out.flush(); System.in.read();
            }
        }
    }
}
