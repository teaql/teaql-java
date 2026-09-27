package io.teaql.core.businessid;

import java.util.Arrays;

/** Versioned 256-bit key material supplied by an application-owned provider. */
public final class BusinessIdEncodingKey {
    private final int version;
    private final byte[] bytes;

    public BusinessIdEncodingKey(int version, byte[] bytes) {
        if (version < 1) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID,
                    "Business ID key version must be positive");
        }
        if (bytes == null || bytes.length != 32) {
            throw new BusinessIdException(
                    BusinessIdErrorCode.BUSINESS_ID_DEFINITION_INVALID,
                    "Business ID V1 key must contain exactly 32 bytes");
        }
        this.version = version;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public int version() {
        return version;
    }

    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }
}
