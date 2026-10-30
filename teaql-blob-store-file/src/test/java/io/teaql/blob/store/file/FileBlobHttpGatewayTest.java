package io.teaql.blob.store.file;

import io.teaql.core.blob.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class FileBlobHttpGatewayTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private static final Instant NOW=Instant.parse("2026-10-25T00:00:00Z");

    @Test public void fullUrlsGetHeadAndByteRangesServeOnlyActualFileBytes() throws Exception {
        try (Fixture f=new Fixture(temporary.getRoot().toPath())) {
            var signed=f.sign(Duration.ofSeconds(10),"private, no-store");
            assertEquals("http",signed.url().getScheme()); assertEquals("127.0.0.1",signed.url().getHost()); assertTrue(signed.url().getPort()>0);
            assertFalse(signed.toString().contains("token="));
            var full=f.request(signed.url(),"GET",null); assertEquals(200,full.statusCode()); assertArrayEquals(f.bytes,full.body());
            assertEquals("application/pdf",full.headers().firstValue("Content-Type").orElseThrow());
            assertEquals("private, no-store",full.headers().firstValue("Cache-Control").orElseThrow());
            String disposition=full.headers().firstValue("Content-Disposition").orElseThrow();
            assertTrue(disposition.startsWith("attachment;")); assertTrue(disposition.contains("filename*=UTF-8''")); assertFalse(disposition.contains(".."));
            var head=f.request(signed.url(),"HEAD",null); assertEquals(200,head.statusCode()); assertEquals(0,head.body().length);
            assertEquals(Integer.toString(f.bytes.length),head.headers().firstValue("Content-Length").orElseThrow());
            for (String range:List.of("bytes=1-8","bytes=1-","bytes=-8")) {
                var response=f.request(signed.url(),"GET",range); assertEquals(206,response.statusCode());
                byte[] expected=range.equals("bytes=1-8")?Arrays.copyOfRange(f.bytes,1,9):range.equals("bytes=1-")?Arrays.copyOfRange(f.bytes,1,f.bytes.length):Arrays.copyOfRange(f.bytes,f.bytes.length-8,f.bytes.length);
                assertArrayEquals(expected,response.body()); assertTrue(response.headers().firstValue("Content-Range").orElseThrow().startsWith("bytes "));
            }
            for (String range:List.of("bytes=999999-","bytes=-0","bytes=2-1","bytes=0-1,4-5","bytes=9999999999999999999999999-")) {
                var response=f.request(signed.url(),"GET",range); assertEquals(416,response.statusCode());
                assertEquals("bytes */"+f.bytes.length,response.headers().firstValue("Content-Range").orElseThrow());
            }
        }
    }

    @Test public void actualStoreTtlCapsExpiryAndCacheWithoutCollectingContent() throws Exception {
        try (Fixture f=new Fixture(temporary.getRoot().toPath())) {
            var signed=f.sign(Duration.ofHours(1),"private, max-age=3600"); assertEquals(NOW.plusSeconds(20),signed.expiresAt());
            f.clock.now=NOW.plusSeconds(19);
            var valid=f.request(signed.url(),"GET",null); assertEquals(200,valid.statusCode()); assertEquals("private, max-age=1",valid.headers().firstValue("Cache-Control").orElseThrow());
            f.clock.now=NOW.plusSeconds(20); assertEquals(403,f.request(signed.url(),"GET",null).statusCode());
            assertEquals(f.content,f.files.stat(null,f.target.location()).orElseThrow());
        }
    }

    @Test public void tamperingMissingCredentialsAndOtherRoutesNeverReadTheObject() throws Exception {
        try (Fixture f=new Fixture(temporary.getRoot().toPath())) {
            var signed=f.sign(Duration.ofSeconds(10),"private, no-store");
            String raw=signed.url().toASCIIString(); char last=raw.charAt(raw.length()-1);
            URI tampered=URI.create(raw.substring(0,raw.length()-1)+(last=='A'?'B':'A'));
            assertEquals(403,f.request(tampered,"GET",null).statusCode());
            URI unsigned=new URI("http",null,"127.0.0.1",signed.url().getPort(),"/blob-read",null,null);
            assertEquals(403,f.request(unsigned,"GET",null).statusCode());
            URI other=new URI("http",null,"127.0.0.1",signed.url().getPort(),"/blob-read-extra",signed.url().getRawQuery(),null);
            assertEquals(404,f.request(other,"GET",null).statusCode());
            assertEquals(405,f.request(signed.url(),"POST",null).statusCode());
            f.key[0]^=1; f.originalKeys.clear();
            assertEquals(200,f.request(signed.url(),"GET",null).statusCode());
            assertFalse(f.gateway.capabilities().contextCheckedReads()); assertFalse(f.gateway.capabilities().directUpload());
        }
    }

    @Test public void staleUrlCannotReadRecreatedContentAndSigningBatchFailsAsAWhole() throws Exception {
        try (Fixture f=new Fixture(temporary.getRoot().toPath())) {
            var signed=f.sign(Duration.ofSeconds(10),"private, no-store");
            f.files.deleteIfVersion(null,f.target.location(),f.content.contentVersion());
            var next=f.files.write(null,f.target,new ByteArrayInputStream(new byte[]{7,8,9}),10);
            assertNotEquals(f.content.contentVersion(),next.contentVersion()); assertEquals(404,f.request(signed.url(),"GET",null).statusCode());
            var valid=new BlobStore.ReadRequest(f.target.location(),next.contentVersion(),"application/pdf","sample.pdf",BlobPolicyProfile.Disposition.ATTACHMENT,Duration.ofSeconds(10),"private, no-store");
            var invalid=f.read(Duration.ofSeconds(10),"private, no-store");
            assertEquals(BlobException.Code.BLOB_NOT_FOUND,assertThrows(BlobException.class,() -> f.gateway.signReadBatch(null,List.of(valid,invalid))).code());
        }
    }

    @Test public void independentCurlProcessUsesTheReturnedUrlWithoutRewritingIt() throws Exception {
        try (Fixture f=new Fixture(temporary.getRoot().toPath())) {
            URI uri=f.sign(Duration.ofSeconds(10),"private, no-store").url(); Path result=temporary.newFile("curl-result").toPath();
            Process client=new ProcessBuilder("curl","--silent","--show-error","--fail","--max-time","5","--config","-","--range","1-8","--output",result.toString()).start();
            try {
                // Keep the bearer credential out of process arguments and test output.
                client.getOutputStream().write(("url = \""+uri.toASCIIString()+"\"\n").getBytes(StandardCharsets.UTF_8)); client.getOutputStream().close();
                assertTrue(client.waitFor(8,TimeUnit.SECONDS)); assertEquals("Independent HTTP client failed",0,client.exitValue());
                assertArrayEquals(Arrays.copyOfRange(f.bytes,1,9),Files.readAllBytes(result));
            } finally { client.destroyForcibly(); assertTrue(client.waitFor(5,TimeUnit.SECONDS)); }
        }
    }

    @Test public void assemblyIsPassiveAndGatewayLifecycleIsExplicit() throws Exception {
        Path root=temporary.getRoot().toPath().resolve("passive"); FileBlobStore files=new FileBlobStore(root); byte[] key=new byte[32]; new SecureRandom().nextBytes(key);
        assertEquals(BlobException.Code.BLOB_POLICY_INVALID,assertThrows(BlobException.class,() -> new FileBlobHttpGateway(files,
                new InetSocketAddress("127.0.0.1",0),URI.create("//127.0.0.1/blob-read"),"active",Map.of("active",key),Clock.systemUTC(),Duration.ofMinutes(1))).code());
        try (var gateway=new FileBlobHttpGateway(files,new InetSocketAddress("127.0.0.1",0),URI.create("http://127.0.0.1:0/blob-read"),"active",Map.of("active",key),Clock.systemUTC(),Duration.ofMinutes(1))) {
            assertFalse(Files.exists(root)); assertEquals(BlobException.Code.BLOB_CAPABILITY_MISSING,assertThrows(BlobException.class,gateway::validateReady).code());
            files.initialize(); gateway.start(); gateway.validateReady(); assertThrows(IllegalStateException.class,gateway::start);
            gateway.close(); assertThrows(BlobException.class,gateway::validateReady); assertTrue(Files.exists(root.resolve("store.format")));
        }
    }

    private static final class MutableClock extends Clock {
        volatile Instant now=NOW;
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { if (!zone.equals(ZoneOffset.UTC)) throw new IllegalArgumentException(); return this; }
    }
    private static final class Fixture implements AutoCloseable {
        final FileBlobStore files;
        final FileBlobHttpGateway gateway;
        final MutableClock clock=new MutableClock();
        final byte[] bytes=FileBlobStoreTest.fixture("pdf"),key=new byte[32];
        final Map<String,byte[]> originalKeys=new HashMap<>();
        final BlobStore.UploadTarget target;
        final BlobStore.StoredContent content;
        final ExecutorService executor=Executors.newFixedThreadPool(2);
        final HttpClient client=HttpClient.newBuilder().executor(executor).connectTimeout(Duration.ofSeconds(3)).build();
        Fixture(Path root) throws IOException {
            files=new FileBlobStore(root.resolve("store")); files.initialize(); target=files.createUpload(null,"http_fixture"); content=files.write(null,target,new ByteArrayInputStream(bytes),bytes.length);
            new SecureRandom().nextBytes(key); originalKeys.put("active",key);
            gateway=new FileBlobHttpGateway(files,new InetSocketAddress("127.0.0.1",0),URI.create("http://127.0.0.1:0/blob-read"),"active",originalKeys,clock,Duration.ofSeconds(20)); gateway.start();
        }
        BlobStore.ReadRequest read(Duration lifetime,String cache) { return new BlobStore.ReadRequest(target.location(),content.contentVersion(),"application/pdf","../../报告.pdf",BlobPolicyProfile.Disposition.ATTACHMENT,lifetime,cache); }
        BlobStore.SignedRead sign(Duration lifetime,String cache) { return gateway.signReadBatch(null,List.of(read(lifetime,cache))).get(0); }
        HttpResponse<byte[]> request(URI uri,String method,String range) throws InterruptedException {
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).method(method,HttpRequest.BodyPublishers.noBody());
            if (range!=null) request.header("Range",range);
            try { return client.send(request.build(),HttpResponse.BodyHandlers.ofByteArray()); }
            catch (IOException failure) { throw new AssertionError("HTTP transport failed"); }
        }
        @Override public void close() throws InterruptedException { gateway.close(); executor.shutdownNow(); assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS)); }
    }
}
