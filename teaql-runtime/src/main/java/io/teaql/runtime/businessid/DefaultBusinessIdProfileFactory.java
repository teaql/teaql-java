package io.teaql.runtime.businessid;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.*;

public final class DefaultBusinessIdProfileFactory implements BusinessIdProfileFactory {
    private static final DailySequenceBusinessIdProfile DAILY =
            new DailySequenceBusinessIdProfile();

    @Override
    public BusinessIdProfile create(UserContext context, BusinessIdDefinition definition) {
        if (BusinessIdDefinition.DEFAULT_PROFILE.equals(definition.profile())) {
            return DAILY;
        }
        throw new BusinessIdException(
                BusinessIdErrorCode.BUSINESS_ID_PROFILE_NOT_FOUND,
                "Business ID profile is not registered: " + definition.profile());
    }
}
