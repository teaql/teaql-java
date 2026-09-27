package io.teaql.runtime.businessid;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.*;

public final class DefaultBusinessIdProfileFactory implements BusinessIdProfileFactory {
    private static final DailySequenceBusinessIdProfile DAILY =
            new DailySequenceBusinessIdProfile();

    @Override
    public BusinessIdProfile create(UserContext context, BusinessIdDefinition definition) {
        if (BusinessIdDefinition.DEFAULT_PROFILE.equals(definition.profile())) {
            BusinessIdKeyProvider keyProvider = context.capability(BusinessIdKeyProvider.class);
            if (keyProvider == null) {
                throw new BusinessIdException(
                        BusinessIdErrorCode.BUSINESS_ID_KEY_NOT_FOUND,
                        "BusinessIdKeyProvider capability is not registered");
            }
            return new PermutedDailyBusinessIdProfile(context, keyProvider);
        }
        if (BusinessIdDefinition.LEGACY_DAILY_SEQUENCE_PROFILE.equals(definition.profile())) {
            return DAILY;
        }
        throw new BusinessIdException(
                BusinessIdErrorCode.BUSINESS_ID_PROFILE_NOT_FOUND,
                "Business ID profile is not registered: " + definition.profile());
    }
}
