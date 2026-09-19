package io.teaql.core.businessid;

public interface BusinessIdProfile {
    BusinessIdPlan plan(BusinessIdGenerationRequest request);

    BusinessIdValue format(BusinessIdPlan plan, BusinessIdAllocation allocation);

    BusinessIdValue validate(BusinessIdDefinition definition, String value);
}
