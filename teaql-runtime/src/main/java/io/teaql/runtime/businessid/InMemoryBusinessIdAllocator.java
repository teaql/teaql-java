package io.teaql.runtime.businessid;

import io.teaql.core.businessid.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** Single-process development/test allocator. */
public final class InMemoryBusinessIdAllocator implements BusinessIdAllocator {
    private final ConcurrentMap<BusinessIdScope, AtomicLong> counters =
            new ConcurrentHashMap<>();

    @Override
    public BusinessIdAllocation allocate(BusinessIdPlan plan) {
        AtomicLong counter = counters.computeIfAbsent(
                plan.scope(), ignored -> new AtomicLong());
        while (true) {
            long current = counter.get();
            if (current >= plan.maximumSequence()) {
                throw new BusinessIdException(
                        BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                        "Business ID range exhausted for " + plan.scope().canonicalKey());
            }
            long next = current + 1;
            if (counter.compareAndSet(current, next)) {
                return new BusinessIdAllocation(plan.scope(), next);
            }
        }
    }
}
