package io.teaql.blob.store.file;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobPolicyProfile.Disposition;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static io.teaql.core.blob.BlobException.Code.*;

/** Explicit host-owned bearer URL gateway. It does not claim context-checked reads. */
public final class FileBlobHttpGateway implements BlobStore,AutoCloseable {
    private final FileBlobStore files;
    private final InetSocketAddress bind;
    private final URI configuredBase;
    private final String activeKey;
    private final Map<String,byte[]> keys;
    private final Clock clock;
    private final Duration maximumTtl;
    private HttpServer server;
    private ExecutorService executor;
    private volatile URI base;

    /** Passive. Key material is independent of secure-document keys and copied defensively. */
    public FileBlobHttpGateway(FileBlobStore files,InetSocketAddress bind,URI publicBase,
            String activeKey,Map<String,byte[]> keys,Clock clock,Duration maximumTtl) {
        this.files=Objects.requireNonNull(files); this.bind=Objects.requireNonNull(bind);
        this.clock=Objects.requireNonNull(clock); this.maximumTtl=Objects.requireNonNull(maximumTtl);
        if (maximumTtl.isZero() || maximumTtl.isNegative() || maximumTtl.getNano()!=0) throw new BlobException(BLOB_POLICY_INVALID);
        try { maximumTtl.toMillis(); } catch (ArithmeticException invalid) { throw new BlobException(BLOB_POLICY_INVALID); }
        if (publicBase==null || publicBase.getHost()==null || publicBase.getRawUserInfo()!=null || publicBase.getRawQuery()!=null
                || publicBase.getRawFragment()!=null || !("http".equals(publicBase.getScheme()) || "https".equals(publicBase.getScheme()))
                || publicBase.getPath()==null || !publicBase.getPath().matches("/[A-Za-z0-9/_-]+") || publicBase.getPort()>65535
                || publicBase.getPort()==0 && !(publicBase.getScheme().equals("http") && Set.of("127.0.0.1","localhost","::1","[::1]").contains(publicBase.getHost()))) {
            throw new BlobException(BLOB_POLICY_INVALID);
        }
        configuredBase=publicBase;
        if (activeKey==null || keys==null || keys.isEmpty() || keys.size()>64 || !keys.containsKey(activeKey)) throw new BlobException(BLOB_POLICY_INVALID);
        Map<String,byte[]> copies=new HashMap<>();
        keys.forEach((name,key) -> {
            if (name==null || !name.matches("[A-Za-z0-9_-]{1,64}") || key==null || key.length<32 || key.length>128) throw new BlobException(BLOB_POLICY_INVALID);
            copies.put(name,key.clone());
        });
        this.activeKey=activeKey; this.keys=Collections.unmodifiableMap(copies);
    }

    /** Explicit listener startup; closes only this gateway's resources on failure. */
    public synchronized void start() {
        if (server!=null) throw new IllegalStateException("Blob gateway is already started");
        files.validateReady(); HttpServer created=null; ExecutorService workers=null;
        try {
            created=HttpServer.create(bind,64);
            workers=new ThreadPoolExecutor(4,4,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(64),r -> {
                Thread thread=new Thread(r,"teaql-file-blob-read"); thread.setDaemon(true); return thread;
            },new ThreadPoolExecutor.AbortPolicy());
            URI published=new URI(configuredBase.getScheme(),null,configuredBase.getHost(),
                    configuredBase.getPort()==0?created.getAddress().getPort():configuredBase.getPort(),configuredBase.getPath(),null,null);
            created.createContext(published.getPath(),this::handle);
            created.setExecutor(workers);
            base=published; created.start(); server=created; executor=workers;
        } catch (IOException | URISyntaxException | RuntimeException unavailable) {
            base=null; if (created!=null) created.stop(0); if (workers!=null) workers.shutdownNow(); throw new BlobException(BLOB_CAPABILITY_MISSING);
        }
    }
    @Override public void validateReady() { if (base==null) throw new BlobException(BLOB_CAPABILITY_MISSING); files.validateReady(); }
    @Override public synchronized void close() {
        base=null;
        if (server!=null) { server.stop(0); server=null; }
        if (executor!=null) {
            executor.shutdownNow();
            try { if (!executor.awaitTermination(5,TimeUnit.SECONDS)) throw new BlobException(BLOB_STORE_FAILED); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new BlobException(BLOB_STORE_FAILED); }
            finally { executor=null; }
        }
    }
    @Override public long maximumWriteBytes() { return files.maximumWriteBytes(); }
    @Override public Capabilities capabilities() { return new Capabilities(false,true,true,false,true,maximumTtl); }
    @Override public UploadTarget createUpload(UserContext context,String uploadId) { return files.createUpload(context,uploadId); }
    @Override public StoredContent write(UserContext context,UploadTarget target,InputStream source,long maxBytes) { return files.write(context,target,source,maxBytes); }
    @Override public Optional<StoredContent> stat(UserContext context,String location) { return files.stat(context,location); }
    @Override public RangeContent openRange(UserContext context,String location,String expectedVersion,long offset,long length) { return files.openRange(context,location,expectedVersion,offset,length); }
    @Override public DeleteResult deleteIfVersion(UserContext context,String location,String expectedVersion) { return files.deleteIfVersion(context,location,expectedVersion); }

    @Override public List<SignedRead> signReadBatch(UserContext context,List<ReadRequest> requests) {
        URI origin=base; if (origin==null) throw new BlobException(BLOB_CAPABILITY_MISSING);
        if (requests.size()>10000) throw new BlobException(BLOB_POLICY_INVALID);
        try {
            List<SignedRead> result=new ArrayList<>(requests.size()); Instant now=clock.instant();
            for (ReadRequest request:requests) {
                StoredContent content=files.stat(context,request.location()).orElseThrow(() -> new BlobException(BLOB_NOT_FOUND));
                if (!content.contentVersion().equals(request.contentVersion())) throw new BlobException(BLOB_NOT_FOUND);
                Duration ttl=request.lifetime().compareTo(maximumTtl)>0?maximumTtl:request.lifetime();
                Instant expiry=now.plus(ttl).truncatedTo(ChronoUnit.SECONDS);
                if (!now.isBefore(expiry)) throw new BlobException(BLOB_SIGN_FAILED);
                String name=basename(request.fileName()); if (name.getBytes(StandardCharsets.UTF_8).length>512) throw new BlobException(BLOB_POLICY_INVALID);
                Claim claim=new Claim(request.location(),request.contentVersion(),request.contentType(),name,request.disposition(),
                        cache(request.cachePolicy(),Duration.between(now,expiry).getSeconds()),expiry);
                String payload=encode(claim); String prefix="fbr1."+activeKey+"."+payload;
                String token=prefix+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(mac(keys.get(activeKey),origin,prefix));
                URI url=new URI(origin.getScheme(),null,origin.getHost(),origin.getPort(),origin.getPath(),"token="+token,null);
                result.add(new SignedRead(url,expiry));
            }
            return List.copyOf(result);
        } catch (BlobException known) { throw known; }
        catch (IOException | GeneralSecurityException | URISyntaxException | RuntimeException unavailable) { throw new BlobException(BLOB_SIGN_FAILED); }
    }

    private record Claim(String location,String version,String mime,String name,Disposition disposition,String cache,Instant expiry) {}
    private record Selection(long offset,long length,boolean partial) {}
    private void handle(HttpExchange exchange) {
        try {
            URI origin=base;
            if (origin==null || !origin.getRawPath().equals(exchange.getRequestURI().getRawPath())) { error(exchange,404); return; }
            boolean head=exchange.getRequestMethod().equals("HEAD");
            if (!head && !exchange.getRequestMethod().equals("GET")) { exchange.getResponseHeaders().set("Allow","GET, HEAD"); error(exchange,405); return; }
            String query=exchange.getRequestURI().getRawQuery();
            if (query==null || !query.startsWith("token=") || query.length()>4102) { error(exchange,403); return; }
            Claim claim=verify(query.substring(6),origin);
            if (claim==null || !clock.instant().isBefore(claim.expiry())) { error(exchange,403); return; }
            StoredContent metadata=files.stat(null,claim.location()).orElseThrow(() -> new BlobException(BLOB_NOT_FOUND));
            if (!metadata.contentVersion().equals(claim.version())) { error(exchange,404); return; }
            Selection selection;
            try { selection=select(exchange.getRequestHeaders().getFirst("Range"),metadata.sizeBytes()); }
            catch (IllegalArgumentException invalid) { exchange.getResponseHeaders().set("Content-Range","bytes */"+metadata.sizeBytes()); error(exchange,416); return; }
            try (RangeContent content=files.openRange(null,claim.location(),claim.version(),selection.offset(),selection.length())) {
                var headers=exchange.getResponseHeaders(); headers.set("Content-Type",claim.mime()); headers.set("X-Content-Type-Options","nosniff");
                headers.set("Content-Disposition",disposition(claim)); headers.set("Accept-Ranges","bytes");
                headers.set("ETag","\""+claim.version()+"\""); headers.set("Cache-Control",cache(claim.cache(),Math.max(0,Duration.between(clock.instant(),claim.expiry()).getSeconds())));
                headers.set("Content-Length",Long.toString(selection.length()));
                if (selection.partial()) headers.set("Content-Range","bytes "+selection.offset()+"-"+(selection.offset()+selection.length()-1)+"/"+metadata.sizeBytes());
                exchange.sendResponseHeaders(selection.partial()?206:200,head || selection.length()==0?-1:selection.length());
                if (!head && selection.length()>0) content.stream().transferTo(exchange.getResponseBody());
            }
        } catch (BlobException failure) {
            try { error(exchange,failure.code()==BLOB_NOT_FOUND?404:503); } catch (IOException ignored) { }
        } catch (IOException | RuntimeException unavailable) {
            try { error(exchange,503); } catch (IOException ignored) { }
        } finally { exchange.close(); }
    }
    private static void error(HttpExchange exchange,int status) throws IOException {
        if (exchange.getResponseCode()<0) { exchange.getResponseHeaders().set("Cache-Control","private, no-store"); exchange.sendResponseHeaders(status,-1); }
    }
    private Claim verify(String token,URI origin) {
        try {
            if (token.length()>4096) return null;
            String[] parts=token.split("\\.",-1);
            if (parts.length!=4 || !parts[0].equals("fbr1") || !parts[1].matches("[A-Za-z0-9_-]{1,64}")
                    || !parts[2].matches("[A-Za-z0-9_-]+") || !parts[3].matches("[A-Za-z0-9_-]+")) return null;
            byte[] key=keys.get(parts[1]); if (key==null) return null;
            byte[] signature=Base64.getUrlDecoder().decode(parts[3]);
            if (signature.length!=32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(signature).equals(parts[3])
                    || !MessageDigest.isEqual(signature,mac(key,origin,parts[0]+"."+parts[1]+"."+parts[2]))) return null;
            byte[] payload=Base64.getUrlDecoder().decode(parts[2]);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(payload).equals(parts[2])) return null;
            try (var input=new DataInputStream(new ByteArrayInputStream(payload))) {
                if (input.readUnsignedByte()!=1) return null;
                Instant expiry=Instant.ofEpochSecond(input.readLong());
                String location=readString(input,64),version=readString(input,32),mime=readString(input,160),name=readString(input,512),cache=readString(input,64);
                int flag=input.readUnsignedByte(); if (flag>1 || input.read()!=-1) return null;
                if (!location.matches("[0-9a-f]{64}") || !version.matches("[0-9a-f]{32}")) return null;
                var validated=new ReadRequest(location,version,mime,name,flag==0?Disposition.INLINE:Disposition.ATTACHMENT,Duration.ofSeconds(1),cache);
                return new Claim(validated.location(),validated.contentVersion(),validated.contentType(),validated.fileName(),validated.disposition(),validated.cachePolicy(),expiry);
            }
        } catch (IOException | GeneralSecurityException | RuntimeException invalid) { return null; }
    }
    private static String encode(Claim claim) throws IOException {
        var bytes=new ByteArrayOutputStream();
        try (var output=new DataOutputStream(bytes)) {
            output.writeByte(1); output.writeLong(claim.expiry().getEpochSecond());
            for (String field:List.of(claim.location(),claim.version(),claim.mime(),claim.name(),claim.cache())) {
                byte[] value=field.getBytes(StandardCharsets.UTF_8); output.writeInt(value.length); output.write(value);
            }
            output.writeByte(claim.disposition()==Disposition.INLINE?0:1);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    }
    private static String readString(DataInputStream input,int maximum) throws IOException {
        int length=input.readInt(); if (length<=0 || length>maximum) throw new IOException("Invalid Blob claim");
        byte[] value=input.readNBytes(length); if (value.length!=length) throw new IOException("Incomplete Blob claim");
        var decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
        return decoder.decode(java.nio.ByteBuffer.wrap(value)).toString();
    }
    private static byte[] mac(byte[] key,URI origin,String prefix) throws GeneralSecurityException {
        Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key,"HmacSHA256"));
        mac.update("teaql.file.blob-read.v1\0".getBytes(StandardCharsets.UTF_8)); mac.update(origin.toASCIIString().getBytes(StandardCharsets.UTF_8)); mac.update((byte)0);
        return mac.doFinal(prefix.getBytes(StandardCharsets.UTF_8));
    }
    private static Selection select(String range,long size) {
        if (range==null) return new Selection(0,size,false);
        if (!range.matches("bytes=([0-9]+-[0-9]*|-[0-9]+)") || size==0) throw new IllegalArgumentException();
        String[] pair=range.substring(6).split("-",-1);
        if (pair[0].isEmpty()) { long suffix=Long.parseLong(pair[1]); if (suffix<=0) throw new IllegalArgumentException(); long length=Math.min(size,suffix); return new Selection(size-length,length,true); }
        long start=Long.parseLong(pair[0]); if (start>=size) throw new IllegalArgumentException();
        long end=pair[1].isEmpty()?size-1:Math.min(size-1,Long.parseLong(pair[1])); if (end<start) throw new IllegalArgumentException();
        return new Selection(start,end-start+1,true);
    }
    private static String cache(String policy,long remaining) {
        return policy.equals("private, no-store")?policy:"private, max-age="+Math.min(remaining,Long.parseLong(policy.substring("private, max-age=".length())));
    }
    private static String basename(String name) {
        String value=name.substring(Math.max(name.lastIndexOf('/'),name.lastIndexOf('\\'))+1);
        if (value.isBlank()) throw new BlobException(BLOB_POLICY_INVALID); return value;
    }
    private static String disposition(Claim claim) {
        String extension=""; int dot=claim.name().lastIndexOf('.');
        if (dot>=0 && claim.name().substring(dot+1).matches("[A-Za-z0-9]{1,12}")) extension=claim.name().substring(dot);
        StringBuilder encoded=new StringBuilder(); char[] hex="0123456789ABCDEF".toCharArray();
        for (byte value:claim.name().getBytes(StandardCharsets.UTF_8)) {
            int c=value&255;
            if (c>='a' && c<='z' || c>='A' && c<='Z' || c>='0' && c<='9' || c=='-' || c=='_' || c=='.' || c=='~') encoded.append((char)c);
            else encoded.append('%').append(hex[c>>>4]).append(hex[c&15]);
        }
        return (claim.disposition()==Disposition.INLINE?"inline":"attachment")+"; filename=\"download"+extension+"\"; filename*=UTF-8''"+encoded;
    }
}
