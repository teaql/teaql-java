package io.teaql.secure.document.reference;

import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;

public class DocumentSnapshotProtocolTest {
    private static final Instant NOW = Instant.ofEpochSecond(1_800_000_000L);

    private static DocumentSnapshot snapshot() {
        var scope = new ReferenceDocumentScope("doc-1", "edit-order", "Order", 10, 4);
        return new DocumentSnapshot("model-v1", "ORD-X1", scope,
                new InternalEntityIdentity("Order", 10, 4),
                List.of(new DocumentSnapshot.Entity("item:0",
                        new InternalEntityIdentity("OrderItem", 20, 2),
                        Set.of("product_code", "quantity"), Set.of("quantity"),
                        Set.of(DocumentSnapshot.MutationKind.UPDATE))),
                List.of(new DocumentSnapshot.Relation("items", List.of("item:0"),
                        DocumentSnapshot.Completeness.COMPLETE, true,
                        Set.of(DocumentSnapshot.MutationKind.CREATE_CHILD,
                                DocumentSnapshot.MutationKind.REMOVE_CHILD))));
    }

    private static TrustedReferencePrincipal principal(String subject) {
        return new TrustedReferencePrincipal("oidc", subject, "Merchant", 7);
    }

    private static AeadRoundTripReferenceProvider provider(
            String environment, String currentKey, boolean retainOld,
            RoundTripReferenceAuthorizationPolicy authorization, Instant now) {
        byte[] old = new byte[32];
        Arrays.fill(old, (byte) 0x42);
        byte[] next = new byte[32];
        Arrays.fill(next, (byte) 0x43);
        var keys = "k1".equals(currentKey)
                ? new StaticRoundTripReferenceKeyProvider(new RoundTripReferenceKey("k1", old))
                : retainOld
                        ? new StaticRoundTripReferenceKeyProvider(
                                new RoundTripReferenceKey("k2", next),
                                new RoundTripReferenceKey("k1", old))
                        : new StaticRoundTripReferenceKeyProvider(
                                new RoundTripReferenceKey("k2", next));
        byte[] nonce = new byte[12];
        Arrays.fill(nonce, (byte) 0x33);
        return AeadRoundTripReferenceProvider.governed(DeploymentProfile.TEST,
                "order-service", environment, keys, authorization)
                .withClock(Clock.fixed(now, ZoneOffset.UTC))
                .withNonceSource(() -> nonce.clone());
    }

    private static UserContext context(AeadRoundTripReferenceProvider provider, String actor) {
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .roundTripReferenceProvider(provider).build();
        return new DefaultUserContext(runtime).withTrustedReferencePrincipal(principal(actor));
    }

    private static void code(RoundTripReferenceErrorCode expected, Runnable action) {
        var error = Assert.assertThrows(RoundTripReferenceException.class, action::run);
        Assert.assertEquals(expected, error.getCode());
    }

    @Test
    public void tqd1RoundTripsAndBindsActorEnvironmentPurposeAndCurrentAuthority() throws Exception {
        AtomicBoolean allowed = new AtomicBoolean(true);
        var provider = provider("test", "k1", false, (context, principal, scope, identity) -> {
            if (!allowed.get()) {
                throw new RoundTripReferenceException(
                        RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                        "ROUND_TRIP_REFERENCE_AUTHORIZATION_REQUIRED");
            }
        }, NOW);
        var alice = context(provider, "alice");
        String token = alice.issueDocumentSnapshot(snapshot(), Duration.ofSeconds(600));
        Assert.assertEquals("69fee1ec01cd4a5b69dd3f795aeaffb3a3546e50d48e25cfc9b8f169a527b267",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(token.getBytes(java.nio.charset.StandardCharsets.US_ASCII))));
        Assert.assertTrue(token.startsWith("tqd1.k1."));
        Assert.assertEquals(snapshot(), alice.consumeDocumentSnapshot(
                token, "Order", "edit-order").snapshot());
        Assert.assertEquals("k1", provider.consumeDocumentSnapshot(alice,
                principal("alice"), token, "Order", "edit-order").keyId());
        code(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> provider.consumeDocumentSnapshot(context(provider, "bob"),
                        principal("bob"), token, "Order", "edit-order"));
        code(RoundTripReferenceErrorCode.CONTEXT_MISMATCH,
                () -> provider.consumeDocumentSnapshot(context(provider, "bob"),
                        principal("alice"), token, "Order", "edit-order"));
        code(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> provider.consumeDocumentSnapshot(alice,
                        principal("alice"), token, "Invoice", "edit-order"));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> provider.consumeDocumentSnapshot(alice,
                        principal("alice"), token, "Order", "view-order"));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> provider("production", "k1", false,
                        (c, p, s, i) -> {}, NOW).consumeDocumentSnapshot(
                                alice, principal("alice"), token, "Order", "edit-order"));
        allowed.set(false);
        code(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                () -> provider.consumeDocumentSnapshot(alice,
                        principal("alice"), token, "Order", "edit-order"));
    }

    @Test
    public void tqd1RotationExpiryAndProjectionRulesFailClosed() {
        var provider = provider("test", "k1", false, (c, p, s, i) -> {}, NOW);
        var alice = context(provider, "alice");
        String token = provider.issueDocumentSnapshot(alice, principal("alice"),
                snapshot(), Duration.ofSeconds(60));
        String tampered = token.substring(0, token.length() - 2) + "AB";
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> provider.consumeDocumentSnapshot(alice,
                        principal("alice"), tampered, "Order", "edit-order"));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> provider.consumeDocumentSnapshot(alice,
                        principal("alice"), token + "=", "Order", "edit-order"));
        var rotated = provider("test", "k2", true, (c, p, s, i) -> {}, NOW.plusSeconds(30));
        Assert.assertEquals("k1", rotated.consumeDocumentSnapshot(
                context(rotated, "alice"), principal("alice"), token,
                "Order", "edit-order").keyId());
        var expired = provider("test", "k2", true, (c, p, s, i) -> {}, NOW.plusSeconds(61));
        code(RoundTripReferenceErrorCode.EXPIRED,
                () -> expired.consumeDocumentSnapshot(context(expired, "alice"),
                        principal("alice"), token, "Order", "edit-order"));
        code(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> new DocumentSnapshot.Entity("item:0",
                        new InternalEntityIdentity("OrderItem", 20, 2),
                        Set.of("product_code"), Set.of("quantity"),
                        Set.of(DocumentSnapshot.MutationKind.UPDATE)));
        code(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> new DocumentSnapshot.Relation("items", List.of("missing"),
                        DocumentSnapshot.Completeness.PARTIAL, false,
                        Set.of(DocumentSnapshot.MutationKind.CREATE_CHILD)));
    }
}
