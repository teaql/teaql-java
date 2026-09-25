package io.teaql.runtime.reference;

import io.teaql.core.UserContext;
import io.teaql.core.reference.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM round-trip references with context-specific key selection. */
public final class AeadRoundTripReferenceProvider implements RoundTripReferenceProvider {
    private static final String PREFIX = "tqr1";
    private static final byte PAYLOAD_VERSION = 1;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final RoundTripReferenceKeyProvider keys;
    private final SecureRandom random;

    public AeadRoundTripReferenceProvider(RoundTripReferenceKeyProvider keys) {
        this(keys, new SecureRandom());
    }

    AeadRoundTripReferenceProvider(RoundTripReferenceKeyProvider keys, SecureRandom random) {
        if (keys == null || random == null) throw new IllegalArgumentException("keys and random are required");
        this.keys = keys;
        this.random = random;
    }

    @Override
    public RoundTripReference issue(UserContext context, InternalEntityIdentity identity) {
        RoundTripReferenceKey key = requireKey(keys.currentKey(context));
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        try {
            byte[] encrypted = cipher(Cipher.ENCRYPT_MODE, key, nonce)
                    .doFinal(encode(identity));
            byte[] envelope = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, envelope, 0, nonce.length);
            System.arraycopy(encrypted, 0, envelope, nonce.length, encrypted.length);
            return new RoundTripReference(PREFIX + "." + key.keyId() + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(envelope));
        } catch (GeneralSecurityException | IOException error) {
            throw invalid("Unable to issue round-trip reference", error);
        }
    }

    @Override
    public ResolvedRoundTripReference resolve(
            UserContext context, RoundTripReference reference, String expectedEntityType) {
        String[] parts = reference.value().split("\\.", -1);
        if (parts.length != 3 || !PREFIX.equals(parts[0])) {
            throw invalid("Invalid round-trip reference", null);
        }
        RoundTripReferenceKey key = keys.keyById(context, parts[1]);
        if (key == null) {
            throw invalid("Round-trip reference is unavailable", null);
        }
        try {
            byte[] envelope = Base64.getUrlDecoder().decode(parts[2]);
            if (envelope.length <= NONCE_LENGTH + 16) throw invalid("Invalid round-trip reference", null);
            byte[] nonce = java.util.Arrays.copyOfRange(envelope, 0, NONCE_LENGTH);
            byte[] encrypted = java.util.Arrays.copyOfRange(envelope, NONCE_LENGTH, envelope.length);
            InternalEntityIdentity identity = decode(
                    cipher(Cipher.DECRYPT_MODE, key, nonce).doFinal(encrypted));
            if (expectedEntityType == null || !identity.entityType().equals(expectedEntityType)) {
                throw new RoundTripReferenceException(
                        RoundTripReferenceErrorCode.TYPE_MISMATCH,
                        "Round-trip reference does not match the expected entity type");
            }
            return new ResolvedRoundTripReference(
                    identity.entityType(), identity.id(), identity.version());
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (AEADBadTagException error) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.CONTEXT_MISMATCH,
                    "Round-trip reference is invalid for the current runtime context");
        } catch (GeneralSecurityException | IOException | IllegalArgumentException error) {
            throw invalid("Invalid round-trip reference", error);
        }
    }

    private static Cipher cipher(int mode, RoundTripReferenceKey key, byte[] nonce)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key.keyBytes(), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD((PREFIX + "." + key.keyId()).getBytes(StandardCharsets.US_ASCII));
        return cipher;
    }

    private static byte[] encode(InternalEntityIdentity identity) throws IOException {
        byte[] entityType = identity.entityType().getBytes(StandardCharsets.UTF_8);
        if (entityType.length == 0 || entityType.length > 65535) {
            throw new IOException("entity type length is invalid");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeByte(PAYLOAD_VERSION);
            output.writeShort(entityType.length);
            output.write(entityType);
            output.writeLong(identity.id());
            output.writeLong(identity.version());
        }
        return bytes.toByteArray();
    }

    private static InternalEntityIdentity decode(byte[] payload) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readUnsignedByte() != PAYLOAD_VERSION) throw new IOException("unsupported payload version");
            int length = input.readUnsignedShort();
            byte[] entityType = input.readNBytes(length);
            if (entityType.length != length) throw new EOFException("truncated entity type");
            long id = input.readLong();
            long version = input.readLong();
            if (input.read() != -1) throw new IOException("trailing payload data");
            return new InternalEntityIdentity(new String(entityType, StandardCharsets.UTF_8), id, version);
        }
    }

    private static RoundTripReferenceKey requireKey(RoundTripReferenceKey key) {
        if (key == null) {
            throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.CONTEXT_BINDING_REQUIRED,
                    "Runtime customization did not provide reference binding material");
        }
        return key;
    }

    private static RoundTripReferenceException invalid(String message, Throwable cause) {
        return cause == null
                ? new RoundTripReferenceException(RoundTripReferenceErrorCode.INVALID_REFERENCE, message)
                : new RoundTripReferenceException(RoundTripReferenceErrorCode.INVALID_REFERENCE, message, cause);
    }
}
