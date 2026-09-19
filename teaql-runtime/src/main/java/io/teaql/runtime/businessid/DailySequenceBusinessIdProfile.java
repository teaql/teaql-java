package io.teaql.runtime.businessid;

import io.teaql.core.businessid.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DailySequenceBusinessIdProfile implements BusinessIdProfile {
    @Override
    public BusinessIdPlan plan(BusinessIdGenerationRequest request) {
        BusinessIdDefinition definition = request.definition();
        if (!BusinessIdDefinition.DEFAULT_PROFILE.equals(definition.profile())
                || !"daily".equals(definition.reset())) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID,
                    "daily-sequence requires reset=daily");
        }
        String dateText;
        try {
            dateText = request.businessDate()
                    .format(DateTimeFormatter.ofPattern(definition.dateFormat()));
        } catch (IllegalArgumentException error) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID,
                    "Invalid Business ID date format: " + definition.dateFormat(), error);
        }
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
                definition.maximumSequence());
    }

    @Override
    public BusinessIdValue format(BusinessIdPlan plan, BusinessIdAllocation allocation) {
        if (!plan.scope().equals(allocation.scope())) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID,
                    "Allocation scope does not match Business ID plan");
        }
        long sequence = allocation.sequence();
        if (sequence < 1 || sequence > plan.maximumSequence()) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_RANGE_EXHAUSTED,
                    "Business ID range exhausted for " + plan.scope().canonicalKey());
        }
        BusinessIdDefinition definition = plan.definition();
        String value = definition.prefix()
                + definition.separator()
                + plan.dateText()
                + definition.separator()
                + String.format("%0" + definition.digits() + "d", sequence);
        return new BusinessIdValue(value, definition.profile(), definition.policyVersion());
    }

    @Override
    public BusinessIdValue validate(BusinessIdDefinition definition, String value) {
        if (value == null || value.isBlank()) {
            throw invalid(value);
        }
        String separator = java.util.regex.Pattern.quote(definition.separator());
        String[] parts = value.split(separator, -1);
        if (parts.length != 3
                || !definition.prefix().equals(parts[0])
                || parts[2].length() != definition.digits()
                || !parts[2].chars().allMatch(Character::isDigit)) {
            throw invalid(value);
        }
        try {
            java.time.LocalDate.parse(
                    parts[1], DateTimeFormatter.ofPattern(definition.dateFormat()));
        } catch (DateTimeParseException | IllegalArgumentException error) {
            throw invalid(value);
        }
        return new BusinessIdValue(value, definition.profile(), definition.policyVersion());
    }

    private BusinessIdException invalid(String value) {
        return new BusinessIdException(
                BusinessIdErrorCode.BUSINESS_ID_FORMAT_INVALID,
                "Invalid daily-sequence Business ID: " + value);
    }
}
