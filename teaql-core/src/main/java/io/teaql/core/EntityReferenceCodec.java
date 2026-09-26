package io.teaql.core;

import java.time.Duration;

/** Provider SPI for boundary-safe entity references. */
public interface EntityReferenceCodec {
    String encode(String entityType, long id, long version, String purpose, Duration lifetime);

    EntityReferenceClaims decode(String token, String expectedEntityType, String purpose);
}
