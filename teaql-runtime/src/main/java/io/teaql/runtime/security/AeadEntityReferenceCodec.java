package io.teaql.runtime.security;

import io.teaql.core.EntityReferenceClaims;
import io.teaql.core.EntityReferenceCodec;
import io.teaql.core.EntityReferenceTokenException;
import io.teaql.core.EntityReferenceWire;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Portable AES-256-GCM implementation of TeaQL opaque entity references. */
public final class AeadEntityReferenceCodec implements EntityReferenceCodec {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final int activeKeyVersion;
    private final Map<Integer, byte[]> keys;
    private Clock clock = Clock.systemUTC();
    private Supplier<byte[]> nonceSource = () -> {
        byte[] nonce = new byte[12];
        SECURE_RANDOM.nextBytes(nonce);
        return nonce;
    };

    public AeadEntityReferenceCodec(int activeKeyVersion, Map<Integer, byte[]> keys) {
        if (keys == null || !keys.containsKey(activeKeyVersion)) {
            throw new IllegalArgumentException("Active entity reference key is missing");
        }
        this.keys = new HashMap<>();
        keys.forEach((version, key) -> {
            if (key == null || key.length != 32) {
                throw new IllegalArgumentException("Every entity reference key must contain 32 bytes");
            }
            this.keys.put(version, Arrays.copyOf(key, key.length));
        });
        this.activeKeyVersion = activeKeyVersion;
    }

    public AeadEntityReferenceCodec withClock(Clock clock) {
        this.clock = java.util.Objects.requireNonNull(clock);
        return this;
    }

    public AeadEntityReferenceCodec withNonceSource(Supplier<byte[]> nonceSource) {
        this.nonceSource = java.util.Objects.requireNonNull(nonceSource);
        return this;
    }

    @Override
    public String encode(String entityType, long id, long version, String purpose, Duration lifetime) {
        if (entityType == null || entityType.isBlank() || id <= 0 || lifetime == null
                || lifetime.isZero() || lifetime.isNegative()) throw EntityReferenceWire.invalid();
        try {
            Instant now = clock.instant();
            byte[] plaintext = EntityReferenceWire.encodeClaims(new EntityReferenceClaims(
                    entityType, id, version, now, now.plus(lifetime), purpose == null ? "" : purpose, 0));
            byte[] nonce = nonceSource.get();
            if (nonce == null || nonce.length != 12) throw new IllegalArgumentException("AES-GCM nonce must contain 12 bytes");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keys.get(activeKeyVersion), "AES"),
                    new GCMParameterSpec(128, nonce));
            cipher.updateAAD(EntityReferenceWire.ASSOCIATED_DATA.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertextAndTag = cipher.doFinal(plaintext);
            byte[] envelope = ByteBuffer.allocate(4 + nonce.length + ciphertextAndTag.length)
                    .putInt(activeKeyVersion).put(nonce).put(ciphertextAndTag).array();
            return EntityReferenceWire.ENCRYPTED_PREFIX + EntityReferenceWire.base64Url(envelope);
        } catch (EntityReferenceTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new TeaqlEntityReferenceConfigurationException("Unable to encode entity reference", exception);
        }
    }

    @Override
    public EntityReferenceClaims decode(String token, String expectedEntityType, String purpose) {
        try {
            if (token == null || !token.startsWith(EntityReferenceWire.ENCRYPTED_PREFIX)) throw EntityReferenceWire.invalid();
            byte[] envelope = EntityReferenceWire.base64UrlDecode(token.substring(EntityReferenceWire.ENCRYPTED_PREFIX.length()));
            if (envelope.length < 4 + 12 + 16) throw EntityReferenceWire.invalid();
            ByteBuffer input = ByteBuffer.wrap(envelope);
            int keyVersion = input.getInt();
            byte[] key = keys.get(keyVersion);
            if (key == null) throw EntityReferenceWire.invalid();
            byte[] nonce = new byte[12];
            input.get(nonce);
            byte[] ciphertextAndTag = new byte[input.remaining()];
            input.get(ciphertextAndTag);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(EntityReferenceWire.ASSOCIATED_DATA.getBytes(StandardCharsets.UTF_8));
            EntityReferenceClaims decoded = EntityReferenceWire.decodeClaims(cipher.doFinal(ciphertextAndTag));
            EntityReferenceClaims claims = new EntityReferenceClaims(
                    decoded.entityType(), decoded.id(), decoded.version(), decoded.issuedAt(),
                    decoded.expiresAt(), decoded.purpose(), keyVersion);
            Instant now = clock.instant();
            if (!claims.expiresAt().isAfter(now) || claims.issuedAt().isAfter(now.plusSeconds(60))
                    || !claims.entityType().equals(expectedEntityType)
                    || !claims.purpose().equals(purpose)) throw EntityReferenceWire.invalid();
            return claims;
        } catch (EntityReferenceTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw EntityReferenceWire.invalid();
        }
    }

    public static final class TeaqlEntityReferenceConfigurationException extends RuntimeException {
        public TeaqlEntityReferenceConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
