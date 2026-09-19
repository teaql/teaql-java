package io.teaql.core.businessid;

public record BusinessIdScope(
        String domainRootKey,
        String aggregateType,
        String namespace,
        String periodKey) {

    public BusinessIdScope {
        domainRootKey = require(domainRootKey, "domainRootKey");
        aggregateType = require(aggregateType, "aggregateType");
        namespace = require(namespace, "namespace");
        periodKey = require(periodKey, "periodKey");
    }

    public String canonicalKey() {
        return escape(domainRootKey) + "|" + escape(aggregateType) + "|"
                + escape(namespace) + "|" + escape(periodKey);
    }

    private static String escape(String value) {
        return value.replace("%", "%25").replace("|", "%7C");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID,
                    field + " must not be blank");
        }
        return value;
    }
}
