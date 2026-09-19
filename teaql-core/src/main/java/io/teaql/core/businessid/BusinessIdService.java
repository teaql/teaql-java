package io.teaql.core.businessid;

import io.teaql.core.UserContext;

public interface BusinessIdService {
    BusinessIdValue ensure(
            UserContext context,
            BusinessIdDefinition definition,
            String domainRootKey,
            String aggregateType,
            BusinessIdSlot slot);
}
