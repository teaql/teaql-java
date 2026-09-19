package io.teaql.runtime.businessid;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.*;

public final class DefaultBusinessIdService implements BusinessIdService {
    private final BusinessIdAllocator allocator;

    public DefaultBusinessIdService(BusinessIdAllocator allocator) {
        this.allocator = java.util.Objects.requireNonNull(allocator, "allocator");
    }

    @Override
    public BusinessIdValue ensure(
            UserContext context,
            BusinessIdDefinition definition,
            String domainRootKey,
            String aggregateType,
            BusinessIdSlot slot) {
        BusinessIdProfileFactory factory = context.capability(BusinessIdProfileFactory.class);
        if (factory == null) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_PROFILE_NOT_FOUND,
                    "BusinessIdProfileFactory capability is not registered");
        }
        BusinessIdProfile profile = factory.create(context, definition);
        String current = slot.currentValue();
        if (current != null && !current.isBlank()) {
            return profile.validate(definition, current);
        }
        if (!slot.newAggregate()) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_IMMUTABLE,
                    "An established Aggregate cannot be assigned a new Business ID");
        }
        BusinessIdGenerationRequest request = new BusinessIdGenerationRequest(
                definition,
                domainRootKey,
                aggregateType,
                context.businessDate());
        BusinessIdPlan plan = profile.plan(request);
        BusinessIdValue value = profile.format(plan, allocator.allocate(plan));
        slot.assignCanonicalValue(value.value());
        return value;
    }
}
