package io.teaql.runtime.businessid;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Default six-character Base36 Business ID profile. */
public final class PermutedDailyBusinessIdProfile implements BusinessIdProfile {
    private final UserContext context;
    private final BusinessIdKeyProvider keyProvider;

    public PermutedDailyBusinessIdProfile(
            UserContext context, BusinessIdKeyProvider keyProvider) {
        this.context = java.util.Objects.requireNonNull(context, "context");
        this.keyProvider = java.util.Objects.requireNonNull(keyProvider, "keyProvider");
    }

    @Override
    public BusinessIdPlan plan(BusinessIdGenerationRequest request) {
        BusinessIdDefinition definition = request.definition();
        if (!BusinessIdDefinition.DEFAULT_PROFILE.equals(definition.profile())
                || !"daily".equals(definition.reset())
                || !BusinessIdDefinition.DEFAULT_DATE_FORMAT.equals(definition.dateFormat())
                || definition.digits() != BusinessIdPermutationV1.WIDTH) {
            throw invalidDefinition(
                    "daily-permuted-v1 requires reset=daily, dateFormat=yyyyMMdd and digits=6");
        }
        String dateText = request.businessDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        BusinessIdScope scope = new BusinessIdScope(
                request.domainRootKey(),
                request.aggregateType(),
                definition.namespace(),
                dateText);
        return new BusinessIdPlan(
                definition,
                scope,
                request.businessDate(),
                dateText,
                0,
                BusinessIdPermutationV1.MAX_SEQUENCE);
    }

    @Override
    public BusinessIdValue format(BusinessIdPlan plan, BusinessIdAllocation allocation) {
        if (!plan.scope().equals(allocation.scope())) {
            throw invalidDefinition("Allocation scope does not match Business ID plan");
        }
        BusinessIdEncodingKey key = keyProvider.currentKey(
                context, plan.definition(), plan.scope());
        if (key == null) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_KEY_NOT_FOUND,
                    "Business ID key provider returned no current key");
        }
        String code = BusinessIdPermutationV1.encode(allocation.sequence(), plan.scope(), key);
        BusinessIdDefinition definition = plan.definition();
        String value = definition.prefix()
                + definition.separator()
                + plan.dateText()
                + definition.separator()
                + code;
        return new BusinessIdValue(value, definition.profile(), definition.policyVersion());
    }

    @Override
    public BusinessIdValue validate(BusinessIdDefinition definition, String value) {
        if (value == null || value.isBlank()) {
            throw invalidFormat(value);
        }
        String[] parts = value.split(Pattern.quote(definition.separator()), -1);
        if (parts.length != 3
                || !definition.prefix().equals(parts[0])
                || parts[2].length() != BusinessIdPermutationV1.WIDTH
                || !parts[2].chars().allMatch(PermutedDailyBusinessIdProfile::isBase36Uppercase)) {
            throw invalidFormat(value);
        }
        try {
            java.time.LocalDate.parse(parts[1], DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException | IllegalArgumentException error) {
            throw invalidFormat(value);
        }
        return new BusinessIdValue(value, definition.profile(), definition.policyVersion());
    }

    private static boolean isBase36Uppercase(int value) {
        return (value >= '0' && value <= '9') || (value >= 'A' && value <= 'Z');
    }

    private BusinessIdException invalidDefinition(String message) {
        return new BusinessIdException(
                BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID, message);
    }

    private BusinessIdException invalidFormat(String value) {
        return new BusinessIdException(
                BusinessIdErrorCode.BUSINESS_ID_FORMAT_INVALID,
                "Invalid daily-permuted-v1 Business ID: " + value);
    }
}
