package io.teaql.core.businessid;

import io.teaql.core.UserContext;

/** Resolves current key material without placing secrets in generated code or metadata. */
public interface BusinessIdKeyProvider {
    BusinessIdEncodingKey currentKey(
            UserContext context, BusinessIdDefinition definition, BusinessIdScope scope);
}
