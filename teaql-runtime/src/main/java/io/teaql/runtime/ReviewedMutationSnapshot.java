package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.data.dynamic.DynamicFieldMutation;
import java.lang.reflect.Array;
import java.util.*;

/** Invocation-local pin of the semantic values that customer policy reviewed. */
final class ReviewedMutationSnapshot {
    private final String requestKey, rootType, reason;
    private final List<Operation> operations;
    private final Map<EntityKey,List<TraceNode>> traces;
    private final MutationPlan prepared;

    private ReviewedMutationSnapshot(MutationPlan plan,EntityMutationLedger ledger,
            io.teaql.core.meta.EntityMetaFactory metadata) {
        requestKey = plan.requestKey(); rootType = plan.rootEntityType();
        reason = plan.auditReason();
        operations = new ArrayList<>(plan.operations().size());
        traces = new LinkedHashMap<>();
        for (var operation : plan.operations()) {
            operations.add(new Operation(operation.kind(),operation.entity(),operation.originalVersion(),
                    freeze(operation.changedValues(),new IdentityHashMap<>())));
            var chain = ledger.getTraceChain(operation.entity());
            traces.put(operation.entity(),chain == null ? null : List.copyOf(chain));
        }
        // The customer receives the original policy view, not this detached
        // provider payload. Mutable operands cannot rewrite the prepared writes.
        prepared = metadata == null ? plan : detach(plan,metadata);
    }

    static ReviewedMutationSnapshot capture(MutationPlan plan,EntityMutationLedger ledger) {
        return new ReviewedMutationSnapshot(plan,ledger,null);
    }

    static ReviewedMutationSnapshot capture(MutationPlan plan,EntityMutationLedger ledger,
            io.teaql.core.meta.EntityMetaFactory metadata) {
        return new ReviewedMutationSnapshot(plan,ledger,metadata);
    }

    MutationPlan preparedPlan() { return prepared; }

    List<TraceNode> traceChain(EntityKey key) { return traces.get(key); }

    void requireUnchanged(MutationPlan plan,EntityMutationLedger ledger) {
        requireUnchanged(plan,ledger,false);
    }

    void requireUnchanged(MutationPlan plan,EntityMutationLedger ledger,boolean providerCompleted) {
        var live = capture(plan,ledger);
        if (providerCompleted) {
            // Executed requests use the prepared original version, never this
            // live ledger value. Providers may now publish authoritative versions
            // through a legacy materialized entity without changing write inputs.
            Map<EntityKey,Long> reviewedVersions = new HashMap<>();
            operations.forEach(operation -> reviewedVersions.put(operation.key(),operation.version()));
            for (int i=0;i<live.operations.size();i++) {
                var operation = live.operations.get(i);
                live.operations.set(i,new Operation(operation.kind(),operation.key(),
                        reviewedVersions.get(operation.key()),operation.values()));
            }
        }
        if (!Objects.equals(requestKey,live.requestKey) || !Objects.equals(rootType,live.rootType)
                || !Objects.equals(reason,live.reason)
                || !operations.equals(live.operations) || !traces.equals(live.traces)) {
            // Do not disclose field values, audit reasons or trace comments.
            throw new TeaQLRuntimeException("MUTATION_PLAN_CHANGED_AFTER_REVIEW: Pending graph changed after policy review; review and retry the current graph");
        }
    }

    static MutationPlan detach(MutationPlan plan,io.teaql.core.meta.EntityMetaFactory metadata) {
        List<MutationOperation> copy = new ArrayList<>();
        for (var operation : plan.operations()) {
            Map<String,Object> values = new LinkedHashMap<>();
            operation.changedValues().forEach((field,value) -> values.put(field,detachValue(value,metadata)));
            copy.add(new MutationOperation(operation.kind(),operation.entity(),operation.originalVersion(),values));
        }
        return new MutationPlan(plan.executionId(),plan.requestKey(),plan.rootEntityType(),plan.auditReason(),copy);
    }

    private static Object detachValue(Object value,io.teaql.core.meta.EntityMetaFactory metadata) {
        if (value instanceof Entity entity) {
            var descriptor = metadata.resolveEntityDescriptor(entity.typeName());
            if (descriptor != null) try {
                var reference = (BaseEntity)descriptor.createEntity();
                reference.__internalSet("id",entity.getId());
                reference.__internalSet("version",entity.getVersion());
                reference.set$status(EntityStatus.REFER);
                return reference;
            } catch (IllegalStateException noFactory) { /* Opaque custom provider reference. */ }
            return value;
        }
        if (value instanceof DynamicFieldMutation mutation)
            return new DynamicFieldMutation(mutation.code(),mutation.dataType(),mutation.kind(),detachValue(mutation.value(),metadata));
        if (value instanceof Date time) return time.clone();
        if (value instanceof java.util.concurrent.atomic.AtomicInteger number) return number.get();
        if (value instanceof java.util.concurrent.atomic.AtomicLong number) return number.get();
        if (value instanceof Map<?,?> map) {
            Map<Object,Object> copy = new LinkedHashMap<>();
            map.forEach((key,item) -> copy.put(detachValue(key,metadata),detachValue(item,metadata)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof Iterable<?> items) {
            Collection<Object> copy = value instanceof Set ? new LinkedHashSet<>() : new ArrayList<>();
            items.forEach(item -> copy.add(detachValue(item,metadata)));
            return copy instanceof Set ? Collections.unmodifiableSet((Set<?>)copy) : Collections.unmodifiableList((List<?>)copy);
        }
        if (value != null && value.getClass().isArray()) {
            int size = Array.getLength(value);
            Object copy = Array.newInstance(value.getClass().getComponentType(),size);
            if (value.getClass().getComponentType().isPrimitive()) System.arraycopy(value,0,copy,0,size);
            else for (int i=0;i<size;i++) Array.set(copy,i,detachValue(Array.get(value,i),metadata));
            return copy;
        }
        return value;
    }

    private record Operation(MutationOperationKind kind,EntityKey key,Long version,Object values) {}
    private record Reference(String type,Long id) {}
    private record CalendarValue(Class<?> type,long millis,int nanos) {}
    private record Dynamic(String code,Object type,Object kind,Object value) {}
    private record ArrayValue(Class<?> type,Object values) {
        @Override public boolean equals(Object other) {
            return other instanceof ArrayValue array && type.equals(array.type) && Objects.deepEquals(values,array.values);
        }
        @Override public int hashCode() { return 31*type.hashCode()+Arrays.deepHashCode(new Object[]{values}); }
    }

    private static Object freeze(Object value,IdentityHashMap<Object,Boolean> active) {
        if (value instanceof Entity entity) return new Reference(entity.typeName(),entity.getId());
        if (value instanceof DynamicFieldMutation mutation)
            return new Dynamic(mutation.code(),mutation.dataType(),mutation.kind(),freeze(mutation.value(),active));
        if (value instanceof Date time) {
            // Date's virtual conversion preserves Timestamp nanoseconds without
            // adding a JDBC/java.sql dependency to the domain-independent runtime.
            int nanos = 0;
            try { nanos = time.toInstant().getNano(); }
            catch (UnsupportedOperationException dateOnly) { /* SQL Date/Time expose millis only. */ }
            return new CalendarValue(value.getClass(),time.getTime(),nanos);
        }
        if (value instanceof java.util.concurrent.atomic.AtomicInteger number) return number.get();
        if (value instanceof java.util.concurrent.atomic.AtomicLong number) return number.get();
        boolean structured = value instanceof Map || value instanceof Iterable || value != null && value.getClass().isArray();
        // Native scalar values (String, boxed numerics, BigDecimal and java.time)
        // are immutable. Opaque custom provider values keep their own equality contract.
        if (!structured) return value;
        if (active.put(value,Boolean.TRUE) != null)
            throw new TeaQLRuntimeException("MUTATION_REVIEW_CYCLIC_VALUE: Cyclic container is not a mutation scalar");
        try {
            if (value instanceof Map<?,?> map) {
                Map<Object,Object> copy = new LinkedHashMap<>();
                map.forEach((key,item) -> copy.put(freeze(key,active),freeze(item,active)));
                return Collections.unmodifiableMap(copy);
            }
            if (value instanceof Set<?> items) {
                Set<Object> copy = new HashSet<>();
                items.forEach(item -> copy.add(freeze(item,active)));
                return Collections.unmodifiableSet(copy);
            }
            if (value instanceof Iterable<?> items) {
                List<Object> copy = new ArrayList<>();
                items.forEach(item -> copy.add(freeze(item,active)));
                return Collections.unmodifiableList(copy);
            }
            int size = Array.getLength(value);
            if (value.getClass().getComponentType().isPrimitive()) {
                Object copy = Array.newInstance(value.getClass().getComponentType(),size);
                System.arraycopy(value,0,copy,0,size);
                return new ArrayValue(value.getClass(),copy);
            }
            Object[] copy = new Object[size];
            for (int i=0;i<size;i++) copy[i] = freeze(Array.get(value,i),active);
            return new ArrayValue(value.getClass(),copy);
        } finally { active.remove(value); }
    }
}
