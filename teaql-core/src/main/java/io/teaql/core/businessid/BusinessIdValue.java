package io.teaql.core.businessid;

public record BusinessIdValue(String value, String profile, int policyVersion) {
    public BusinessIdValue {
        if (value == null || value.isBlank()) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_FORMAT_INVALID,
                    "Business ID value must not be blank");
        }
    }
}
