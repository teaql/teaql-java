package io.teaql.runtime.businessid;

import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessIdDefinition;
import io.teaql.core.businessid.BusinessIdEncodingKey;
import io.teaql.core.businessid.BusinessIdKeyProvider;
import io.teaql.core.businessid.BusinessIdScope;

/** Deterministic key provider for tests and explicitly configured single-key deployments. */
public final class StaticBusinessIdKeyProvider implements BusinessIdKeyProvider {
    private final BusinessIdEncodingKey key;

    public StaticBusinessIdKeyProvider(BusinessIdEncodingKey key) {
        this.key = java.util.Objects.requireNonNull(key, "key");
    }

    @Override
    public BusinessIdEncodingKey currentKey(
            UserContext context, BusinessIdDefinition definition, BusinessIdScope scope) {
        return key;
    }
}
