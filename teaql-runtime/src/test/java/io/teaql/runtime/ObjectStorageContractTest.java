package io.teaql.runtime;

import io.teaql.core.storage.*;
import io.teaql.core.storage.ObjectStorage.*;
import java.io.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class ObjectStorageContractTest {
    static class Stub implements ObjectStorageProvider {
        byte[] data = {1, 2, 3};
        long size = 3;
        int calls;
        Invocation seen;
        StorageException.Code error;
        @Override public Metadata put(Invocation i, Key k, InputStream content, PutOptions o) { seen=i; calls++; return metadata(k); }
        Metadata metadata(Key key) { return new Metadata(key, size, "application/octet-stream", "opaque", null, Map.of()); }
        @Override public Content get(Invocation i, Key k) { seen=i; calls++; return new Content(metadata(k), new ByteArrayInputStream(data)); }
        @Override public Optional<Metadata> head(Invocation i, Key k) { seen=i; calls++; if(error!=null) throw new StorageException(error); return Optional.empty(); }
        @Override public void delete(Invocation i, Key k) { calls++; }
        @Override public ReadUrl getReadUrl(Invocation i, Key k, ReadUrlOptions o) { return new ReadUrl(URI.create("https://example.invalid/?signature=secret"), Instant.now()); }
    }
    DefaultUserContext context(Stub provider) {
        var context = new DefaultUserContext(null);
        context.putAttribute(ObjectStorageProvider.class.getName(), provider);
        return context;
    }
    @Test public void contextIntentAndFailureCategories() {
        var stub = new Stub(); var context = context(stub); var key = new Key("private-key");
        expect(StorageException.Code.NOT_CONFIGURED, () -> new DefaultUserContext(null).objectStorage("load"));
        expect(StorageException.Code.INVALID_INPUT, () -> context.objectStorage("  "));
        assertEquals(0, stub.calls);
        assertFalse(context.objectStorage("load attachment").exists(key));
        assertSame(context, stub.seen.context());
        assertEquals("load attachment", stub.seen.comment());
        for (var code : List.of(StorageException.Code.ACCESS_DENIED, StorageException.Code.UNAVAILABLE)) {
            stub.error = code; expect(code, () -> context.objectStorage("check object").exists(key));
        }
    }
    @Test public void inputOwnershipAndBoundedDownloads() {
        var stub = new Stub(); var storage = context(stub).objectStorage("verify stream boundaries");
        var key = new Key("private-key");
        AtomicInteger closed = new AtomicInteger();
        storage.put(key, new ByteArrayInputStream(new byte[0]) {
            @Override public void close() { closed.incrementAndGet(); }
        }, PutOptions.create(0));
        assertEquals(1, closed.get());
        assertArrayEquals(stub.data, storage.getBytes(key, 3));
        expect(StorageException.Code.TOO_LARGE, () -> storage.getBytes(key, 2));
        stub.size = 2; // A provider's metadata cannot bypass actual body-length validation.
        expect(StorageException.Code.IO, () -> storage.getBytes(key, 2));
        stub.size = 4;
        expect(StorageException.Code.IO, () -> storage.getBytes(key, 4));
    }
    @Test public void streamReportsCompletionOnlyAtEofAndObservabilityCannotBreakStorage() throws Exception {
        var stub = new Stub(); var context = context(stub);
        List<String> outcomes = new ArrayList<>();
        context.putAttribute(ObjectStorageObserver.class.getName(), (ObjectStorageObserver) (c, operation, comment) -> outcomes::add);
        var storage = context.objectStorage("read attachment"); var key = new Key("private-key");
        try (var response = storage.get(key)) { assertEquals(1, response.stream().read()); }
        assertEquals(List.of("cancelled"), outcomes); outcomes.clear();
        assertArrayEquals(stub.data, storage.getBytes(key, 3));
        assertEquals(List.of("success"), outcomes);
        context.putAttribute(ObjectStorageObserver.class.getName(), (ObjectStorageObserver) (c, op, comment) -> { throw new IllegalStateException("observer failure"); });
        assertFalse(context.objectStorage("probe availability").exists(key));
    }
    @Test public void optionsValidateHeadersAndKeepSecretsOutOfDiagnostics() {
        expect(StorageException.Code.INVALID_INPUT, () -> new Key(""));
        expect(StorageException.Code.INVALID_INPUT, () -> new Key("x".repeat(1025)));
        expect(StorageException.Code.TOO_LARGE, () -> PutOptions.create(ObjectStorage.MAX_PUT_SIZE + 1));
        expect(StorageException.Code.INVALID_INPUT, () -> new PutOptions(0, "x\r\ny", Map.of(), WriteMode.REPLACE));
        expect(StorageException.Code.INVALID_INPUT, () -> new PutOptions(0, null, Map.of("Bad Key", "value"), WriteMode.CREATE_ONLY));
        for (var lifetime : List.of(Duration.ZERO, Duration.ofNanos(1), Duration.ofDays(8)))
            expect(StorageException.Code.INVALID_INPUT, () -> new ReadUrlOptions(lifetime, null, null));
        expect(StorageException.Code.INVALID_INPUT, () -> new ReadUrlOptions(Duration.ofSeconds(1), Disposition.ATTACHMENT, "bad\nname"));
        assertFalse(new Key("private-key").toString().contains("private-key"));
        var url = new ReadUrl(URI.create("https://example.invalid/?signature=secret"), Instant.now());
        assertFalse(url.toString().contains("signature"));
        assertNull(new StorageException(StorageException.Code.ACCESS_DENIED).getCause());
    }
    static void expect(StorageException.Code code, Runnable work) {
        try { work.run(); fail("Expected " + code); } catch (StorageException error) { assertEquals(code, error.code()); }
    }
}
