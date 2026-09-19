package io.teaql.core.businessid;

import io.teaql.core.UserContext;

/**
 * Provider-owned Business ID schema contribution.
 *
 * <p>Installing an allocator is passive. TeaQL invokes this hook only from the
 * explicit {@link UserContext#ensureSchema()} lifecycle.
 */
public interface BusinessIdSchemaContributor {
    void ensureSchema(UserContext context);
}
