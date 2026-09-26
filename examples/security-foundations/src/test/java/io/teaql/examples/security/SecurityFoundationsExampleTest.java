package io.teaql.examples.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.teaql.core.EntityReferenceTokenException;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.security.AeadEntityReferenceCodec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

public class SecurityFoundationsExampleTest {
    @Test
    public void userContextProtectsBoundaryReferences() {
        byte[] key = new byte[32];
        byte[] nonce = new byte[12];
        Arrays.fill(key, (byte) 0x22);
        Arrays.fill(nonce, (byte) 0x33);
        AeadEntityReferenceCodec codec = new AeadEntityReferenceCodec(2, Map.of(2, key))
                .withClock(Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC))
                .withNonceSource(() -> nonce.clone());
        DefaultUserContext context = new DefaultUserContext(null);
        context.withEntityReferenceCodec(codec);
        String token = context.encodeEntityReference("OrderItem", 42, 7, "edit-order", Duration.ofHours(1));
        assertEquals(42, context.decodeEntityReference(token, "OrderItem", "edit-order").id());
        assertThrows(EntityReferenceTokenException.class,
                () -> context.decodeEntityReference(token, "OrderItem", "view-order"));
        System.out.println("PASS Java security foundations: opaque reference is portable and purpose-bound");
    }
}
