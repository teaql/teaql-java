package io.teaql.runtime.reference;

import io.teaql.core.UserContext;

/** Selects the runtime's current or decode-only key using the complete context. */
public interface RoundTripReferenceKeyProvider {
    RoundTripReferenceKey currentKey(UserContext context);

    RoundTripReferenceKey keyById(UserContext context, String keyId);
}
