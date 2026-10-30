package io.teaql.blob.reference;

import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobPolicyProfile.Sharing;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static io.teaql.core.blob.BlobException.Code.*;

/** Authoritative binding writes on the same adapter and physical transaction as the business save. */
public final class PostgresBlobMutationParticipant implements BlobMutationParticipant {
    private final PostgresBlobCatalog catalog;
    private final BlobPolicies policies;
    private final BlobBindingAuthorization authorization;
    private final Clock clock;

    public PostgresBlobMutationParticipant(PostgresBlobCatalog catalog, BlobPolicies policies,
            BlobBindingAuthorization authorization, Clock clock) {
        this.catalog=Objects.requireNonNull(catalog); this.policies=Objects.requireNonNull(policies);
        this.authorization=Objects.requireNonNull(authorization); this.clock=Objects.requireNonNull(clock);
    }

    @Override public Plan plan(UserContext context, MutationPlan approvedPlan) {
        BlobBindingIntents intents=BlobBindingIntents.from(context,approvedPlan);
        return new Plan() {
            @Override public void requireTransaction(Object resource) { catalog.requireResource(resource); }
            @Override public Prepared prepare(UserContext current,Object resource) {
                return catalog.guarded(() -> prepareBindings(current,resource,intents));
            }
        };
    }

    private Prepared prepareBindings(UserContext context,Object resource,BlobBindingIntents intents) {
        Object transaction=catalog.requireActive(resource);
        catalog.validateSchema(); BlobScope scope=authorization.scope(context);
        List<Pending> pending=new ArrayList<>();
        SortedSet<String> keys=new TreeSet<>();
        Set<BlobFieldTarget> targets=new HashSet<>();
        var sorted=new ArrayList<>(intents.mutations());
        sorted.sort(Comparator.comparing(BlobBindingIntents.Mutation::entity));
        for (var mutation:sorted) {
            for (var entry:new TreeMap<>(mutation.fields()).entrySet()) {
                BlobFieldTarget target=new BlobFieldTarget(mutation.entity().entity(),mutation.entity().id(),entry.getKey());
                if (!targets.add(target)) throw new BlobException(BLOB_REFERENCE_INVALID);
                authorization.requireWrite(context,target,mutation.kind());
                PostgresBlobCatalog.Binding previous=catalog.lockBinding(scope,target);
                if (mutation.kind()==MutationOperationKind.CREATE && (previous.version()!=0 || previous.reference()!=null)) {
                    throw new BlobException(BLOB_REFERENCE_INVALID);
                }
                if (mutation.kind()==MutationOperationKind.RECOVER && previous.reference()!=null) throw new BlobException(BLOB_REFERENCE_INVALID);
                BlobRef next;
                if (mutation.kind()==MutationOperationKind.DELETE) next=null;
                else if (mutation.assignments().containsKey(entry.getKey())) next=mutation.assignments().get(entry.getKey());
                else if (mutation.kind()==MutationOperationKind.RECOVER) next=previous.retiredReference();
                else throw new BlobException(BLOB_REFERENCE_INVALID);
                if (next==null && !entry.getValue().nullable() && mutation.kind()!=MutationOperationKind.DELETE) throw new BlobException(BLOB_BIND_DENIED);
                BlobPolicySnapshot policy=policies.resolve(entry.getValue());
                if (previous.reference()!=null) keys.add(previous.reference().key());
                if (next!=null) keys.add(next.key());
                pending.add(new Pending(mutation.kind(),mutation.entity(),target,previous,next,policy));
            }
        }
        Map<BlobRef,BlobCatalogRecord> locked=new LinkedHashMap<>();
        for (String key:keys) {
            BlobRef reference=BlobRef.of(key); locked.put(reference,catalog.lockBlob(scope,reference));
        }
        validateAssignments(context,pending,locked,clock.instant());
        return new PreparedBindings(resource,transaction,scope,pending,locked);
    }

    private record Pending(MutationOperationKind kind,EntityKey planned,BlobFieldTarget target,
                           PostgresBlobCatalog.Binding previous,BlobRef next,BlobPolicySnapshot policy) {}

    private void validateAssignments(UserContext context,List<Pending> pending,Map<BlobRef,BlobCatalogRecord> locked,Instant now) {
        Map<BlobRef,Long> counts=new HashMap<>(); Set<BlobRef> single=new HashSet<>();
        for (var blob:locked.values()) {
            counts.put(blob.reference(),catalog.bindingCount(blob.reference()));
            if (blob.singleBinding()) single.add(blob.reference());
        }
        for (Pending change:pending) {
            authorization.requireWrite(context,change.target(),change.kind());
            BlobRef old=change.previous().reference();
            if (!Objects.equals(old,change.next())) {
                if (old!=null) counts.compute(old,(key,count) -> Objects.requireNonNull(count)-1);
                if (change.next()!=null) counts.compute(change.next(),(key,count) -> Objects.requireNonNull(count)+1);
            }
            if (change.next()==null) continue;
            BlobCatalogRecord blob=locked.get(change.next());
            authorization.requireBind(context,change.target(),blob);
            Instant eligible=blob.unreferencedAt()==null?null:BlobRetention.unreferencedEligibleAt(blob.unreferencedAt(),
                    Duration.ofSeconds(blob.effectiveUnreferencedSeconds()),blob.retentionFloorUntil());
            BlobRetention.requireBindable(blob.state(),now,blob.completedAt(),blob.policy().unboundRetention(),eligible);
            if (blob.sizeBytes()==null || blob.contentVersion()==null || blob.sha256()==null) throw new BlobException(BLOB_UPLOAD_INCOMPLETE);
            if (!change.policy().field().allowedExtensions().contains(blob.extension())
                    || !change.policy().field().allowedContentTypes().contains(blob.contentType())) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
            if (blob.sizeBytes()>change.policy().maxSizeBytes() || blob.sizeBytes()==0 && !change.policy().allowEmpty()) throw new BlobException(BLOB_SIZE_EXCEEDED);
            if (change.policy().sharing()==Sharing.SINGLE_BINDING) single.add(change.next());
        }
        for (var count:counts.entrySet()) {
            if (count.getValue()<0) throw new BlobException(BLOB_REFERENCE_INVALID);
            if (count.getValue()>1 && single.contains(count.getKey())) throw new BlobException(BLOB_BIND_DENIED);
        }
    }

    private final class PreparedBindings implements Prepared {
        private final Object resource;
        private final Object transaction;
        private final BlobScope scope;
        private final List<Pending> pending;
        private final Map<BlobRef,BlobCatalogRecord> locked;
        private boolean applied;
        PreparedBindings(Object resource,Object transaction,BlobScope scope,List<Pending> pending,Map<BlobRef,BlobCatalogRecord> locked) {
            this.resource=resource; this.transaction=transaction; this.scope=scope;
            this.pending=List.copyOf(pending); this.locked=Map.copyOf(locked);
        }
        @Override public void apply(UserContext context,Object currentResource,List<PersistedEntity> entities) {
            catalog.guarded(() -> {
                if (currentResource!=resource) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
                catalog.requireActive(currentResource,transaction);
                if (applied) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
                applied=true;
                if (!scope.equals(authorization.scope(context))) throw new BlobException(BLOB_BIND_DENIED);
                Instant now=clock.instant();
                validateAssignments(context,pending,locked,now);
                Map<EntityKey,PersistedEntity> resolved=new HashMap<>();
                for (var entity:entities) if (resolved.put(entity.plannedEntity(),entity)!=null) throw new BlobException(BLOB_REFERENCE_INVALID);
                for (Pending change:pending) {
                    PersistedEntity persisted=resolved.get(change.planned());
                    if (persisted==null || persisted.kind()!=change.kind()
                            || (change.kind()==MutationOperationKind.DELETE)!=(persisted.version()<0)
                            || persisted.version()==Long.MIN_VALUE) throw new BlobException(BLOB_REFERENCE_INVALID);
                    PostgresBlobCatalog.Binding previous=change.previous();
                    if (!persisted.persistedEntity().equals(change.planned())) {
                        if (change.kind()!=MutationOperationKind.CREATE) throw new BlobException(BLOB_REFERENCE_INVALID);
                        BlobFieldTarget actual=new BlobFieldTarget(persisted.persistedEntity().entity(),persisted.persistedEntity().id(),change.target().field());
                        authorization.requireWrite(context,actual,change.kind());
                        if (change.next()!=null) authorization.requireBind(context,actual,locked.get(change.next()));
                        previous=catalog.lockBinding(scope,actual);
                        if (previous.version()!=0 || previous.reference()!=null || previous.retiredReference()!=null) throw new BlobException(BLOB_REFERENCE_INVALID);
                    }
                    long revision;
                    try { revision=Math.max(Math.addExact(previous.version(),1),Math.abs(persisted.version())); }
                    catch (ArithmeticException overflow) { throw new BlobException(BLOB_REFERENCE_INVALID); }
                    Instant boundAt=change.next()==null?null:now;
                    Instant retainUntil=change.next()==null?null:PostgresBlobCatalog.plus(now,change.policy().minimumRetention());
                    if (change.next()!=null && change.next().equals(previous.reference()) && previous.retainUntil()!=null
                            && previous.retainUntil().isAfter(retainUntil)) retainUntil=previous.retainUntil();
                    BlobRef retired=change.kind()==MutationOperationKind.DELETE?previous.reference():null;
                    catalog.writeBinding(previous,change.next(),retired,revision,boundAt,retainUntil);
                    if (change.next()!=null) catalog.acceptPolicy(locked.get(change.next()),change.policy(),retainUntil);
                }
                List<BlobRef> keys=new ArrayList<>(locked.keySet()); keys.sort(Comparator.comparing(BlobRef::key));
                for (BlobRef key:keys) catalog.refreshReferences(scope,key,now);
                return null;
            });
        }
    }
}
