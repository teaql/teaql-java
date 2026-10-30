package io.teaql.core.storage;

import io.teaql.core.UserContext;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import static io.teaql.core.storage.StorageException.Code.*;

/** Context-owned binary storage. Keys are durable; signed URLs are temporary bearer credentials. */
public final class ObjectStorage {
    public static final long MAX_PUT_SIZE = 5L * 1024 * 1024 * 1024;
    public record Key(String value) {
        public Key {
            if (value == null || value.isEmpty() || value.getBytes(StandardCharsets.UTF_8).length > 1024
                    || value.codePoints().anyMatch(Character::isISOControl)) throw new StorageException(INVALID_INPUT);
        }
        @Override public String toString() { return "ObjectKey[masked]"; }
    }
    public enum WriteMode { CREATE_ONLY, REPLACE }
    public record PutOptions(long size, String contentType, Map<String, String> metadata, WriteMode mode) {
        public PutOptions {
            if (size < 0 || mode == null) throw new StorageException(INVALID_INPUT);
            if (size > MAX_PUT_SIZE) throw new StorageException(TOO_LARGE);
            contentType = contentType == null ? "application/octet-stream" : header(contentType);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
            metadata.forEach((key, value) -> {
                if (!key.matches("[a-z0-9][a-z0-9-]*")) throw new StorageException(INVALID_INPUT);
                header(value);
            });
        }
        public static PutOptions create(long size) { return new PutOptions(size, null, Map.of(), WriteMode.CREATE_ONLY); }
        @Override public String toString() { return "PutOptions[size=" + size + ", mode=" + mode + ", metadata=masked]"; }
    }
    public record Metadata(Key key, long size, String contentType, String etag,
            Instant lastModified, Map<String, String> customMetadata) {
        public Metadata {
            Objects.requireNonNull(key);
            if (size < 0) throw new StorageException(UNAVAILABLE);
            customMetadata = customMetadata == null ? Map.of() : Map.copyOf(customMetadata);
        }
        @Override public String toString() { return "ObjectMetadata[size=" + size + ", details=masked]"; }
    }
    public enum Disposition { INLINE, ATTACHMENT }
    public record ReadUrlOptions(Duration lifetime, Disposition disposition, String filename) {
        public ReadUrlOptions {
            if (lifetime == null || lifetime.isZero() || lifetime.isNegative()
                    || lifetime.getNano() != 0 || lifetime.compareTo(Duration.ofDays(7)) > 0)
                throw new StorageException(INVALID_INPUT);
            if (filename != null) {
                header(filename);
                if (filename.contains("/") || filename.contains("\\") || filename.contains("\""))
                    throw new StorageException(INVALID_INPUT);
                if (disposition == null) throw new StorageException(INVALID_INPUT);
            }
        }
        public static ReadUrlOptions defaults() { return new ReadUrlOptions(Duration.ofMinutes(5), null, null); }
        public String contentDisposition() {
            return disposition == null ? null : disposition.name().toLowerCase(java.util.Locale.ROOT)
                    + (filename == null ? "" : "; filename=\"" + filename + "\"");
        }
    }
    public record ReadUrl(URI url, Instant expiresAt) {
        public ReadUrl { Objects.requireNonNull(url); Objects.requireNonNull(expiresAt); }
        @Override public String toString() { return "ObjectReadUrl[url=masked, expiresAt=" + expiresAt + "]"; }
    }
    public record Content(Metadata metadata, InputStream stream) implements AutoCloseable {
        public Content { Objects.requireNonNull(metadata); Objects.requireNonNull(stream); }
        @Override public void close() throws IOException { stream.close(); }
        @Override public String toString() { return "ObjectContent[masked]"; }
    }
    public static final class Invocation {
        private final UserContext context;
        private final String comment;
        private Invocation(UserContext context, String comment) { this.context = context; this.comment = comment; }
        public UserContext context() { return context; }
        public String comment() { return comment; }
    }
    private final Invocation invocation;
    private final ObjectStorageProvider provider;
    private final ObjectStorageObserver observer;
    private ObjectStorage(UserContext context, String comment, ObjectStorageProvider provider) {
        if (comment == null || comment.isBlank()) throw new StorageException(INVALID_INPUT);
        this.invocation = new Invocation(context, comment);
        this.provider = Objects.requireNonNull(provider);
        ObjectStorageObserver configured = context.capability(ObjectStorageObserver.class);
        this.observer = configured == null ? ObjectStorageObserver.LOGGING : configured;
    }
    public static ObjectStorage forContext(UserContext context, String comment) {
        ObjectStorageProvider provider = context.capability(ObjectStorageProvider.class);
        if (provider == null) throw new StorageException(NOT_CONFIGURED);
        return new ObjectStorage(context, comment, provider);
    }

    /** Trusted adapter composition; preserves this invocation's context and observer. */
    @io.teaql.core.FrameworkInternal("Configured storage adapter composition, not client input")
    public static ObjectStorage forProvider(UserContext context, String comment, ObjectStorageProvider provider) {
        return new ObjectStorage(Objects.requireNonNull(context), comment, provider);
    }

    public boolean supportsVersionConditionalDelete() { return provider.supportsVersionConditionalDelete(); }

    public void deleteIfVersionMatches(Key key, String expectedVersion) {
        if (expectedVersion == null || expectedVersion.isBlank() || expectedVersion.length() > 1024
                || expectedVersion.equals("*") || expectedVersion.contains(",")
                || !expectedVersion.chars().allMatch(ch -> ch >= 33 && ch <= 126)) {
            throw new StorageException(INVALID_INPUT);
        }
        execute("delete_conditional", () -> {
            if (!provider.supportsVersionConditionalDelete()) throw new StorageException(UNSUPPORTED);
            provider.deleteIfVersionMatches(invocation, Objects.requireNonNull(key), expectedVersion);
            return null;
        });
    }
    public Metadata put(Key key, InputStream content, PutOptions options) {
        Objects.requireNonNull(content);
        return execute("put", () -> {
            try (content) {
                return provider.put(invocation, Objects.requireNonNull(key), content, Objects.requireNonNull(options));
            } catch (IOException error) { throw new StorageException(IO); }
        });
    }
    public Metadata putBytes(Key key, byte[] content, PutOptions options) {
        return put(key, new ByteArrayInputStream(content), options);
    }
    public Content get(Key key) {
        ObjectStorageObserver.Scope scope = start("get");
        try {
            Content content = provider.get(invocation, key);
            return new Content(content.metadata(), new FilterInputStream(content.stream()) {
                private long consumed;
                private boolean closed;
                @Override public int read() throws IOException {
                    byte[] one = new byte[1];
                    int count = read(one, 0, 1);
                    return count < 0 ? -1 : one[0] & 0xff;
                }
                @Override public int read(byte[] bytes, int off, int len) throws IOException {
                    if (closed) throw new IOException("Object storage: stream closed");
                    try {
                        int count = in.read(bytes, off, len);
                        if (count < 0) {
                            if (consumed != content.metadata().size()) throw new IOException("Object storage: length mismatch");
                            finish(scope, "success");
                        } else {
                            consumed += count;
                            if (consumed > content.metadata().size()) throw new IOException("Object storage: length mismatch");
                        }
                        return count;
                    } catch (IOException error) {
                        finish(scope, "IO");
                        closed = true;
                        try { in.close(); } catch (IOException ignored) { /* Keep the sanitized read failure. */ }
                        throw new IOException("Object storage: IO");
                    }
                }
                @Override public long skip(long n) throws IOException {
                    long remaining = n;
                    byte[] buffer = new byte[8192];
                    while (remaining > 0) {
                        int count = read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (count < 0) break;
                        remaining -= count;
                    }
                    return n - remaining;
                }
                @Override public boolean markSupported() { return false; }
                @Override public void mark(int ignored) {}
                @Override public void reset() throws IOException { throw new IOException("Object storage: mark unsupported"); }
                @Override public void close() throws IOException {
                    if (!closed) {
                        closed = true;
                        try { super.close(); finish(scope, "cancelled"); }
                        catch (IOException error) { finish(scope, "IO"); throw new IOException("Object storage: IO"); }
                    }
                }
            });
        } catch (RuntimeException error) { finish(scope, category(error)); throw error; }
    }
    public byte[] getBytes(Key key, int maximumBytes) {
        if (maximumBytes < 0) throw new StorageException(INVALID_INPUT);
        try (Content content = get(key)) {
            if (content.metadata().size() > maximumBytes) throw new StorageException(TOO_LARGE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = content.stream().read(buffer)) != -1) {
                if ((long) output.size() + count > maximumBytes) throw new StorageException(TOO_LARGE);
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } catch (IOException error) { throw new StorageException(IO); }
    }
    public Optional<Metadata> head(Key key) { return execute("head", () -> provider.head(invocation, key)); }
    public boolean exists(Key key) { return head(key).isPresent(); }
    public void delete(Key key) { execute("delete", () -> { provider.delete(invocation, key); return null; }); }
    public ReadUrl getReadUrl(Key key, ReadUrlOptions options) {
        return execute("read_url", () -> provider.getReadUrl(invocation, key, options));
    }
    private <T> T execute(String operation, Supplier<T> work) {
        var scope = start(operation);
        try { T result = work.get(); finish(scope, "success"); return result; }
        catch (RuntimeException error) { finish(scope, category(error)); throw error; }
    }
    private ObjectStorageObserver.Scope start(String operation) {
        try {
            var delegate = observer.start(invocation.context(), operation, invocation.comment());
            var once = new java.util.concurrent.atomic.AtomicBoolean();
            return outcome -> { if (once.compareAndSet(false, true)) delegate.finish(outcome); };
        } catch (RuntimeException ignored) { return outcome -> {}; }
    }
    private static void finish(ObjectStorageObserver.Scope scope, String outcome) {
        try { scope.finish(outcome); } catch (RuntimeException ignored) { /* Observability is fail-open. */ }
    }
    private static String category(RuntimeException error) {
        return error instanceof StorageException storage ? storage.code().name() : "UNAVAILABLE";
    }
    private static String header(String value) {
        if (value == null || value.isEmpty() || value.codePoints().anyMatch(c -> c < 32 || c > 126))
            throw new StorageException(INVALID_INPUT);
        return value;
    }
}
