package io.teaql.blob.store.file;

import io.teaql.core.blob.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class FileBlobStoreTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private Path root() { return temporary.getRoot().toPath().resolve("store"); }
    private FileBlobStore store() { var store=new FileBlobStore(root()); store.initialize(); return store; }
    static byte[] fixture(String extension) throws IOException {
        try (var input=FileBlobStoreTest.class.getResourceAsStream("/reference-files/files/sample."+extension)) {
            if (input==null) throw new IOException("Fixture missing"); return input.readAllBytes();
        }
    }

    @Test public void constructionIsPassiveAndInitializationRequiresPrivateDedicatedRoot() throws Exception {
        FileBlobStore store=new FileBlobStore(root()); store.createUpload(null,"passive"); assertFalse(Files.exists(root()));
        assertEquals(BlobException.Code.BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,store::validateReady).code());
        store.initialize(); store.validateReady(); new FileBlobStore(root()).initialize();
        Path unrelated=temporary.newFolder("unrelated").toPath(); Files.writeString(unrelated.resolve("keep"),"original");
        assertEquals(BlobException.Code.BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> new FileBlobStore(unrelated).initialize()).code());
        assertEquals("original",Files.readString(unrelated.resolve("keep"))); assertFalse(Files.exists(unrelated.resolve("store.format")));
        assertFalse(store.capabilities().signedReads()); assertTrue(store.capabilities().versionConditionalDelete());
    }

    @Test public void allTenRealFormatsRoundTripExactBytesAndDigests() throws Exception {
        FileBlobStore store=store();
        for (String extension:List.of("jpg","jpeg","png","mp4","mp3","doc","docx","xls","xlsx","pdf")) {
            byte[] bytes=fixture(extension); var target=store.createUpload(null,"real_"+extension);
            var content=store.write(null,target,new ByteArrayInputStream(bytes),bytes.length);
            assertEquals(bytes.length,content.sizeBytes()); assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),content.sha256());
            assertEquals(content,store.stat(null,target.location()).orElseThrow());
            try (var read=store.openRange(null,target.location(),content.contentVersion(),0,content.sizeBytes())) { assertArrayEquals(bytes,read.stream().readAllBytes()); }
            var again=store.write(null,target,new ByteArrayInputStream(bytes),bytes.length); assertEquals(content,again);
            assertFalse(content.toString().contains(target.location()));
        }
        try (var paths=Files.list(root().resolve("tmp"))) { assertEquals(0,paths.count()); }
    }

    @Test public void streamingLimitConsumesAtMostOneExtraByteAndFailurePublishesNothing() throws Exception {
        FileBlobStore store=store(); byte[] bytes=new byte[65538]; Arrays.fill(bytes,(byte)7); int[] consumed={0};
        InputStream source=new ByteArrayInputStream(bytes) {
            @Override public synchronized int read(byte[] buffer,int offset,int length) { int count=super.read(buffer,offset,length); if (count>0) consumed[0]+=count; return count; }
        };
        var target=store.createUpload(null,"bounded");
        assertEquals(BlobException.Code.BLOB_SIZE_EXCEEDED,assertThrows(BlobException.class,() -> store.write(null,target,source,65536)).code());
        assertEquals(65537,consumed[0]); assertTrue(store.stat(null,target.location()).isEmpty());
        try (var paths=Files.list(root().resolve("tmp"))) { assertEquals(0,paths.count()); }
        var empty=store.write(null,target,new ByteArrayInputStream(new byte[0]),1);
        try (var read=store.openRange(null,target.location(),empty.contentVersion(),0,0)) { assertEquals(-1,read.stream().read()); }
    }

    @Test public void incompatibleRetryAndStaleDeletionCannotReplaceAnotherGeneration() throws Exception {
        FileBlobStore store=store(); var target=store.createUpload(null,"immutable"); byte[] first={1,2,3},second={4,5};
        var initial=store.write(null,target,new ByteArrayInputStream(first),10);
        assertEquals(BlobException.Code.BLOB_IDEMPOTENCY_CONFLICT,assertThrows(BlobException.class,() -> store.write(null,target,new ByteArrayInputStream(second),10)).code());
        assertEquals(initial,store.stat(null,target.location()).orElseThrow());
        try (var opened=store.openRange(null,target.location(),initial.contentVersion(),1,2)) {
            assertEquals(BlobStore.DeleteResult.DELETED,store.deleteIfVersion(null,target.location(),initial.contentVersion()));
            var next=store.write(null,target,new ByteArrayInputStream(second),10);
            assertNotEquals(initial.contentVersion(),next.contentVersion());
            assertEquals(BlobStore.DeleteResult.VERSION_MISMATCH,store.deleteIfVersion(null,target.location(),initial.contentVersion()));
            assertEquals(next,store.stat(null,target.location()).orElseThrow()); assertArrayEquals(new byte[]{2,3},opened.stream().readAllBytes());
        }
        var current=store.stat(null,target.location()).orElseThrow();
        assertEquals(BlobStore.DeleteResult.DELETED,store.deleteIfVersion(null,target.location(),current.contentVersion()));
        assertEquals(BlobStore.DeleteResult.ALREADY_MISSING,store.deleteIfVersion(null,target.location(),current.contentVersion()));
    }

    @Test public void pathsSymlinksAndCorruptHeadersFailWithoutReadingOutsideTheRoot() throws Exception {
        FileBlobStore store=store(); var target=store.createUpload(null,"symlink");
        Path outside=temporary.newFile("outside").toPath(); Files.writeString(outside,"outside-canary");
        Path object=root().resolve("objects").resolve(target.location()+".blob"); Files.createSymbolicLink(object,outside);
        assertThrows(BlobException.class,() -> store.stat(null,target.location()));
        assertEquals("outside-canary",Files.readString(outside)); Files.delete(object);
        assertEquals(BlobException.Code.BLOB_REFERENCE_INVALID,assertThrows(BlobException.class,() -> store.stat(null,"../outside")).code());
        assertThrows(BlobException.class,() -> store.createUpload(null,"../outside"));
        var content=store.write(null,target,new ByteArrayInputStream(new byte[]{1,2,3}),10);
        try (var channel=FileChannel.open(object,StandardOpenOption.WRITE)) { channel.truncate(64); }
        assertEquals(BlobException.Code.BLOB_STORE_FAILED,assertThrows(BlobException.class,() -> store.openRange(null,target.location(),content.contentVersion(),0,3)).code());
    }

    @Test public void independentInstancesSerializeIdenticalConcurrentWrites() throws Exception {
        FileBlobStore first=store(),second=new FileBlobStore(root()); second.initialize(); var target=first.createUpload(null,"concurrent");
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1); byte[] bytes=fixture("pdf");
        try {
            Future<BlobStore.StoredContent> a=pool.submit(() -> { assertTrue(start.await(5,TimeUnit.SECONDS)); return first.write(null,target,new ByteArrayInputStream(bytes),bytes.length); });
            Future<BlobStore.StoredContent> b=pool.submit(() -> { assertTrue(start.await(5,TimeUnit.SECONDS)); return second.write(null,target,new ByteArrayInputStream(bytes),bytes.length); });
            start.countDown(); assertEquals(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
    }

    @Test public void nativeFileLockCoordinatesWithASeparateJvm() throws Exception {
        FileBlobStore store=store(); var target=store.createUpload(null,"process_lock"); var content=store.write(null,target,new ByteArrayInputStream(new byte[]{1}),1);
        Path lock=root().resolve("locks").resolve(target.location().substring(0,2)+".lock");
        Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-cp","target/test-classes",LockProbe.class.getName(),lock.toString()).redirectErrorStream(true).start();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try (var output=new BufferedReader(new InputStreamReader(child.getInputStream(),java.nio.charset.StandardCharsets.UTF_8))) {
            assertEquals("locked",pool.submit(output::readLine).get(5,TimeUnit.SECONDS));
            Future<BlobStore.DeleteResult> deletion=pool.submit(() -> store.deleteIfVersion(null,target.location(),content.contentVersion()));
            assertThrows(TimeoutException.class,() -> deletion.get(150,TimeUnit.MILLISECONDS));
            child.getOutputStream().write(1); child.getOutputStream().flush();
            assertTrue(child.waitFor(5,TimeUnit.SECONDS)); assertEquals(0,child.exitValue()); assertEquals(BlobStore.DeleteResult.DELETED,deletion.get(5,TimeUnit.SECONDS));
        } finally { child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS)); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
    }
    public static final class LockProbe {
        public static void main(String[] arguments) throws Exception {
            try (var channel=FileChannel.open(Path.of(arguments[0]),StandardOpenOption.CREATE,StandardOpenOption.WRITE); var lock=channel.lock()) {
                if (!lock.isValid()) throw new IOException("Lock missing"); System.out.println("locked"); System.out.flush(); System.in.read();
            }
        }
    }
}
