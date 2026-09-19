package io.teaql.core.businessid;

public record BusinessIdDefinition(
        String fieldName,
        String profile,
        String prefix,
        String dateFormat,
        String reset,
        int digits,
        String separator,
        String namespace,
        int policyVersion) {

    public static final String DEFAULT_PROFILE = "daily-sequence";
    public static final String DEFAULT_DATE_FORMAT = "yyyyMMdd";
    public static final int DEFAULT_DIGITS = 8;

    public BusinessIdDefinition {
        fieldName = requireText(fieldName, "fieldName");
        profile = requireText(profile, "profile");
        prefix = requireText(prefix, "prefix");
        dateFormat = requireText(dateFormat, "dateFormat");
        reset = requireText(reset, "reset");
        separator = requireText(separator, "separator");
        namespace = requireText(namespace, "namespace");
        if (digits < 1 || digits > 18) {
            throw invalid("digits must be between 1 and 18");
        }
        if (policyVersion < 1) {
            throw invalid("policyVersion must be positive");
        }
    }

    public static BusinessIdDefinition dailySequence(
            String fieldName, String prefix, String namespace) {
        return new BusinessIdDefinition(
                fieldName,
                DEFAULT_PROFILE,
                prefix,
                DEFAULT_DATE_FORMAT,
                "daily",
                DEFAULT_DIGITS,
                "-",
                namespace,
                1);
    }

    public long maximumSequence() {
        long maximum = 1;
        for (int i = 0; i < digits; i++) {
            maximum = Math.multiplyExact(maximum, 10);
        }
        return maximum - 1;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + " must not be blank");
        }
        return value;
    }

    private static BusinessIdException invalid(String message) {
        return new BusinessIdException(
                BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID, message);
    }
}
