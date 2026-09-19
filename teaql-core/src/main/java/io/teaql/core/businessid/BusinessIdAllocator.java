package io.teaql.core.businessid;

public interface BusinessIdAllocator {
    BusinessIdAllocation allocate(BusinessIdPlan plan);
}
