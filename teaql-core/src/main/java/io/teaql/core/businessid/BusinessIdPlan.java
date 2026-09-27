package io.teaql.core.businessid;

import java.time.LocalDate;

public record BusinessIdPlan(
        BusinessIdDefinition definition,
        BusinessIdScope scope,
        LocalDate businessDate,
        String dateText,
        long initialSequence,
        long maximumSequence) {
    public BusinessIdPlan {
        if (initialSequence < 0 || maximumSequence < initialSequence) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID,
                    "Business ID allocation range must satisfy 0 <= initialSequence <= maximumSequence");
        }
    }
}
