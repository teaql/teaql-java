package io.teaql.core.businessid;

import io.teaql.core.UserContext;

public interface BusinessIdProfileFactory {
    BusinessIdProfile create(UserContext context, BusinessIdDefinition definition);
}
