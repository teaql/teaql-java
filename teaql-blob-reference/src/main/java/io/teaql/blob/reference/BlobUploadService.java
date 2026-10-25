package io.teaql.blob.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import static io.teaql.core.blob.BlobException.Code.*;
import static io.teaql.blob.reference.BlobUploadAuthorization.Action.*;

/** Current-context upload orchestration. File bytes become bindable only after actual verification. */
public final class BlobUploadService implements BlobRuntime.Uploads {
    private final PostgresBlobCatalog catalog;
    private final BlobStore store;
    private final BlobContentVerifier verifier;
    private final BlobPolicies policies;
    private final BlobUploadAuthorization authorization;
    private final Clock clock;
    private final SecureRandom random=new SecureRandom();
    public BlobUploadService(PostgresBlobCatalog catalog,BlobStore store,BlobContentVerifier verifier,
            BlobPolicies policies,BlobUploadAuthorization authorization,Clock clock) {
        this.catalog=Objects.requireNonNull(catalog); this.store=Objects.requireNonNull(store); this.verifier=Objects.requireNonNull(verifier);
        this.policies=Objects.requireNonNull(policies); this.authorization=Objects.requireNonNull(authorization); this.clock=Objects.requireNonNull(clock);
    }
    public Session begin(UserContext context,BlobFieldTarget target,FileInfo input,String idempotencyKey) {
        FileInfo info=normalize(input); BlobFieldSpec field=field(context,target); BlobScope scope=authorization.scope(context);
        authorization.require(context,target,BEGIN,info);
        String extension=extension(info.fileName());
        if (!field.allowedExtensions().contains(extension)) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
        policies.requireUploadFormat(extension);
        if (info.declaredSize()!=null && info.declaredSize()>policies.maximumUploadSizeBytes()) throw new BlobException(BLOB_SIZE_EXCEEDED);
        String requestDigest=digest(target,info);
        var previous=catalog.findUploadRequest(scope,idempotencyKey,requestDigest);
        if (previous.isPresent()) return session(context,previous.orElseThrow(),info);
        BlobPolicySnapshot policy=policies.resolve(field);
        if (!policy.field().allowedExtensions().contains(extension)) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
        if (info.declaredSize()!=null && info.declaredSize()>policy.maxSizeBytes()) throw new BlobException(BLOB_SIZE_EXCEEDED);
        if (policy.uploadMode()==BlobPolicyProfile.UploadMode.DIRECT && !store.capabilities().directUpload()) throw new BlobException(BLOB_CAPABILITY_MISSING);
        String uploadId="bu_"+randomId(); BlobRef key=BlobRef.of("blob_"+randomId());
        BlobStore.UploadTarget candidate=store.createUpload(context,uploadId);
        if (!scope.equals(authorization.scope(context))) throw new BlobException(BLOB_BIND_DENIED);
        authorization.require(context,target,BEGIN,info);
        BlobCatalogRecord blob=catalog.begin(new PostgresBlobCatalog.UploadDraft(scope,key,uploadId,idempotencyKey,requestDigest,
                info.fileName(),extension,candidate.location(),target,policy),clock.instant());
        return session(context,blob,info);
    }
    private Session session(UserContext context,BlobCatalogRecord blob,FileInfo info) {
        requireScope(context,blob);
        authorization.require(context,blob.uploadTarget(),BEGIN,info);
        BlobStore.UploadTarget actual=store.createUpload(context,blob.uploadId()); requireLocation(blob,actual);
        if (blob.policy().uploadMode()==BlobPolicyProfile.UploadMode.DIRECT) {
            if (actual.directUrl()==null || actual.directExpiresAt()==null || actual.directExpiresAt().isAfter(blob.uploadExpiresAt())
                    || actual.directExpiresAt().isAfter(clock.instant().plus(blob.policy().uploadUrlTtl()))) throw new BlobException(BLOB_CAPABILITY_MISSING);
        } else if (actual.directUrl()!=null) throw new BlobException(BLOB_CAPABILITY_MISSING);
        return new Session(blob.uploadId(),blob.reference(),blob.uploadExpiresAt(),actual.directUrl(),actual.directExpiresAt());
    }
    public WriteReceipt write(UserContext context,String uploadId,InputStream source) {
        BlobCatalogRecord blob=upload(context,uploadId,WRITE); requireWriteTime(blob);
        return catalog.withUploadLock(blob.scope(),blob.reference(),locked -> {
            requireScope(context,locked); requireWriteTime(locked);
            authorization.require(context,locked.uploadTarget(),WRITE,new FileInfo(locked.originalName(),locked.sizeBytes(),locked.contentType()));
            if (locked.policy().uploadMode()!=BlobPolicyProfile.UploadMode.SERVER_STREAM) throw new BlobException(BLOB_CAPABILITY_MISSING);
            BlobStore.UploadTarget target=store.createUpload(context,locked.uploadId()); requireLocation(locked,target);
            BlobStore.StoredContent content=store.write(context,target,new DeadlineInput(source,locked.uploadExpiresAt()),maximum(locked));
            requireScope(context,locked); requireWriteTime(locked);
            authorization.require(context,locked.uploadTarget(),WRITE,new FileInfo(locked.originalName(),content.sizeBytes(),null));
            catalog.recordWrite(locked.scope(),locked.reference(),content,clock.instant());
            return new WriteReceipt(locked.uploadId(),content.sizeBytes());
        });
    }
    public BlobRef complete(UserContext context,String uploadId) {
        BlobCatalogRecord blob=upload(context,uploadId,CONFIRM);
        if (blob.state()==BlobState.INITIATED) requireWriteTime(blob);
        else {
            Instant eligible=blob.unreferencedAt()==null?null:BlobRetention.unreferencedEligibleAt(blob.unreferencedAt(),Duration.ofSeconds(blob.effectiveUnreferencedSeconds()),blob.retentionFloorUntil());
            BlobRetention.requireBindable(blob.state(),clock.instant(),blob.completedAt(),blob.policy().unboundRetention(),eligible);
        }
        BlobStore.StoredContent content=store.stat(context,blob.storageLocation()).orElseThrow(() -> new BlobException(BLOB_UPLOAD_INCOMPLETE));
        if (content.sizeBytes()>maximum(blob)) throw new BlobException(BLOB_SIZE_EXCEEDED);
        if (blob.contentVersion()!=null && !blob.contentVersion().equals(content.contentVersion())) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
        if (blob.state()!=BlobState.INITIATED) {
            if (!Objects.equals(blob.sizeBytes(),content.sizeBytes()) || content.sha256()!=null && !content.sha256().equals(blob.sha256())) throw new BlobException(BLOB_UPLOAD_INCOMPLETE);
            return blob.reference();
        }
        BlobContentVerifier.Verified verified;
        try (BlobStore.RangeContent bytes=store.openRange(context,blob.storageLocation(),content.contentVersion(),0,content.sizeBytes())) {
            verified=verifier.verify(bytes.stream(),blob.extension(),maximum(blob),blob.policy().allowEmpty());
        } catch (IOException unavailable) { throw new BlobException(BLOB_STORE_FAILED); }
        if (verified.sizeBytes()!=content.sizeBytes() || content.sha256()!=null && !content.sha256().equals(verified.sha256())) throw new BlobException(BLOB_UPLOAD_INCOMPLETE);
        requireScope(context,blob); requireWriteTime(blob);
        authorization.require(context,blob.uploadTarget(),CONFIRM,new FileInfo(blob.originalName(),verified.sizeBytes(),verified.contentType()));
        return catalog.complete(blob.scope(),blob.reference(),new PostgresBlobCatalog.VerifiedContent(verified.contentType(),verified.sizeBytes(),verified.sha256(),content.contentVersion()),clock.instant()).reference();
    }
    private BlobCatalogRecord upload(UserContext context,String id,BlobUploadAuthorization.Action action) {
        BlobCatalogRecord blob=catalog.findUpload(authorization.scope(context),id).orElseThrow(() -> new BlobException(BLOB_BIND_DENIED));
        policies.requireUploadFormat(blob.extension());
        BlobFieldSpec current=field(context,blob.uploadTarget());
        if (!current.allowedExtensions().contains(blob.extension())) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
        authorization.require(context,blob.uploadTarget(),action,new FileInfo(blob.originalName(),blob.sizeBytes(),blob.contentType()));
        return blob;
    }
    private void requireWriteTime(BlobCatalogRecord blob) {
        if (blob.state()==BlobState.DELETING || blob.state()==BlobState.DELETED || !clock.instant().isBefore(blob.uploadExpiresAt())) throw new BlobException(BLOB_UPLOAD_EXPIRED);
    }
    private void requireScope(UserContext context,BlobCatalogRecord blob) { if (!authorization.scope(context).equals(blob.scope())) throw new BlobException(BLOB_BIND_DENIED); }
    private long maximum(BlobCatalogRecord blob) { return Math.min(blob.policy().maxSizeBytes(),policies.maximumUploadSizeBytes()); }
    private static void requireLocation(BlobCatalogRecord blob,BlobStore.UploadTarget target) {
        if (!blob.storageLocation().equals(target.location()) || !blob.uploadId().equals(target.uploadId())) throw new BlobException(BLOB_STORE_FAILED);
    }
    private static BlobFieldSpec field(UserContext context,BlobFieldTarget target) {
        for (EntityDescriptor descriptor=EntityMetaFactory.requireFrom(context).resolveEntityDescriptor(target.entity());descriptor!=null;descriptor=descriptor.getParent()) {
            for (PropertyDescriptor property:descriptor.getProperties()) if (property.getName().equals(target.field())) {
                if (property.blobFieldSpec()==null || property.getType().javaType()!=BlobRef.class) throw new BlobException(BLOB_REFERENCE_INVALID);
                return property.blobFieldSpec();
            }
        }
        throw new BlobException(BLOB_REFERENCE_INVALID);
    }
    private static FileInfo normalize(FileInfo input) {
        Objects.requireNonNull(input); String name=input.fileName(); name=name.substring(Math.max(name.lastIndexOf('/'),name.lastIndexOf('\\'))+1);
        if (name.isBlank() || name.getBytes(StandardCharsets.UTF_8).length>512 || name.codePoints().anyMatch(Character::isISOControl)) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
        return new FileInfo(name,input.declaredSize(),input.declaredContentType()==null?null:input.declaredContentType().trim().toLowerCase(Locale.ROOT));
    }
    private static String extension(String name) { int dot=name.lastIndexOf('.'); return dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT); }
    private String randomId() { byte[] value=new byte[24]; random.nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private static String digest(BlobFieldTarget target,FileInfo info) {
        try {
            var bytes=new ByteArrayOutputStream(); try (var output=new DataOutputStream(bytes)) {
                for (String value:List.of(target.entity(),target.field(),info.fileName())) { byte[] text=value.getBytes(StandardCharsets.UTF_8); output.writeInt(text.length); output.write(text); }
                output.writeBoolean(target.entityId()!=null); if (target.entityId()!=null) output.writeLong(target.entityId());
                output.writeBoolean(info.declaredSize()!=null); if (info.declaredSize()!=null) output.writeLong(info.declaredSize());
                output.writeBoolean(info.declaredContentType()!=null);
                if (info.declaredContentType()!=null) { byte[] text=info.declaredContentType().getBytes(StandardCharsets.UTF_8); output.writeInt(text.length); output.write(text); }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | GeneralSecurityException impossible) { throw new BlobException(BLOB_POLICY_INVALID); }
    }
    private final class DeadlineInput extends FilterInputStream {
        private final Instant expiry;
        DeadlineInput(InputStream source,Instant expiry) { super(Objects.requireNonNull(source)); this.expiry=expiry; }
        private void check() { if (!clock.instant().isBefore(expiry)) throw new BlobException(BLOB_UPLOAD_EXPIRED); }
        @Override public int read() throws IOException { check(); int n=in.read(); check(); return n; }
        @Override public int read(byte[] bytes,int offset,int length) throws IOException { check(); int n=in.read(bytes,offset,length); check(); return n; }
        @Override public void close() { /* Caller owns the original stream. */ }
    }
}
