package io.teaql.runtime.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import io.teaql.core.EntityReferenceClaims;
import io.teaql.core.EntityReferenceTokenException;
import io.teaql.runtime.DefaultUserContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

public class AeadEntityReferenceCodecTest {
    private static final String GOLDEN = "tqr1.AAAAAjMzMzMzMzMzMzMzM3bKiZgRSQQhfIj2cBXRDZIloUGHWLBp8QrXL_aejwIXPFtvV_E71O7wbOXy3cvYo_SwxvuS-89x572T9CO_pDAY4tbjWCNv";
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    public void referenceIsPortableOpaqueBoundRotatableAndExpiring() {
        byte[] key = new byte[32];
        byte[] nonce = new byte[12];
        Arrays.fill(key, (byte) 0x22);
        Arrays.fill(nonce, (byte) 0x33);
        AeadEntityReferenceCodec codec = new AeadEntityReferenceCodec(2, Map.of(2, key))
                .withClock(Clock.fixed(NOW, ZoneOffset.UTC))
                .withNonceSource(() -> nonce.clone());
        DefaultUserContext context = new DefaultUserContext(null);
        context.withEntityReferenceCodec(codec);

        String token = context.encodeEntityReference(
                "OrderItem", 42, 7, "edit-order", Duration.ofHours(1));
        assertEquals(GOLDEN, token);
        assertFalse(token.contains("OrderItem"));
        assertFalse(token.contains("42"));
        EntityReferenceClaims claims = context.decodeEntityReference(token, "OrderItem", "edit-order");
        assertEquals(42, claims.id());
        assertEquals(7, claims.version());
        assertEquals(2, claims.keyVersion());

        assertThrows(EntityReferenceTokenException.class,
                () -> context.decodeEntityReference(token, "InvoiceItem", "edit-order"));
        assertThrows(EntityReferenceTokenException.class,
                () -> context.decodeEntityReference(token, "OrderItem", "other-purpose"));
        assertThrows(EntityReferenceTokenException.class,
                () -> context.decodeEntityReference(token.substring(0, token.length() - 1) + "A", "OrderItem", "edit-order"));

        codec.withClock(Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC));
        assertThrows(EntityReferenceTokenException.class,
                () -> context.decodeEntityReference(token, "OrderItem", "edit-order"));
    }

    @Test
    public void missingCodecFailsClosedWithoutExactDevelopmentAcknowledgement() {
        DefaultUserContext context = new DefaultUserContext(null);
        EntityReferenceTokenException error = assertThrows(EntityReferenceTokenException.class,
                () -> context.encodeEntityReference("Order", 1, 1, "edit", Duration.ofMinutes(1)));
        assertEquals("ENTITY_REFERENCE_CODEC_REQUIRED", error.code());
    }
}
