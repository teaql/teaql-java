package io.teaql.core;

import java.util.*;

/**
 * Central change tracking context shared across all entities in a save graph.
 * Holds the change set stack, deleted/recovered/new keys, trace chains, and original versions.
 *
 * This is the Java equivalent of Rust's {@code EntityMutationLedger}.
 */
public class EntityMutationLedger {
    private final ChangeSetStack changeSets = new ChangeSetStack();
    private String comment;
    private final Set<EntityKey> deletedKeys = new TreeSet<>();
    private final Set<EntityKey> recoveredKeys = new TreeSet<>();
    private final Set<EntityKey> newKeys = new TreeSet<>();
    private final Map<EntityKey, List<TraceNode>> traceChains = new TreeMap<>();
    private final Map<EntityKey, Long> originalVersions = new TreeMap<>();
    private final List<Submission> submissions = new ArrayList<>();
    private Object submissionOwner;
    private boolean outcomeUnknown;

    // --- Change Set Stack ---

    public void pushChangeSet() {
        requireNoSubmissions();
        changeSets.push();
    }

    public EntityChangeSet popChangeSet() {
        requireNoSubmissions();
        return changeSets.pop();
    }

    public void clearCurrentChangeSet() {
        requireNoSubmissions();
        clearWorkingChanges();
    }

    private void clearWorkingChanges() {
        changeSets.clearCurrent();
        newKeys.clear();
        deletedKeys.clear();
        recoveredKeys.clear();
        // A successful save establishes a new persistence baseline. Keeping the
        // pre-save version here makes a later mutation on the same entity use a
        // stale optimistic-lock value (for example update -> save -> delete ->
        // save). The materialized entity now carries the authoritative version
        // returned by the provider, so the next mutation must capture that value.
        originalVersions.clear();
        traceChains.clear();
    }

    /** Submitted intent remains retained until the owning physical transaction resolves. */
    public boolean hasUncommittedChanges() { return !submissions.isEmpty(); }
    public boolean hasUnknownTransactionOutcome() { return outcomeUnknown; }

    @FrameworkInternal("Validate the physical owner before a graph can write again")
    public void requireSubmissionOwner(Object owner) {
        if (outcomeUnknown) throw new TransactionCompletionException(TransactionCompletion.Outcome.UNKNOWN,null);
        if (submissionOwner!=null && submissionOwner!=owner) throw new TeaQLRuntimeException("Graph has an unfinished physical transaction");
    }

    @FrameworkInternal("Retain submitted working intent while later edits form a separate batch")
    public Submission submit(Object owner) {
        Objects.requireNonNull(owner); requireSubmissionOwner(owner);
        Submission value=new Submission(this,copyWorking());
        submissionOwner=owner; submissions.add(value); clearWorkingChanges();
        return value;
    }

    private void requireNoSubmissions() {
        if (!submissions.isEmpty()) throw new TeaQLRuntimeException("Cannot discard an unfinished transaction journal");
    }

    private EntityMutationLedger copyWorking() {
        EntityMutationLedger copy=new EntityMutationLedger(); copy.mergeFrom(this); copy.comment=comment; return copy;
    }

    /** A framework-owned journal ticket, never a durable commit or retry credential. */
    public static final class Submission {
        private final EntityMutationLedger ledger,changes;
        private TransactionCompletion.Outcome outcome;
        private Submission(EntityMutationLedger ledger,EntityMutationLedger changes) { this.ledger=ledger; this.changes=changes; }

        @FrameworkInternal("Physical owner completion only")
        public void complete(TransactionCompletion.Outcome value) {
            Objects.requireNonNull(value);
            if (outcome!=null) {
                if (outcome==value) return;
                throw new TeaQLRuntimeException("Transaction journal outcome cannot change");
            }
            if (value==TransactionCompletion.Outcome.UNKNOWN) {
                ledger.outcomeUnknown=true; outcome=value; return;
            }
            if (value==TransactionCompletion.Outcome.COMMITTED) {
                // All registered batches for this owner are physically committed.
                // Retire their intent before the first audit delivery can reenter.
                for (Submission pending:ledger.submissions) pending.outcome=value;
                ledger.submissions.clear(); ledger.submissionOwner=null; return;
            }
            if (ledger.submissions.isEmpty() || ledger.submissions.get(ledger.submissions.size()-1)!=this) {
                throw new TeaQLRuntimeException("Transaction journal must unwind in reverse order");
            }
            EntityMutationLedger later=ledger.copyWorking();
            ledger.clearWorkingChanges(); ledger.mergeFrom(changes); ledger.mergeFrom(later);
            // A retry guards the earliest restored database baseline, not the
            // provisional version captured by a later batch in this transaction.
            changes.originalVersions.forEach(ledger.originalVersions::put);
            ledger.submissions.remove(ledger.submissions.size()-1); outcome=value;
            if (ledger.submissions.isEmpty()) ledger.submissionOwner=null;
        }
    }

    @FrameworkInternal("Rebase retained lifecycle intent onto the rolled-back persistence baseline")
    public boolean rebaseAfterRollback(EntityKey key,Long version,boolean created) {
        if (created) {
            if (deletedKeys.remove(key)) {
                newKeys.remove(key); recoveredKeys.remove(key); changeSets.clearEntity(key); originalVersions.remove(key); return true;
            }
            recoveredKeys.remove(key); newKeys.add(key); return false;
        }
        if (version!=null && version>0) recoveredKeys.remove(key);
        if (version!=null && version<0 && deletedKeys.remove(key)) {
            recoveredKeys.remove(key); changeSets.clearEntity(key);
        }
        return false;
    }

    public void set(EntityKey key, String field, Object value) {
        changeSets.set(key, field, value);
    }

    public void setDynamic(EntityKey key, io.teaql.data.dynamic.DynamicFieldMutation mutation) {
        changeSets.currentMut().setDynamic(key, mutation);
    }

    public Object get(EntityKey key, String field) {
        return changeSets.get(key, field);
    }

    public EntityChangeSet currentChangeSet() {
        EntityChangeSet cs = changeSets.current();
        return cs != null ? cs : new EntityChangeSet();
    }

    // --- Comment ---

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    // --- New Keys ---

    public void markAsNew(EntityKey key) {
        newKeys.add(key);
    }

    public boolean isNew(EntityKey key) {
        return newKeys.contains(key);
    }

    public Set<EntityKey> newKeys() {
        return Collections.unmodifiableSet(newKeys);
    }

    // --- Deleted Keys ---

    public void markAsDelete(EntityKey key) {
        changeSets.clearEntity(key);
        recoveredKeys.remove(key);
        deletedKeys.add(key);
    }

    public boolean isMarkedAsDelete(EntityKey key) {
        return deletedKeys.contains(key);
    }

    public Set<EntityKey> deletedKeys() {
        return Collections.unmodifiableSet(deletedKeys);
    }

    // Recovery is a mutation even when no scalar property has changed.
    public void markAsRecover(EntityKey key) {
        deletedKeys.remove(key);
        recoveredKeys.add(key);
    }

    public Set<EntityKey> recoveredKeys() {
        return Collections.unmodifiableSet(recoveredKeys);
    }

    // --- Changed Fields ---

    public Set<String> changedFieldNames(EntityKey key) {
        return changeSets.changedFieldNames(key);
    }

    // --- Trace Chains ---

    public void setTraceChain(EntityKey key, List<TraceNode> traceChain) {
        traceChains.put(key, traceChain == null ? List.of() : List.copyOf(traceChain));
    }

    public List<TraceNode> getTraceChain(EntityKey key) {
        return traceChains.get(key);
    }

    // --- Original Versions ---

    public void setOriginalVersion(EntityKey key, Long version) {
        originalVersions.put(key, version);
    }

    public Long getOriginalVersion(EntityKey key) {
        return originalVersions.get(key);
    }

    /**
     * Imports only the explicitly visited entity's pending mutation. A loaded
     * reference may be shared by otherwise independent graphs; importing its
     * entire ledger could pull in an unrelated root's changes. Returns false
     * for a read-only entity, which must retain its private ledger ownership.
     */
    public boolean mergeEntityFrom(EntityMutationLedger other, EntityKey key) {
        if (other == null || other == this) return false;
        Map<String, Object> fields = other.currentChangeSet().changes().get(key);
        var dynamic = other.currentChangeSet().dynamicChanges().get(key);
        boolean pending = (fields != null && !fields.isEmpty()) || other.newKeys.contains(key)
                || other.deletedKeys.contains(key) || other.recoveredKeys.contains(key) || (dynamic != null && !dynamic.isEmpty());
        if (!pending) return false;
        if (other.hasUncommittedChanges()) throw new TeaQLRuntimeException("Cannot move a pending graph journal to another graph");
        if (fields != null) fields.forEach((field, value) -> set(key, field, value));
        if (dynamic != null) dynamic.values().forEach(mutation -> setDynamic(key, mutation));
        if (other.deletedKeys.contains(key)) markAsDelete(key);
        if (other.recoveredKeys.contains(key)) markAsRecover(key);
        if (other.newKeys.contains(key)) markAsNew(key);
        if (other.traceChains.containsKey(key)) setTraceChain(key, other.traceChains.get(key));
        if (other.originalVersions.containsKey(key)) setOriginalVersion(key, other.originalVersions.get(key));
        return true;
    }

    /**
     * Merge another EntityMutationLedger's changes into this one.
     * Used when saving an entity graph (e.g., Order + OrderItems).
     */
    public void mergeFrom(EntityMutationLedger other) {
        if (other == null) return;
        
        // Merge change sets
        EntityChangeSet otherChangeSet = other.currentChangeSet();
        otherChangeSet.dynamicChanges().forEach((key, mutations) -> mutations.values().forEach(mutation -> setDynamic(key, mutation)));
        for (Map.Entry<EntityKey, Map<String, Object>> entry : otherChangeSet.changes().entrySet()) {
            EntityKey key = entry.getKey();
            for (Map.Entry<String, Object> fieldEntry : entry.getValue().entrySet()) {
                this.set(key, fieldEntry.getKey(), fieldEntry.getValue());
            }
        }
        
        // Merge deleted keys
        for (EntityKey key : other.deletedKeys()) {
            this.markAsDelete(key);
        }

        for (EntityKey key : other.recoveredKeys()) {
            this.markAsRecover(key);
        }
        
        // Merge new keys
        for (EntityKey key : other.newKeys()) {
            this.markAsNew(key);
        }
        
        // Merge trace chains
        for (Map.Entry<EntityKey, List<TraceNode>> entry : other.traceChains.entrySet()) {
            this.setTraceChain(entry.getKey(), entry.getValue());
        }
        
        // Merge original versions
        for (Map.Entry<EntityKey, Long> entry : other.originalVersions.entrySet()) {
            this.setOriginalVersion(entry.getKey(), entry.getValue());
        }
    }
}
