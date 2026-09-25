package io.teaql.runtime.reference;

import java.util.Arrays;

/** AES key material used only by the runtime's default reference provider. */
public final class RoundTripReferenceKey {
    private final String keyId;
    private final byte[] keyBytes;

    public RoundTripReferenceKey(String keyId, byte[] keyBytes) {
        if (keyId == null || !keyId.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("keyId must be 1-32 URL-safe characters");
        }
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 key must contain exactly 32 bytes");
        }
        this.keyId = keyId;
        this.keyBytes = Arrays.copyOf(keyBytes, keyBytes.length);
    }

    public String keyId() {
        return keyId;
    }

    public byte[] keyBytes() {
        return Arrays.copyOf(keyBytes, keyBytes.length);
    }
}
