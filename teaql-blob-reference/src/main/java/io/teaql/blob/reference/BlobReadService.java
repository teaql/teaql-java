package io.teaql.blob.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import io.teaql.core.meta.EntityMetaFactory;
import java.time.*;
import java.util.*;
import static io.teaql.core.blob.BlobException.Code.*;

/** Current target authorization and authoritative binding precede transport signing. */
public final class BlobReadService implements BlobRuntime.Reads {
    private final PostgresBlobCatalog catalog;
    private final BlobStore store;
    private final BlobPolicies policies;
    private final BlobReadAuthorization authorization;
    private final Clock clock;
    public BlobReadService(PostgresBlobCatalog catalog,BlobStore store,BlobPolicies policies,BlobReadAuthorization authorization,Clock clock) {
        this.catalog=Objects.requireNonNull(catalog); this.store=Objects.requireNonNull(store); this.policies=Objects.requireNonNull(policies);
        this.authorization=Objects.requireNonNull(authorization); this.clock=Objects.requireNonNull(clock);
    }
    @Override public List<Read> readBatch(UserContext context,List<Request> input,Options options) {
        Objects.requireNonNull(context); Objects.requireNonNull(input); Objects.requireNonNull(options);
        if (input.size()>MAX_BATCH) throw new BlobException(BLOB_POLICY_INVALID);
        if (input.isEmpty()) return List.of();
        List<Request> requests=List.copyOf(input); BlobScope scope=authorization.scope(context);
        Map<BlobFieldTarget,BlobRef> targets=new LinkedHashMap<>(); List<BlobPolicySnapshot> current=new ArrayList<>();
        for (Request request:requests) {
            authorization.requireField(context,request.target());
            BlobRef previous=targets.putIfAbsent(request.target(),request.reference());
            if (previous!=null && !previous.equals(request.reference())) throw new BlobException(BLOB_REFERENCE_INVALID);
            current.add(policies.resolve(BlobModel.field(EntityMetaFactory.requireFrom(context),request.target())));
        }
        BlobStore.Capabilities capabilities=store.capabilities();
        if (!capabilities.signedReads() || options.requireContextCheckedRead() && !capabilities.contextCheckedReads()) throw new BlobException(BLOB_CAPABILITY_MISSING);
        Map<BlobFieldTarget,BlobCatalogRecord> blobs=catalog.readBindings(scope,targets);
        Instant started=clock.instant(); List<BlobStore.ReadRequest> signing=new ArrayList<>(); List<Integer> positions=new ArrayList<>();
        Map<BlobStore.ReadRequest,Integer> unique=new LinkedHashMap<>();
        for (int i=0;i<requests.size();i++) {
            Request request=requests.get(i); BlobCatalogRecord blob=requireBlob(blobs,request); authorization.requireRead(context,request.target(),blob);
            BlobPolicySnapshot policy=current.get(i);
            if (!policy.field().allowedExtensions().contains(blob.extension()) || !policy.field().allowedContentTypes().contains(blob.contentType())) throw new BlobException(BLOB_READ_DENIED);
            Duration remaining=options.documentExpiresAt()==null?null:Duration.between(started,options.documentExpiresAt());
            Duration ttl=policies.readTtl(policy,options.lifetime(),capabilities.maximumReadTtl(),remaining);
            BlobStore.ReadRequest transport=new BlobStore.ReadRequest(blob.storageLocation(),blob.contentVersion(),blob.contentType(),blob.originalName(),
                    policies.readDisposition(policy,blob.contentType()),ttl,policy.cachePolicy());
            Integer index=unique.get(transport);
            if (index==null) { index=signing.size(); unique.put(transport,index); signing.add(transport); }
            positions.add(index);
        }
        requireScope(context,scope);
        for (Request request:requests) authorization.requireRead(context,request.target(),requireBlob(blobs,request));
        List<BlobStore.SignedRead> signed;
        try { signed=List.copyOf(store.signReadBatch(context,List.copyOf(signing))); }
        catch (BlobException known) { throw known; }
        catch (RuntimeException unavailable) { throw new BlobException(BLOB_SIGN_FAILED); }
        if (signed.size()!=signing.size()) throw new BlobException(BLOB_SIGN_FAILED);
        requireScope(context,scope);
        if (!blobs.equals(catalog.readBindings(scope,targets))) throw new BlobException(BLOB_READ_DENIED);
        Instant observed=clock.instant(); List<Read> result=new ArrayList<>();
        for (int i=0;i<requests.size();i++) {
            Request request=requests.get(i); BlobCatalogRecord blob=requireBlob(blobs,request); authorization.requireRead(context,request.target(),blob);
            BlobStore.SignedRead read=signed.get(positions.get(i)); BlobStore.ReadRequest transport=signing.get(positions.get(i));
            if (!observed.isBefore(read.expiresAt()) || read.expiresAt().isAfter(observed.plus(transport.lifetime()))
                    || options.documentExpiresAt()!=null && read.expiresAt().isAfter(options.documentExpiresAt())) throw new BlobException(BLOB_SIGN_FAILED);
            String cache=transport.cachePolicy();
            if (!cache.equals("private, no-store")) cache="private, max-age="+Math.min(Long.parseLong(cache.substring("private, max-age=".length())),Duration.between(observed,read.expiresAt()).getSeconds());
            result.add(new Read(request.reference(),read.url(),read.expiresAt(),blob.originalName(),blob.contentType(),blob.sizeBytes(),cache,current.get(i).responseShape()));
        }
        requireScope(context,scope);
        return List.copyOf(result);
    }
    private static BlobCatalogRecord requireBlob(Map<BlobFieldTarget,BlobCatalogRecord> blobs,Request request) {
        BlobCatalogRecord blob=blobs.get(request.target());
        if (blob==null || blob.state()!=BlobState.AVAILABLE || !blob.reference().equals(request.reference())
                || blob.contentVersion()==null || blob.sizeBytes()==null || blob.contentType()==null) throw new BlobException(BLOB_READ_DENIED);
        return blob;
    }
    private void requireScope(UserContext context,BlobScope expected) {
        if (!expected.equals(authorization.scope(context))) throw new BlobException(BLOB_READ_DENIED);
    }
}
