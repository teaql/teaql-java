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
        long sequence = counters
                .computeIfAbsent(plan.scope(), ignored -> new AtomicLong())
                .incrementAndGet();
        if (sequence > plan.maximumSequence()) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                    "Business ID range exhausted for " + plan.scope().canonicalKey());
        }
        return new BusinessIdAllocation(plan.scope(), sequence);
    }
}
