package io.teaql.runtime;

import io.teaql.core.*;
import io.teaql.data.dynamic.DynamicFieldMutation;
import java.lang.reflect.Array;
import java.util.*;

/** Invocation-local pin of the semantic values that customer policy reviewed. */
final class ReviewedMutationSnapshot {
    private final String requestKey, rootType, reason, comment;
    private final List<Operation> operations;
    private final Map<EntityKey,List<TraceNode>> traces;

    private ReviewedMutationSnapshot(MutationPlan plan,EntityMutationLedger ledger) {
        requestKey = plan.requestKey(); rootType = plan.rootEntityType();
        reason = plan.auditReason(); comment = ledger.getComment();
        operations = new ArrayList<>(plan.operations().size());
        traces = new LinkedHashMap<>();
        for (var operation : plan.operations()) {
            operations.add(new Operation(operation.kind(),operation.entity(),operation.originalVersion(),
                    freeze(operation.changedValues(),new IdentityHashMap<>())));
            var chain = ledger.getTraceChain(operation.entity());
            traces.put(operation.entity(),chain == null ? null : List.copyOf(chain));
        }
    }

    static ReviewedMutationSnapshot capture(MutationPlan plan,EntityMutationLedger ledger) {
        return new ReviewedMutationSnapshot(plan,ledger);
    }

    void requireUnchanged(MutationPlan plan,EntityMutationLedger ledger) {
        var live = capture(plan,ledger);
        if (!Objects.equals(requestKey,live.requestKey) || !Objects.equals(rootType,live.rootType)
                || !Objects.equals(reason,live.reason) || !Objects.equals(comment,live.comment)
                || !operations.equals(live.operations) || !traces.equals(live.traces)) {
            // Do not disclose field values, audit reasons or trace comments.
            throw new TeaQLRuntimeException("MUTATION_PLAN_CHANGED_AFTER_REVIEW: Pending graph changed after policy review; review and retry the current graph");
        }
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
