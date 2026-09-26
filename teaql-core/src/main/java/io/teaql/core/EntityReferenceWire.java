package io.teaql.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/** Portable wire helpers shared by the runtime codec and the explicit development bypass. */
public final class EntityReferenceWire {
    public static final String ENCRYPTED_PREFIX = "tqr1.";
    public static final String RAW_PREFIX = "tqr0.";
    public static final String ASSOCIATED_DATA = "teaql.entity-reference.v1";
    public static final String UNSAFE_ENVIRONMENT = "TEAQL_UNSAFE_RAW_ENTITY_REFERENCES";
    public static final String UNSAFE_ACKNOWLEDGEMENT =
            "I_UNDERSTAND_RAW_ENTITY_IDS_ARE_VISIBLE_FOR_LOCAL_DEVELOPMENT_ONLY";

    private EntityReferenceWire() {}

    public static byte[] encodeClaims(EntityReferenceClaims claims) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            writeText(output, claims.entityType());
            output.write(ByteBuffer.allocate(32)
                    .putLong(claims.id())
                    .putLong(claims.version())
                    .putLong(claims.issuedAt().getEpochSecond())
                    .putLong(claims.expiresAt().getEpochSecond())
                    .array());
            writeText(output, claims.purpose());
            return output.toByteArray();
        } catch (EntityReferenceTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    public static EntityReferenceClaims decodeClaims(byte[] data) {
        try {
            ByteBuffer input = ByteBuffer.wrap(data);
            String entityType = readText(input);
            if (input.remaining() < 32) throw invalid();
            long id = input.getLong();
            long version = input.getLong();
            long issuedAt = input.getLong();
            long expiresAt = input.getLong();
            String purpose = readText(input);
            if (input.hasRemaining() || entityType.isEmpty() || id <= 0) throw invalid();
            return new EntityReferenceClaims(
                    entityType, id, version, Instant.ofEpochSecond(issuedAt),
                    Instant.ofEpochSecond(expiresAt), purpose, 0);
        } catch (EntityReferenceTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    public static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static byte[] base64UrlDecode(String text) {
        try {
            return Base64.getUrlDecoder().decode(text);
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    public static boolean unsafeRawReferencesEnabled() {
        return UNSAFE_ACKNOWLEDGEMENT.equals(System.getenv(UNSAFE_ENVIRONMENT));
    }

    public static EntityReferenceTokenException invalid() {
        return new EntityReferenceTokenException("ENTITY_REFERENCE_INVALID");
    }

    private static void writeText(ByteArrayOutputStream output, String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0xffff) throw invalid();
        output.write((bytes.length >>> 8) & 0xff);
        output.write(bytes.length & 0xff);
        output.writeBytes(bytes);
    }

    private static String readText(ByteBuffer input) {
        if (input.remaining() < 2) throw invalid();
        int length = Short.toUnsignedInt(input.getShort());
        if (input.remaining() < length) throw invalid();
        byte[] bytes = new byte[length];
        input.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
