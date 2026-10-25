package io.teaql.blob.staging.file;

import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobException;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import static io.teaql.core.blob.BlobException.Code.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.*;

/** Trusted host scratch storage. Each job owns an OS lease until all of its resources are closed. */
public final class PrivateStagingArea {
    private static final byte[] FORMAT="teaql.private-staging/v1\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final ReentrantLock GUARD=new ReentrantLock();
    private final Path root;
    private final Clock clock;
    private final String identity;
    /** Passive construction. Root provisioning happens only on explicit open/maintenance operations. */
    public PrivateStagingArea(Path root) { this(root,Clock.systemUTC()); }
    public PrivateStagingArea(Path root,Clock clock) {
        this.root=Objects.requireNonNull(root).toAbsolutePath().normalize(); this.clock=Objects.requireNonNull(clock);
        try { identity=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(this.root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    public Job open() {
        try { return coordinated(() -> {
            Path directory=Files.createDirectory(root.resolve("stage-"+UUID.randomUUID().toString().replace("-","")),privateMode());
            FileChannel channel=FileChannel.open(directory.resolve("lease"),CREATE_NEW,WRITE,NOFOLLOW_LINKS);
            try {
                FileLock lease=channel.lock();
                Files.createFile(directory.resolve("content.bin"),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                return new Job(this,directory,channel,lease);
            }
            catch (IOException | RuntimeException failed) { channel.close(); throw failed; }
        }); } catch (BlobException known) { throw known; }
        catch (UnsupportedOperationException unsupported) { throw new BlobException(BLOB_CAPABILITY_MISSING); }
        catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    @FunctionalInterface public interface Permission { boolean canCollectSharedScratch(UserContext current); }
    public Maintenance maintenance(Permission permission) { return new Maintenance(this,Objects.requireNonNull(permission)); }
    public record Cursor(String area,String after) {
        public Cursor {
            if (area==null || !area.matches("[0-9a-f]{64}") || after==null || !after.matches("stage-[0-9a-f]{32}")) throw new BlobException(BLOB_REFERENCE_INVALID);
        }
        @Override public String toString() { return "StagingCursor[redacted]"; }
    }
    public record Batch(int candidates,int removed,int busy,int fresh,int failed,Cursor next) {}
    public static final class Maintenance {
        private final PrivateStagingArea area;
        private final Permission permission;
        private Maintenance(PrivateStagingArea area,Permission permission) { this.area=area; this.permission=permission; }
        public Batch collect(UserContext context,Duration minimumAge,int limit,Cursor cursor) {
            authorize(context);
            if (minimumAge==null || minimumAge.isZero() || minimumAge.isNegative() || limit<1 || limit>1000) throw new BlobException(BLOB_POLICY_INVALID);
            if (cursor!=null && !cursor.area().equals(area.identity)) throw new BlobException(BLOB_MAINTENANCE_DENIED);
            Instant cutoff;
            try { cutoff=area.clock.instant().minus(minimumAge); }
            catch (DateTimeException | ArithmeticException invalid) { throw new BlobException(BLOB_POLICY_INVALID); }
            if (!Files.exists(area.root,NOFOLLOW_LINKS)) return new Batch(0,0,0,0,0,null);
            List<String> candidates=area.candidates(cursor==null?"":cursor.after(),limit);
            int removed=0,busy=0,fresh=0,failed=0;
            for (String name:candidates) {
                authorize(context);
                try {
                    Job job=area.claim(name,cutoff);
                    if (job==null) { busy++; continue; }
                    try (job) {
                        authorize(context);
                        job.remove();
                    }
                    removed++;
                } catch (FreshStage ignored) { fresh++; }
                catch (BlobException error) { if (error.code()==BLOB_MAINTENANCE_DENIED) throw error; failed++; }
                catch (IOException unavailable) { failed++; }
            }
            Cursor next=candidates.size()==limit?new Cursor(area.identity,candidates.get(candidates.size()-1)):null;
            return new Batch(candidates.size(),removed,busy,fresh,failed,next);
        }
        private void authorize(UserContext context) {
            try { if (context==null || !permission.canCollectSharedScratch(context)) throw new BlobException(BLOB_MAINTENANCE_DENIED); }
            catch (RuntimeException denied) { throw new BlobException(BLOB_MAINTENANCE_DENIED); }
        }
    }
    public static final class Job implements AutoCloseable {
        private final PrivateStagingArea area;
        private final Path directory;
        private final FileChannel channel;
        private final FileLock lease;
        private boolean closed,keep;
        private Job(PrivateStagingArea area,Path directory,FileChannel channel,FileLock lease) {
            this.area=area; this.directory=directory; this.channel=channel; this.lease=lease;
        }
        public Path directory() { return directory; }
        public Path content() { return directory.resolve("content.bin"); }
        private void keep() { keep=true; }
        private void remove() { keep=false; }
        @Override public synchronized void close() {
            if (closed) return; closed=true;
            try {
                if (!keep) {
                    deleteContents(directory);
                    area.coordinated(() -> {
                        Files.deleteIfExists(directory.resolve("lease")); Files.deleteIfExists(directory);
                        return null;
                    });
                }
            } catch (IOException | RuntimeException unavailable) {
                throw new BlobException(BLOB_STORE_FAILED);
            } finally {
                try { lease.close(); }
                catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
                finally { try { channel.close(); } catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); } }
            }
        }
    }
    private List<String> candidates(String after,int limit) {
        try { return coordinated(() -> { try (var entries=Files.newDirectoryStream(root)) {
            TreeSet<String> selected=new TreeSet<>();
            for (Path entry:entries) {
                String name=entry.getFileName().toString();
                if (name.matches("stage-[0-9a-f]{32}") && name.compareTo(after)>0) {
                    selected.add(name); if (selected.size()>limit) selected.pollLast();
                }
            }
            return List.copyOf(selected);
        } }); } catch (BlobException known) { throw known; }
        catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    private Job claim(String name,Instant cutoff) throws IOException,FreshStage {
        return coordinated(() -> {
            Path directory=root.resolve(name);
            if (!Files.exists(directory,NOFOLLOW_LINKS)) throw new FreshStage();
            requirePrivate(directory);
            if (Files.getLastModifiedTime(directory,NOFOLLOW_LINKS).toInstant().isAfter(cutoff)) throw new FreshStage();
            FileChannel channel=FileChannel.open(directory.resolve("lease"),CREATE,WRITE,NOFOLLOW_LINKS);
            try {
                FileLock lease;
                try { lease=channel.tryLock(); } catch (OverlappingFileLockException active) { channel.close(); return null; }
                if (lease==null) { channel.close(); return null; }
                Job job=new Job(this,directory,channel,lease); job.keep(); return job;
            } catch (IOException | RuntimeException failed) { channel.close(); throw failed; }
        });
    }
    @FunctionalInterface private interface Operation<T> { T run() throws IOException; }
    private <T> T coordinated(Operation<T> operation) throws IOException {
        GUARD.lock(); T result=null;
        try {
            Files.createDirectories(root,privateMode()); requirePrivate(root);
            Path marker=root.resolve("staging.format");
            if (!Files.exists(marker,NOFOLLOW_LINKS)) requireEmptyForProvisioning();
            try (FileChannel channel=FileChannel.open(root.resolve("staging.guard"),CREATE,WRITE,NOFOLLOW_LINKS); FileLock lock=channel.lock()) {
                if (!lock.isValid()) throw new BlobException(BLOB_STORE_FAILED);
                if (!Files.exists(marker,NOFOLLOW_LINKS)) {
                    requireEmptyForProvisioning(); Files.write(marker,FORMAT,CREATE_NEW,WRITE);
                    try (var data=FileChannel.open(marker,WRITE,NOFOLLOW_LINKS)) { data.force(true); }
                    try (var parent=FileChannel.open(root,READ)) { parent.force(true); }
                }
                if (!Files.isRegularFile(marker,NOFOLLOW_LINKS)) throw new BlobException(BLOB_POLICY_INVALID);
                try (var content=Files.newInputStream(marker,NOFOLLOW_LINKS)) {
                    if (!Arrays.equals(FORMAT,content.readNBytes(FORMAT.length+1))) throw new BlobException(BLOB_POLICY_INVALID);
                }
                result=operation.run();
            }
            return result;
        } catch (IOException | RuntimeException failed) {
            // A failed root-lock close must not leak a new job lease before ownership reaches its caller.
            if (result instanceof Job job) {
                job.keep();
                try { job.close(); } catch (RuntimeException cleanup) { failed.addSuppressed(cleanup); }
            }
            throw failed;
        } finally { GUARD.unlock(); }
    }
    private void requireEmptyForProvisioning() throws IOException {
        if (Files.exists(root.resolve("staging.format"),NOFOLLOW_LINKS)) return;
        try (var entries=Files.newDirectoryStream(root)) {
            for (Path entry:entries) if (!entry.getFileName().toString().equals("staging.guard")) {
                if (Files.exists(root.resolve("staging.format"),NOFOLLOW_LINKS)) return;
                throw new BlobException(BLOB_POLICY_INVALID);
            }
        }
    }
    private static final class FreshStage extends IOException { private static final long serialVersionUID=1L; }
    private static FileAttribute<Set<PosixFilePermission>> privateMode() { return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")); }
    private static void requirePrivate(Path path) throws IOException {
        if (!Files.isDirectory(path,NOFOLLOW_LINKS) || Files.getPosixFilePermissions(path,NOFOLLOW_LINKS).stream().anyMatch(p -> p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_"))) {
            throw new BlobException(BLOB_POLICY_INVALID);
        }
    }
    private static void deleteContents(Path directory) throws IOException {
        int[] count={0};
        Files.walkFileTree(directory,Set.of(),16,new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attributes) throws IOException {
                if (file.equals(directory.resolve("lease"))) return FileVisitResult.CONTINUE;
                if (++count[0]>10000 || !attributes.isRegularFile() || attributes.isSymbolicLink()) throw new IOException("Invalid staging contents");
                Files.deleteIfExists(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path path,IOException failure) throws IOException {
                if (failure!=null) throw failure;
                if (!path.equals(directory)) { if (++count[0]>10000) throw new IOException("Staging budget exceeded"); Files.deleteIfExists(path); }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
