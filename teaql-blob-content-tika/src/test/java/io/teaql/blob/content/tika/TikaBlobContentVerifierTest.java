package io.teaql.blob.content.tika;

import io.teaql.core.blob.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class TikaBlobContentVerifierTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private Path staging() { return temporary.getRoot().toPath().resolve("staging"); }
    private TikaBlobContentVerifier verifier() { return new TikaBlobContentVerifier(staging(),TikaBlobContentVerifier.Limits.defaults()); }
    private static byte[] file(String extension) throws IOException {
        try (var stream=TikaBlobContentVerifierTest.class.getResourceAsStream("/reference-files/files/sample."+extension)) {
            if (stream==null) throw new IOException("Fixture absent"); return stream.readAllBytes();
        }
    }
    @Test public void actualAllTenFormatsParseAndKeepExactSizeAndDigest() throws Exception {
        var verifier=verifier(); assertFalse(Files.exists(staging()));
        for (String extension:List.of("jpg","jpeg","png","mp4","mp3","doc","docx","xls","xlsx","pdf")) {
            byte[] bytes=file(extension); BlobContentVerifier.Verified result;
            try { result=verifier.verify(new ByteArrayInputStream(bytes),extension,bytes.length,false); }
            catch (BlobException failed) { throw new AssertionError("Real format "+extension+" rejected with "+failed.code()); }
            assertEquals(extension,BlobFieldSpec.of("Asset","file",List.of(extension),true,"default").allowedContentTypes().first(),result.contentType());
            assertEquals(bytes.length,result.sizeBytes()); assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),result.sha256());
            assertEmpty();
        }
    }
    @Test public void filenameLikeRestrictionsCannotMasqueradeActualContainers() throws Exception {
        var verifier=verifier();
        for (var pair:List.of(List.of("jpg","png"),List.of("docx","xlsx"),List.of("xls","doc"),List.of("pdf","exe"))) {
            byte[] bytes=file(pair.get(0));
            assertEquals(BlobException.Code.BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,
                    () -> verifier.verify(new ByteArrayInputStream(bytes),pair.get(1),100000,false)).code());
        }
        byte[] garbage="%PDF-1.7\nnot-a-document\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        assertEquals(BlobException.Code.BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,
                () -> verifier.verify(new ByteArrayInputStream(garbage),"pdf",100000,false)).code());
        assertEmpty();
    }
    @Test public void exactStreamBoundsAndIoFailuresLeaveNoStage() throws Exception {
        byte[] bytes=file("pdf"); var verifier=verifier();
        assertEquals(BlobException.Code.BLOB_SIZE_EXCEEDED,assertThrows(BlobException.class,
                () -> verifier.verify(new ByteArrayInputStream(bytes),"pdf",bytes.length-1,false)).code());
        var failure=assertThrows(BlobException.class,() -> verifier.verify(new InputStream() {
            @Override public int read() throws IOException { throw new IOException("PRIVATE-CANARY"); }
        },"pdf",100000,false));
        assertEquals(BlobException.Code.BLOB_STORE_FAILED,failure.code()); assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("CANARY"));
        assertEquals(BlobException.Code.BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,
                () -> verifier.verify(new ByteArrayInputStream(new byte[0]),"pdf",100000,true)).code()); assertEmpty();
    }
    @Test public void containerHeaderWithoutVideoTrackIsNotAnMp4Video() throws Exception {
        byte[] header=Arrays.copyOf(file("mp4"),32);
        assertEquals(BlobException.Code.BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,
                () -> verifier().verify(new ByteArrayInputStream(header),"mp4",100000,false)).code());
        assertEmpty();
    }
    @Test public void truncatedImagesAndAudioTagsCannotPassFromMagicAlone() throws Exception {
        for (var item:Map.of("jpg",32,"png",33,"mp3",45).entrySet()) {
            byte[] bytes=Arrays.copyOf(file(item.getKey()),item.getValue());
            assertEquals(item.getKey(),BlobException.Code.BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,
                    () -> verifier().verify(new ByteArrayInputStream(bytes),item.getKey(),100000,false)).code());
        }
        assertEmpty();
    }
    @Test public void stagingFailuresAreDistinctFromInvalidContentAndInsecureConfiguration() throws Exception {
        byte[] bytes=file("pdf"); Path obstruction=temporary.newFile().toPath();
        var unavailable=new TikaBlobContentVerifier(obstruction,TikaBlobContentVerifier.Limits.defaults());
        assertEquals(BlobException.Code.BLOB_STORE_FAILED,assertThrows(BlobException.class,
                () -> unavailable.verify(new ByteArrayInputStream(bytes),"pdf",100000,false)).code());
        Path shared=temporary.newFolder().toPath();
        Files.setPosixFilePermissions(shared,java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-x---"));
        var insecure=new TikaBlobContentVerifier(shared,TikaBlobContentVerifier.Limits.defaults());
        assertEquals(BlobException.Code.BLOB_POLICY_INVALID,assertThrows(BlobException.class,
                () -> insecure.verify(new ByteArrayInputStream(bytes),"pdf",100000,false)).code());
        try (var paths=Files.list(shared)) { assertEquals(0,paths.count()); }
    }
    @Test public void actualPdfCacheSpillsInsideTheLeaseAndEnforcesItsStorageBudget() throws Exception {
        var area=new io.teaql.blob.staging.file.PrivateStagingArea(staging());
        try (var stage=area.open()) {
            var parser=new TikaBlobContentVerifier.StagedPdfParser(stage.directory(),10L*1024*1024);
            try (var cache=parser.cache().create(); var buffer=cache.createBuffer()) {
                byte[] block=new byte[1024*1024]; for (int i=0;i<9;i++) buffer.write(block);
                try (var entries=Files.list(stage.directory())) {
                    assertTrue(entries.anyMatch(path -> !Set.of("lease","content.bin").contains(path.getFileName().toString())));
                }
                assertThrows(IOException.class,() -> { for (int i=0;i<3;i++) buffer.write(block); });
            }
        }
        assertEmpty();
    }
    @Test public void expandedPackagesTraversalAndExternalXmlRejectBeforeParsing() throws Exception {
        var small=new TikaBlobContentVerifier(staging(),new TikaBlobContentVerifier.Limits(64,100,1000,1));
        byte[] bomb=zip("word/document.xml","<root>"+"x".repeat(4096)+"</root>");
        assertEquals(BlobException.Code.BLOB_SIZE_EXCEEDED,assertThrows(BlobException.class,
                () -> small.verify(new ByteArrayInputStream(bomb),"docx",100000,false)).code());
        for (byte[] unsafe:List.of(zip("../escape.xml","<root/>"),zip("word/document.xml","<!DOCTYPE root [<!ENTITY x SYSTEM 'file:///nonexistent-private-file'>]><root>&x;</root>"))) {
            assertEquals(BlobException.Code.BLOB_FORMAT_NOT_ALLOWED,assertThrows(BlobException.class,
                    () -> verifier().verify(new ByteArrayInputStream(unsafe),"docx",100000,false)).code());
        }
        assertEmpty();
    }
    @Test public void boundedConcurrencyFailsExplicitlyWithoutAnotherParserOrStage() throws Exception {
        var verifier=new TikaBlobContentVerifier(staging(),new TikaBlobContentVerifier.Limits(104857600,10000,1048576,1));
        byte[] bytes=file("pdf"); CountDownLatch reading=new CountDownLatch(1),release=new CountDownLatch(1); ExecutorService pool=Executors.newSingleThreadExecutor();
        try {
            Future<BlobContentVerifier.Verified> first=pool.submit(() -> verifier.verify(new ByteArrayInputStream(bytes) {
                @Override public synchronized int read(byte[] buffer,int offset,int length) {
                    reading.countDown(); try { if (!release.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("gate timed out"); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("interrupted"); }
                    return super.read(buffer,offset,length);
                }
            },"pdf",100000,false));
            assertTrue(reading.await(5,TimeUnit.SECONDS));
            assertEquals(BlobException.Code.BLOB_VERIFICATION_BUSY,assertThrows(BlobException.class,
                    () -> verifier.verify(new ByteArrayInputStream(bytes),"pdf",100000,false)).code());
            release.countDown(); assertEquals("application/pdf",first.get(10,TimeUnit.SECONDS).contentType()); assertEmpty();
        } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
    }
    private void assertEmpty() throws IOException { if (Files.exists(staging())) try (var paths=Files.list(staging())) { var names=paths.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet()); assertTrue(names.isEmpty() || names.equals(Set.of("staging.format","staging.guard"))); } }
    private static byte[] zip(String name,String text) throws IOException {
        var bytes=new ByteArrayOutputStream(); try (var zip=new ZipOutputStream(bytes)) { zip.putNextEntry(new ZipEntry(name)); zip.write(text.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); } return bytes.toByteArray();
    }
}
