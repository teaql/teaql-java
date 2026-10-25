package io.teaql.blob.store.s3;

import io.teaql.blob.staging.file.PrivateStagingArea;
import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import static io.teaql.core.blob.BlobException.Code.*;

/** Dedicated versioned S3 namespace. Supplied SDK client/presigner ownership transfers to this store. */
public final class S3BlobStore implements BlobStore,AutoCloseable {
    private static final String STATE="teaql-blob-state",CONTENT="content-v1",RETIRED="retired-v1",SHA="teaql-blob-sha256",RETIRED_VERSION="teaql-blob-retired-version";
    private static final long MAX_PUT_BYTES=5L*1024*1024*1024;
    private final S3Client client;
    private final S3Presigner presigner;
    private final AwsCredentialsProvider credentials;
    private final String bucket,prefix;
    private final Duration maximumReadTtl;
    private final PrivateStagingArea staging;
    private volatile boolean ready;
    /** Passive assembly: no request, bucket administration, actor capture or background work. */
    public S3BlobStore(S3Client client,S3Presigner presigner,AwsCredentialsProvider credentials,String bucket,String prefix,Path temporaryRoot,Duration maximumReadTtl) {
        this.client=Objects.requireNonNull(client); this.presigner=Objects.requireNonNull(presigner); this.credentials=Objects.requireNonNull(credentials);
        if (bucket==null || bucket.isBlank() || prefix==null || prefix.length()>512 || !prefix.matches("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*/")
                || maximumReadTtl==null || maximumReadTtl.isZero() || maximumReadTtl.isNegative() || maximumReadTtl.getNano()!=0 || maximumReadTtl.compareTo(Duration.ofDays(7))>0) throw new BlobException(BLOB_POLICY_INVALID);
        this.bucket=bucket; this.prefix=prefix; this.maximumReadTtl=maximumReadTtl; staging=new PrivateStagingArea(temporaryRoot);
    }
    public static S3BlobStore connect(URI endpoint,URI readEndpoint,Region region,AwsCredentialsProvider credentials,String bucket,String prefix,Path temporaryRoot,Duration maximumReadTtl,boolean pathStyle) {
        var configuration=S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build();
        var builder=S3Client.builder().region(region).credentialsProvider(credentials).serviceConfiguration(configuration)
                .httpClientBuilder(Apache5HttpClient.builder().connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(30)))
                .overrideConfiguration(options -> options.apiCallTimeout(Duration.ofMinutes(2)));
        var signer=S3Presigner.builder().region(region).credentialsProvider(credentials).serviceConfiguration(configuration);
        if (endpoint!=null) builder.endpointOverride(endpoint);
        if (readEndpoint!=null) signer.endpointOverride(readEndpoint); else if (endpoint!=null) signer.endpointOverride(endpoint);
        var client=builder.build(); S3Presigner presigner=null;
        try { presigner=signer.build(); return new S3BlobStore(client,presigner,credentials,bucket,prefix,temporaryRoot,maximumReadTtl); }
        catch (RuntimeException failed) { try { client.close(); } finally { if (presigner!=null) presigner.close(); } throw new BlobException(BLOB_POLICY_INVALID); }
    }
    /** Explicit read-only readiness. Provisioning/enabling versioning is a separate trusted host operation. */
    @Override public void validateReady() {
        ready=false;
        try { client.headBucket(b -> b.bucket(bucket)); requireVersioning(); ready=true; }
        catch (BlobException known) { throw known; }
        catch (SdkException unavailable) { throw new BlobException(BLOB_CAPABILITY_MISSING); }
    }
    public PrivateStagingArea.Maintenance stagingMaintenance(PrivateStagingArea.Permission permission) { return staging.maintenance(permission); }
    @Override public long maximumWriteBytes() { return MAX_PUT_BYTES; }
    @Override public Capabilities capabilities() { return new Capabilities(false,true,true,false,true,maximumReadTtl); }
    @Override public UploadTarget createUpload(UserContext context,String uploadId) {
        if (uploadId==null || !uploadId.matches("[A-Za-z0-9_-]{1,128}")) throw new BlobException(BLOB_REFERENCE_INVALID);
        return new UploadTarget(uploadId,prefix+HexFormat.of().formatHex(digest().digest(uploadId.getBytes(StandardCharsets.UTF_8))),null,null);
    }
    @Override public StoredContent write(UserContext context,UploadTarget target,InputStream source,long maximumBytes) {
        requireReady(); Objects.requireNonNull(source);
        if (maximumBytes<=0) throw new BlobException(BLOB_POLICY_INVALID);
        if (target.directUrl()!=null || !createUpload(context,target.uploadId()).location().equals(target.location())) throw new BlobException(BLOB_REFERENCE_INVALID);
        try (var job=staging.open()) {
            requireVersioning(); long size=0,maximum=Math.min(maximumBytes,MAX_PUT_BYTES); MessageDigest digest=digest(); byte[] buffer=new byte[65536];
            try (var output=Files.newOutputStream(job.content(),StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING,LinkOption.NOFOLLOW_LINKS)) {
                while (true) {
                    long remaining=maximum-size; int n=source.read(buffer,0,(int)Math.min(buffer.length,remaining+1));
                    if (n<0) break;
                    if (n==0) { int value=source.read(); if (value<0) break; buffer[0]=(byte)value; n=1; }
                    if (n>remaining) throw new BlobException(BLOB_SIZE_EXCEEDED);
                    output.write(buffer,0,n); digest.update(buffer,0,n); size+=n;
                }
            }
            String sha=HexFormat.of().formatHex(digest.digest()); long bytes=size;
            try {
                var put=client.putObject(b -> b.bucket(bucket).key(target.location()).ifNoneMatch("*").contentLength(bytes)
                        .contentType("application/octet-stream").metadata(Map.of(STATE,CONTENT,SHA,sha)),RequestBody.fromFile(job.content()));
                version(put.versionId());
                return new StoredContent(target.location(),put.versionId(),size,sha);
            } catch (S3Exception concurrent) {
                if (concurrent.statusCode()!=409 && concurrent.statusCode()!=412) throw concurrent;
                var existing=rawHead(target.location(),null).orElseThrow(() -> new BlobException(BLOB_IDEMPOTENCY_CONFLICT));
                if (retired(existing)) throw new BlobException(BLOB_UPLOAD_EXPIRED);
                var stored=stored(target.location(),existing);
                if (stored.sizeBytes()!=size || !sha.equals(stored.sha256())) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
                return stored;
            }
        } catch (BlobException known) { throw known; }
        catch (SdkException | IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    @Override public Optional<StoredContent> stat(UserContext context,String location) {
        requireReady(); location(location);
        try { return rawHead(location,null).filter(head -> !retired(head)).map(head -> stored(location,head)); }
        catch (BlobException known) { throw known; }
        catch (SdkException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    @Override public RangeContent openRange(UserContext context,String location,String expectedVersion,long offset,long length) {
        requireReady(); location(location); requestedVersion(expectedVersion);
        try {
            var head=rawHead(location,expectedVersion).orElseThrow(() -> new BlobException(BLOB_NOT_FOUND));
            if (retired(head)) throw new BlobException(BLOB_NOT_FOUND);
            StoredContent content=stored(location,head);
            if (offset<0 || length<0 || offset>content.sizeBytes() || length>content.sizeBytes()-offset) throw new BlobException(BLOB_REFERENCE_INVALID);
            if (length==0) return new RangeContent(content,offset,0,new ByteArrayInputStream(new byte[0]));
            ResponseInputStream<GetObjectResponse> body=client.getObject(b -> b.bucket(bucket).key(location).versionId(expectedVersion).range("bytes="+offset+"-"+(offset+length-1)));
            var response=body.response();
            if (!expectedVersion.equals(response.versionId()) || response.contentLength()==null || response.contentLength()!=length) { body.abort(); throw new BlobException(BLOB_STORE_FAILED); }
            return new RangeContent(content,offset,length,new S3Range(body,length));
        } catch (BlobException known) { throw known; }
        catch (S3Exception unavailable) {
            if (unavailable.statusCode()==404) {
                try { client.headBucket(b -> b.bucket(bucket)); } catch (SdkException bucketFailure) { throw new BlobException(BLOB_STORE_FAILED); }
                throw new BlobException(BLOB_NOT_FOUND);
            }
            throw new BlobException(BLOB_STORE_FAILED);
        }
        catch (SdkException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    @Override public List<SignedRead> signReadBatch(UserContext context,List<ReadRequest> requests) {
        requireReady();
        try {
            var snapshot=credentials.resolveCredentials(); var credentialExpiry=snapshot.expirationTime();
            if (snapshot instanceof AwsSessionCredentials && credentialExpiry.isEmpty()) throw new BlobException(BLOB_SIGN_FAILED);
            var identity=AwsRequestOverrideConfiguration.builder().credentialsProvider(StaticCredentialsProvider.create(snapshot)).build();
            List<SignedRead> output=new ArrayList<>(requests.size());
            for (var request:requests) {
                location(request.location()); requestedVersion(request.contentVersion());
                var head=rawHead(request.location(),null).orElseThrow(() -> new BlobException(BLOB_NOT_FOUND));
                if (retired(head) || !request.contentVersion().equals(head.versionId())) throw new BlobException(BLOB_NOT_FOUND);
                long seconds=Math.min(request.lifetime().getSeconds(),maximumReadTtl.getSeconds());
                if (credentialExpiry.isPresent()) seconds=Math.min(seconds,Duration.between(Instant.now(),credentialExpiry.orElseThrow()).getSeconds());
                if (seconds<=0) throw new BlobException(BLOB_SIGN_FAILED);
                String disposition=request.disposition()==BlobPolicyProfile.Disposition.INLINE?"inline":"attachment";
                String name=URLEncoder.encode(request.fileName(),StandardCharsets.UTF_8).replace("+","%20");
                String cache=request.cachePolicy();
                if (!cache.equals("private, no-store")) cache="private, max-age="+Math.min(seconds,Long.parseLong(cache.substring("private, max-age=".length())));
                var get=GetObjectRequest.builder().bucket(bucket).key(request.location()).versionId(request.contentVersion())
                        .responseContentType(request.contentType()).responseContentDisposition(disposition+"; filename=\"download\"; filename*=UTF-8''"+name)
                        .responseCacheControl(cache).overrideConfiguration(identity).build();
                Duration lifetime=Duration.ofSeconds(seconds);
                var signed=presigner.presignGetObject(b -> b.signatureDuration(lifetime).getObjectRequest(get));
                if (!signed.isBrowserExecutable()) throw new BlobException(BLOB_SIGN_FAILED);
                Instant expires=signed.expiration(); if (credentialExpiry.isPresent() && credentialExpiry.orElseThrow().isBefore(expires)) expires=credentialExpiry.orElseThrow();
                output.add(new SignedRead(signed.url().toURI(),expires));
            }
            return List.copyOf(output);
        } catch (BlobException known) { throw known; }
        catch (SdkException | URISyntaxException unavailable) { throw new BlobException(BLOB_SIGN_FAILED); }
    }
    /** Persistent ordinary-object tombstones make If-None-Match publishers lose, including late remote requests. */
    @Override public Optional<StoredContent> sealForDeletion(UserContext context,String location,String expectedVersion) {
        requireReady(); location(location); if (expectedVersion!=null) requestedVersion(expectedVersion);
        try {
            requireVersioning();
            for (int attempt=0;attempt<6;attempt++) {
                var current=rawHead(location,null);
                if (current.isPresent() && retired(current.orElseThrow())) {
                    var marker=current.orElseThrow(); String selected=selected(marker);
                    if (expectedVersion!=null && !expectedVersion.equals(selected)) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
                    if (!singlePayload(location,marker.versionId(),selected)) {
                        client.deleteObject(b -> b.bucket(bucket).key(location).versionId(marker.versionId()));
                        throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
                    }
                    return selected==null?Optional.empty():rawHead(location,selected).filter(head -> !retired(head)).map(head -> stored(location,head));
                }
                String selected=current.map(HeadObjectResponse::versionId).orElse(expectedVersion);
                if (expectedVersion!=null && current.isPresent() && !expectedVersion.equals(selected)) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
                Map<String,String> metadata=new HashMap<>(); metadata.put(STATE,RETIRED);
                if (selected!=null) metadata.put(RETIRED_VERSION,Base64.getUrlEncoder().withoutPadding().encodeToString(selected.getBytes(StandardCharsets.UTF_8)));
                byte[] tombstone=("teaql.blob.retired/v1\n"+UUID.randomUUID()+"\n").getBytes(StandardCharsets.US_ASCII);
                var put=PutObjectRequest.builder().bucket(bucket).key(location).contentType("application/x-teaql-blob-retired").metadata(metadata).contentLength((long)tombstone.length);
                if (current.isPresent()) put.ifMatch(current.orElseThrow().eTag()); else put.ifNoneMatch("*");
                try { version(client.putObject(put.build(),RequestBody.fromBytes(tombstone)).versionId()); }
                catch (S3Exception race) { if (race.statusCode()!=409 && race.statusCode()!=412) throw race; }
            }
            throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
        } catch (BlobException known) { throw known; }
        catch (SdkException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    @Override public DeleteResult deleteIfVersion(UserContext context,String location,String expectedVersion) {
        requireReady(); location(location); requestedVersion(expectedVersion);
        try {
            var sealed=sealForDeletion(context,location,expectedVersion);
            if (sealed.isEmpty()) {
                var current=rawHead(location,null);
                return current.isPresent() && retired(current.orElseThrow()) && expectedVersion.equals(selected(current.orElseThrow()))?DeleteResult.ALREADY_MISSING:DeleteResult.VERSION_MISMATCH;
            }
            client.deleteObject(b -> b.bucket(bucket).key(location).versionId(expectedVersion));
            var current=rawHead(location,null);
            if (current.isEmpty() || !retired(current.orElseThrow()) || !expectedVersion.equals(selected(current.orElseThrow()))) return DeleteResult.VERSION_MISMATCH;
            return DeleteResult.DELETED;
        } catch (BlobException error) { if (error.code()==BLOB_IDEMPOTENCY_CONFLICT) return DeleteResult.VERSION_MISMATCH; throw error; }
        catch (SdkException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    private boolean singlePayload(String location,String marker,String selected) {
        var versions=client.listObjectVersions(b -> b.bucket(bucket).prefix(location).maxKeys(64));
        if (Boolean.TRUE.equals(versions.isTruncated()) || versions.deleteMarkers().stream().anyMatch(entry -> entry.key().equals(location))) return false;
        for (var entry:versions.versions()) if (entry.key().equals(location) && !entry.versionId().equals(marker) && !entry.versionId().equals(selected)) {
            var head=rawHead(location,entry.versionId()); if (head.isPresent() && !retired(head.orElseThrow())) return false;
        }
        return true;
    }
    private Optional<HeadObjectResponse> rawHead(String location,String requestedVersion) {
        try {
            var response=client.headObject(b -> b.bucket(bucket).key(location).versionId(requestedVersion));
            version(response.versionId());
            if (response.contentLength()==null || response.contentLength()<0 || response.eTag()==null || response.eTag().isBlank()) throw new BlobException(BLOB_STORE_FAILED);
            return Optional.of(response);
        } catch (S3Exception missing) {
            if (missing.statusCode()!=404) throw missing;
            try { client.headBucket(b -> b.bucket(bucket)); } catch (SdkException bucketFailure) { throw new BlobException(BLOB_STORE_FAILED); }
            return Optional.empty();
        }
    }
    private static boolean retired(HeadObjectResponse head) { return RETIRED.equals(head.metadata().get(STATE)); }
    private static String selected(HeadObjectResponse head) {
        String value=head.metadata().get(RETIRED_VERSION); if (value==null) return null;
        try { String decoded=new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8); version(decoded); return decoded; }
        catch (RuntimeException invalid) { throw new BlobException(BLOB_STORE_FAILED); }
    }
    private static StoredContent stored(String location,HeadObjectResponse head) {
        if (!CONTENT.equals(head.metadata().get(STATE))) throw new BlobException(BLOB_STORE_FAILED);
        return new StoredContent(location,head.versionId(),head.contentLength(),head.metadata().get(SHA));
    }
    private void requireVersioning() {
        if (client.getBucketVersioning(b -> b.bucket(bucket)).status()!=BucketVersioningStatus.ENABLED) throw new BlobException(BLOB_CAPABILITY_MISSING);
    }
    private void requireReady() { if (!ready) throw new BlobException(BLOB_CAPABILITY_MISSING); }
    private void location(String value) { if (value==null || !value.startsWith(prefix) || !value.substring(prefix.length()).matches("[0-9a-f]{64}")) throw new BlobException(BLOB_REFERENCE_INVALID); }
    private static void version(String value) {
        if (value==null || value.isBlank() || value.equals("null") || value.getBytes(StandardCharsets.UTF_8).length>1024 || value.codePoints().anyMatch(Character::isISOControl)) throw new BlobException(BLOB_CAPABILITY_MISSING);
    }
    private static void requestedVersion(String value) {
        try { version(value); } catch (BlobException invalid) { throw new BlobException(BLOB_REFERENCE_INVALID); }
    }
    private static MessageDigest digest() { try { return MessageDigest.getInstance("SHA-256"); } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable"); } }
    private static final class S3Range extends InputStream {
        private final ResponseInputStream<GetObjectResponse> body; private long remaining;
        S3Range(ResponseInputStream<GetObjectResponse> body,long remaining) { this.body=body; this.remaining=remaining; }
        @Override public int read() throws IOException { byte[] one=new byte[1]; int n=read(one,0,1); return n<0?-1:one[0]&255; }
        @Override public int read(byte[] bytes,int offset,int length) throws IOException {
            Objects.checkFromIndexSize(offset,length,bytes.length); if (length==0) return 0; if (remaining==0) return -1;
            int n=body.read(bytes,offset,(int)Math.min(length,remaining)); if (n<0) throw new EOFException("Blob stream incomplete"); remaining-=n; return n;
        }
        @Override public void close() throws IOException { if (remaining==0) body.close(); else body.abort(); }
    }
    @Override public void close() { ready=false; try { client.close(); } finally { presigner.close(); } }
    @Override public String toString() { return "S3BlobStore[redacted]"; }
}
