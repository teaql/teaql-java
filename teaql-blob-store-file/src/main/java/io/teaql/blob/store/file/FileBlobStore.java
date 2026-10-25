package io.teaql.blob.store.file;

import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import static io.teaql.core.blob.BlobException.Code.*;
import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/** Immutable filesystem generations. The configured root is exclusive to this adapter's service account. */
public final class FileBlobStore implements BlobStore {
    private static final byte[] MAGIC={'T','E','A','Q','L','B','L','1'};
    private static final byte[] FORMAT="teaql.file.blob-store/v1\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final int HEADER=64;
    private static final ReentrantLock[] LOCKS=new ReentrantLock[256];
    static { for (int i=0;i<LOCKS.length;i++) LOCKS[i]=new ReentrantLock(); }
    private final Path configuredRoot;
    private final SecureRandom random=new SecureRandom();
    private volatile Path root;

    /** Passive. No directory, file, socket or background job is created. */
    public FileBlobStore(Path root) { configuredRoot=Objects.requireNonNull(root).toAbsolutePath().normalize(); }

    /** Explicit provisioning of a private, dedicated, empty or already initialized root. */
    public synchronized void initialize() {
        try {
            Files.createDirectories(configuredRoot,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            requirePrivateDirectory(configuredRoot);
            Path actual=configuredRoot.toRealPath(); Path marker=actual.resolve("store.format");
            if (!Files.exists(marker,NOFOLLOW_LINKS)) {
                try (var contents=Files.newDirectoryStream(actual)) {
                    if (contents.iterator().hasNext()) throw new BlobException(BLOB_POLICY_INVALID);
                }
                Files.write(marker,FORMAT,CREATE_NEW,WRITE);
                try (var channel=FileChannel.open(marker,WRITE,NOFOLLOW_LINKS)) { channel.force(true); }
            }
            checkFormat(marker);
            for (String directory:List.of("objects","locks","tmp")) {
                Path path=actual.resolve(directory);
                Files.createDirectories(path,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
                requirePrivateDirectory(path);
            }
            forceDirectory(actual);
            root=actual;
        } catch (BlobException known) { throw known; }
        catch (UnsupportedOperationException unsupported) { throw new BlobException(BLOB_CAPABILITY_MISSING); }
        catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    public void validateReady() {
        Path actual=ready();
        try {
            requirePrivateDirectory(actual); checkFormat(actual.resolve("store.format"));
            for (String directory:List.of("objects","locks","tmp")) requirePrivateDirectory(actual.resolve(directory));
        } catch (BlobException known) { throw known; }
        catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }

    @Override public Capabilities capabilities() { return new Capabilities(false,true,false,false,true,Duration.ZERO); }
    @Override public UploadTarget createUpload(UserContext context,String uploadId) {
        if (uploadId==null || !uploadId.matches("[A-Za-z0-9_-]{1,128}")) throw new BlobException(BLOB_REFERENCE_INVALID);
        String location=HexFormat.of().formatHex(digest().digest(uploadId.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return new UploadTarget(uploadId,location,null,null);
    }

    @Override public StoredContent write(UserContext context,UploadTarget target,InputStream source,long maxBytes) {
        Objects.requireNonNull(source);
        if (target.directUrl()!=null || !createUpload(context,target.uploadId()).location().equals(target.location())) throw new BlobException(BLOB_REFERENCE_INVALID);
        if (maxBytes<=0) throw new BlobException(BLOB_POLICY_INVALID);
        return locked(target.location(),actual -> {
            Path temporary=Files.createTempFile(actual.resolve("tmp"),"upload-"+target.location()+"-",".part");
            try {
                byte[] version=new byte[16]; random.nextBytes(version); MessageDigest digest=digest(); long size=0;
                try (FileChannel output=FileChannel.open(temporary,WRITE,NOFOLLOW_LINKS)) {
                    output.position(HEADER); byte[] buffer=new byte[65536];
                    while (true) {
                        long remaining=maxBytes-size;
                        int count=source.read(buffer,0,(int)Math.min(buffer.length,remaining==Long.MAX_VALUE?buffer.length:remaining+1));
                        if (count<0) break;
                        if (count==0) { int value=source.read(); if (value<0) break; buffer[0]=(byte)value; count=1; }
                        if (count>remaining) throw new BlobException(BLOB_SIZE_EXCEEDED);
                        writeAll(output,ByteBuffer.wrap(buffer,0,count)); digest.update(buffer,0,count); size+=count;
                    }
                    byte[] sha=digest.digest();
                    ByteBuffer header=ByteBuffer.allocate(HEADER).put(MAGIC).put(version).putLong(size).put(sha); header.flip();
                    output.position(0); writeAll(output,header); output.force(true);
                }
                StoredContent staged=readMetadata(temporary,target.location()); Path destination=object(actual,target.location());
                if (Files.exists(destination,NOFOLLOW_LINKS)) {
                    StoredContent existing=readMetadata(destination,target.location());
                    if (existing.sizeBytes()!=staged.sizeBytes() || !existing.sha256().equals(staged.sha256())) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
                    return existing;
                }
                // All writers/deleters hold this interprocess stripe lock. No overwrite fallback is allowed.
                Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE);
                forceDirectory(actual.resolve("objects"));
                return staged;
            } finally { Files.deleteIfExists(temporary); }
        });
    }

    @Override public Optional<StoredContent> stat(UserContext context,String location) {
        return locked(location,actual -> {
            Path path=object(actual,location);
            return Files.exists(path,NOFOLLOW_LINKS)?Optional.of(readMetadata(path,location)):Optional.empty();
        });
    }
    @Override public RangeContent openRange(UserContext context,String location,String expectedVersion,long offset,long length) {
        requireVersion(expectedVersion);
        return locked(location,actual -> {
            Path path=object(actual,location);
            if (!Files.exists(path,NOFOLLOW_LINKS)) throw new BlobException(BLOB_NOT_FOUND);
            FileChannel channel=FileChannel.open(path,READ,NOFOLLOW_LINKS);
            try {
                StoredContent metadata=readMetadata(channel,location);
                if (!metadata.contentVersion().equals(expectedVersion)) throw new BlobException(BLOB_NOT_FOUND);
                if (offset<0 || length<0 || offset>metadata.sizeBytes() || length>metadata.sizeBytes()-offset) throw new BlobException(BLOB_REFERENCE_INVALID);
                channel.position(HEADER+offset);
                RangeContent result=new RangeContent(metadata,offset,length,new BoundedChannelInput(channel,length));
                channel=null;
                return result;
            } finally { if (channel!=null) channel.close(); }
        });
    }
    @Override public List<SignedRead> signReadBatch(UserContext context,List<ReadRequest> requests) {
        throw new BlobException(BLOB_CAPABILITY_MISSING);
    }
    @Override public DeleteResult deleteIfVersion(UserContext context,String location,String expectedVersion) {
        requireVersion(expectedVersion);
        return locked(location,actual -> {
            Path path=object(actual,location);
            if (!Files.exists(path,NOFOLLOW_LINKS)) return DeleteResult.ALREADY_MISSING;
            if (!readMetadata(path,location).contentVersion().equals(expectedVersion)) return DeleteResult.VERSION_MISMATCH;
            Files.delete(path); forceDirectory(actual.resolve("objects")); return DeleteResult.DELETED;
        });
    }

    private Path ready() { Path actual=root; if (actual==null) throw new BlobException(BLOB_CAPABILITY_MISSING); return actual; }
    private static Path object(Path actual,String location) { return actual.resolve("objects").resolve(location+".blob"); }
    private static void requireLocation(String location) {
        if (location==null || !location.matches("[0-9a-f]{64}")) throw new BlobException(BLOB_REFERENCE_INVALID);
    }
    private static void requireVersion(String version) {
        if (version==null || !version.matches("[0-9a-f]{32}")) throw new BlobException(BLOB_REFERENCE_INVALID);
    }
    @FunctionalInterface private interface Operation<T> { T run(Path actual) throws IOException; }
    private <T> T locked(String location,Operation<T> operation) {
        requireLocation(location); Path actual=ready(); int stripe=Integer.parseInt(location.substring(0,2),16);
        ReentrantLock local=LOCKS[stripe]; local.lock();
        try {
            requirePrivateDirectory(actual.resolve("locks")); requirePrivateDirectory(actual.resolve("objects")); requirePrivateDirectory(actual.resolve("tmp"));
            Path path=actual.resolve("locks").resolve(location.substring(0,2)+".lock");
            try (FileChannel channel=FileChannel.open(path,CREATE,WRITE,NOFOLLOW_LINKS); var lock=channel.lock()) {
                if (!lock.isValid()) throw new BlobException(BLOB_STORE_FAILED);
                return operation.run(actual);
            }
        } catch (BlobException known) { throw known; }
        catch (IOException | RuntimeException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
        finally { local.unlock(); }
    }
    private static StoredContent readMetadata(Path path,String location) throws IOException {
        try (FileChannel channel=FileChannel.open(path,READ,NOFOLLOW_LINKS)) { return readMetadata(channel,location); }
    }
    private static StoredContent readMetadata(FileChannel channel,String location) throws IOException {
        ByteBuffer buffer=ByteBuffer.allocate(HEADER);
        while (buffer.hasRemaining()) if (channel.read(buffer)<0) throw new BlobException(BLOB_STORE_FAILED);
        buffer.flip(); byte[] magic=new byte[8],version=new byte[16],sha=new byte[32]; buffer.get(magic); buffer.get(version); long size=buffer.getLong(); buffer.get(sha);
        if (!Arrays.equals(MAGIC,magic) || size<0 || size>Long.MAX_VALUE-HEADER || channel.size()!=HEADER+size) throw new BlobException(BLOB_STORE_FAILED);
        return new StoredContent(location,HexFormat.of().formatHex(version),size,HexFormat.of().formatHex(sha));
    }
    private static void writeAll(FileChannel output,ByteBuffer buffer) throws IOException { while (buffer.hasRemaining()) output.write(buffer); }
    private static void forceDirectory(Path directory) throws IOException { try (var channel=FileChannel.open(directory,READ)) { channel.force(true); } }
    private static void checkFormat(Path marker) throws IOException {
        try (var channel=FileChannel.open(marker,READ,NOFOLLOW_LINKS)) {
            if (channel.size()!=FORMAT.length) throw new BlobException(BLOB_POLICY_INVALID);
            ByteBuffer data=ByteBuffer.allocate(FORMAT.length); while (data.hasRemaining()) if (channel.read(data)<0) throw new BlobException(BLOB_POLICY_INVALID);
            if (!Arrays.equals(FORMAT,data.array())) throw new BlobException(BLOB_POLICY_INVALID);
        }
    }
    private static void requirePrivateDirectory(Path path) throws IOException {
        if (!Files.isDirectory(path,NOFOLLOW_LINKS)) throw new BlobException(BLOB_POLICY_INVALID);
        Set<PosixFilePermission> permissions=Files.getPosixFilePermissions(path,NOFOLLOW_LINKS);
        if (permissions.stream().anyMatch(p -> p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_"))) throw new BlobException(BLOB_POLICY_INVALID);
    }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    private static final class BoundedChannelInput extends InputStream {
        private final FileChannel channel; private long remaining;
        BoundedChannelInput(FileChannel channel,long length) { this.channel=channel; remaining=length; }
        @Override public int read() throws IOException { byte[] single=new byte[1]; int count=read(single,0,1); return count<0?-1:single[0]&255; }
        @Override public int read(byte[] bytes,int offset,int length) throws IOException {
            Objects.checkFromIndexSize(offset,length,bytes.length); if (length==0) return 0; if (remaining==0) return -1;
            int count=channel.read(ByteBuffer.wrap(bytes,offset,(int)Math.min(length,remaining)));
            if (count<0) throw new EOFException("Blob content incomplete"); remaining-=count; return count;
        }
        @Override public void close() throws IOException { channel.close(); }
    }
}
