package io.teaql.core.secret;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.time.Instant;

/** Small secret wrapper; plaintext access must be an explicit adapter action. */
@JsonSerialize(using = SecretValue.BlockedSerializer.class)
public final class SecretValue {
    public static final int MAX_BYTES = 16 * 1024;
    private final byte[] bytes;
    private final String version;
    private final Instant expiresAt;

    public SecretValue(byte[] bytes, String version, Instant expiresAt) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new SecretFailure(SecretFailure.Code.INVALID_INPUT);
        }
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new SecretFailure(SecretFailure.Code.EXPIRED);
        }
        this.bytes = bytes.clone();
        this.version = version;
        this.expiresAt = expiresAt;
    }

    /** Deliberate adapter-only plaintext access. Never log or serialize the result. */
    public byte[] exposeBytes() {
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new SecretFailure(SecretFailure.Code.EXPIRED);
        }
        return bytes.clone();
    }

    public String version() {
        return version;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override public String toString() {
        return "SecretValue[masked]";
    }

    public static final class BlockedSerializer extends JsonSerializer<SecretValue> {
        @Override public void serialize(SecretValue value, JsonGenerator generator,
                                        SerializerProvider serializers) throws IOException {
            throw new IOException("secret serialization denied");
        }
    }
}
