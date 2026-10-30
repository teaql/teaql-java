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
        return readBatch(context,input,options,false).reads();
    }
    io.teaql.core.view.ViewBlobContributor.Projection readGraph(UserContext context,io.teaql.core.view.AuthorizedResponseGraph graph,Options options) {
        var result=readBatch(context,graph.blobs(),options,true);
        return new io.teaql.core.view.ViewBlobContributor.Projection(result.reads(),(current,viewOptions,document) -> revalidate(current,result,options));
    }
    private record Reading(BlobScope scope,List<Request> requests,Map<BlobFieldTarget,BlobCatalogRecord> facts,
                           List<BlobPolicySnapshot> policies,BlobStore.Capabilities capabilities,List<Read> reads) {}
    private Reading readBatch(UserContext context,List<Request> input,Options options,boolean graph) {
        Objects.requireNonNull(context); Objects.requireNonNull(input); Objects.requireNonNull(options);
        if (input.size()>(graph?io.teaql.core.view.AuthorizedResponseGraph.MAX_NODES:MAX_BATCH)) throw new BlobException(BLOB_POLICY_INVALID);
        if (input.isEmpty()) return new Reading(null,List.of(),Map.of(),List.of(),null,List.of());
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
        Map<BlobFieldTarget,BlobCatalogRecord> blobs=graph?catalog.readGraphBindings(scope,targets):catalog.readBindings(scope,targets);
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
        try {
            List<BlobStore.SignedRead> all=new ArrayList<>();
            for (int start=0;start<signing.size();start+=MAX_BATCH) {
                var batch=List.copyOf(signing.subList(start,Math.min(start+MAX_BATCH,signing.size())));
                var projected=List.copyOf(store.signReadBatch(context,batch));
                if (projected.size()!=batch.size()) throw new BlobException(BLOB_SIGN_FAILED); all.addAll(projected);
            }
            signed=List.copyOf(all);
        }
        catch (BlobException known) { throw known; }
        catch (RuntimeException unavailable) { throw new BlobException(BLOB_SIGN_FAILED); }
        if (signed.size()!=signing.size()) throw new BlobException(BLOB_SIGN_FAILED);
        requireScope(context,scope);
        if (!blobs.equals(graph?catalog.readGraphBindings(scope,targets):catalog.readBindings(scope,targets))) throw new BlobException(BLOB_READ_DENIED);
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
        return new Reading(scope,requests,blobs,List.copyOf(current),capabilities,List.copyOf(result));
    }
    /** Reuse performs current catalog/policy checks without issuing another transport credential. */
    private void revalidate(UserContext context,Reading issued,Options options) {
        if (issued.requests().isEmpty()) return;
        requireScope(context,issued.scope());
        if (!issued.capabilities().equals(store.capabilities())) throw new BlobException(BLOB_READ_DENIED);
        Map<BlobFieldTarget,BlobRef> targets=new LinkedHashMap<>();
        for (int index=0;index<issued.requests().size();index++) {
            Request request=issued.requests().get(index); authorization.requireField(context,request.target());
            if (!issued.policies().get(index).equals(policies.resolve(BlobModel.field(EntityMetaFactory.requireFrom(context),request.target())))) throw new BlobException(BLOB_READ_DENIED);
            targets.put(request.target(),request.reference());
        }
        var current=catalog.readGraphBindings(issued.scope(),targets);
        if (!issued.facts().equals(current)) throw new BlobException(BLOB_READ_DENIED);
        for (int index=0;index<issued.requests().size();index++) {
            Request request=issued.requests().get(index); authorization.requireRead(context,request.target(),requireBlob(current,request));
            if (!clock.instant().isBefore(issued.reads().get(index).expiresAt())) throw new BlobException(BLOB_REFERENCE_EXPIRED);
        }
        if (options.documentExpiresAt()!=null && !clock.instant().isBefore(options.documentExpiresAt())) throw new BlobException(BLOB_REFERENCE_EXPIRED);
        requireScope(context,issued.scope());
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
