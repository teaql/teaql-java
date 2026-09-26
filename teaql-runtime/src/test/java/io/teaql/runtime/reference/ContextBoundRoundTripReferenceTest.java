package io.teaql.runtime.reference;

import io.teaql.core.BaseEntity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.ReferenceMode;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.TeaQLRuntime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class ContextBoundRoundTripReferenceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");
    private static final String GOLDEN = "tqr1.k2.MzMzMzMzMzMzMzMzmhCET15vBdSc1BbFJcrvMMnRP1wAyjunShYlFPnDZL7Gk5V1LuptWyrbiQ8LQSDHFUmN3o6vb-MEgXZuh3N-YkeO55CYLeVgiyBBYslzuSu-fvWC2KMZcnZ5D8BRmWpGYA2Ok5sWe1Fg1aIyadTjVoKB-H2vPW_0okj-";

    private static final class OrderItem extends BaseEntity {
        OrderItem(long id, long version) {
            __internalSet("id", id);
            __internalSet("version", version);
        }

        @Override public String typeName() { return "OrderItem"; }
    }

    @Test
    public void governedVectorMatchesRustAndBindsActorDocumentAggregateAndType() {
        AeadRoundTripReferenceProvider provider = provider(DeploymentProfile.TEST, "test-a", null, null);
        UserContext alice = context(provider, "alice");
        Object reference = alice.referenceFor(new OrderItem(42, 3), scope(), Duration.ofSeconds(600));

        Assert.assertEquals(GOLDEN, reference);
        Assert.assertFalse(reference.toString().contains("OrderItem"));
        Assert.assertEquals(42, alice.resolveReference(reference, scope(), "OrderItem").id());
        Assert.assertEquals(ReferenceMode.GOVERNED,
                alice.resolveReference(reference, scope(), "OrderItem").mode());

        assertCode(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> context(provider, "bob").resolveReference(reference, scope(), "OrderItem"));
        assertCode(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> alice.resolveReference(reference,
                        new ReferenceDocumentScope("doc-101", "edit-order", "Order", 100, 9),
                        "OrderItem"));
        assertCode(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> alice.resolveReference(reference, scope(), "InvoiceItem"));
    }

    @Test
    public void expiryEnvironmentRotationTamperingAndCurrentAuthorizationFailClosed() {
        AtomicInteger authorizations = new AtomicInteger();
        AeadRoundTripReferenceProvider provider = provider(
                DeploymentProfile.TEST, "test-a", null,
                (context, principal, scope, identity) -> {
                    authorizations.incrementAndGet();
                    if (!"alice".equals(principal.subject())) {
                        throw new RoundTripReferenceException(
                                RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                                "ROUND_TRIP_REFERENCE_AUTHORIZATION_REQUIRED");
                    }
                });
        UserContext alice = context(provider, "alice");
        Object reference = alice.referenceFor(new OrderItem(42, 3), scope(), Duration.ofSeconds(600));
        alice.resolveReference(reference, scope(), "OrderItem");
        Assert.assertEquals("authorization must run at issue and consume", 2, authorizations.get());

        String token = reference.toString();
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");
        assertCode(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> alice.resolveReference(tampered, scope(), "OrderItem"));
        assertCode(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> context(provider(DeploymentProfile.TEST, "test-b", null, null), "alice")
                        .resolveReference(reference, scope(), "OrderItem"));

        provider.withClock(Clock.fixed(NOW.plusSeconds(601), ZoneOffset.UTC));
        assertCode(RoundTripReferenceErrorCode.EXPIRED,
                () -> alice.resolveReference(reference, scope(), "OrderItem"));

        AeadRoundTripReferenceProvider old = provider(DeploymentProfile.TEST, "test-a", "k1", null);
        UserContext beforeRotation = context(old, "alice");
        Object oldReference = beforeRotation.referenceFor(
                new OrderItem(42, 3), scope(), Duration.ofSeconds(600));
        Assert.assertEquals(42,
                context(provider(DeploymentProfile.TEST, "test-a", null, null), "alice")
                        .resolveReference(oldReference, scope(), "OrderItem").id());
    }

    @Test
    public void freshNoncesMakeRepeatedReferencesUnlinkable() {
        AtomicInteger nonce = new AtomicInteger(1);
        AeadRoundTripReferenceProvider provider = provider(
                DeploymentProfile.TEST, "test-a", null, null)
                .withNonceSource(() -> filledNonce(nonce.getAndIncrement()));
        UserContext context = context(provider, "alice");
        Object first = context.referenceFor(new OrderItem(42, 3), scope(), Duration.ofMinutes(10));
        Object second = context.referenceFor(new OrderItem(42, 3), scope(), Duration.ofMinutes(10));
        Assert.assertNotEquals(first, second);
        Assert.assertEquals(42, context.resolveReference(first, scope(), "OrderItem").id());
        Assert.assertEquals(42, context.resolveReference(second, scope(), "OrderItem").id());
    }

    @Test
    public void raw01UnsetUsesGovernedReferences() {
        Assert.assertEquals(ReferenceMode.GOVERNED,
                provider(DeploymentProfile.TEST, "test-a", null, null).mode());
    }

    @Test
    public void raw02ExactAcknowledgementEnablesDevelopmentRawMode() {
        Assert.assertEquals(ReferenceMode.RAW, raw(DeploymentProfile.DEVELOPMENT, null).mode());
    }

    @Test
    public void raw03ExactAcknowledgementEnablesTestRawMode() {
        Assert.assertEquals(ReferenceMode.RAW, raw(DeploymentProfile.TEST, null).mode());
    }

    @Test
    public void raw04NearMatchDoesNotEnableRawMode() {
        Assert.assertEquals(ReferenceMode.GOVERNED,
                provider(DeploymentProfile.TEST, "test-a", "near-match", null).mode());
    }

    @Test
    public void raw05ProductionFailsClosed() {
        assertCode(RoundTripReferenceErrorCode.CONFIGURATION_INVALID,
                () -> raw(DeploymentProfile.PRODUCTION, null));
    }

    @Test
    public void raw06AnotherActorIsRejectedByCurrentAuthorization() {
        AeadRoundTripReferenceProvider raw = raw(DeploymentProfile.TEST,
                (context, principal, scope, identity) -> {
                    if (!"alice".equals(principal.subject())) {
                        throw new RoundTripReferenceException(
                                RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                                "ROUND_TRIP_REFERENCE_AUTHORIZATION_REQUIRED");
                    }
                });
        Object wire = context(raw, "alice").referenceFor(
                new OrderItem(42, 3), scope(), Duration.ofMinutes(10));
        assertCode(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                () -> context(raw, "bob").resolveReference(wire, scope(), "OrderItem"));
    }

    @Test
    public void raw07ModeChangeRejectsOldWireShape() {
        AeadRoundTripReferenceProvider raw = raw(DeploymentProfile.TEST, null);
        assertCode(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> context(raw, "alice").resolveReference(GOLDEN, scope(), "OrderItem"));
        Object rawWire = context(raw, "alice").referenceFor(
                new OrderItem(42, 3), scope(), Duration.ofMinutes(10));
        assertCode(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> context(provider(DeploymentProfile.TEST, "test-a", null, null), "alice")
                        .resolveReference(rawWire, scope(), "OrderItem"));
    }

    @Test
    public void raw08RetainsVersionTypeAndAuthorizationGuards() {

        AtomicInteger authorizations = new AtomicInteger();
        AeadRoundTripReferenceProvider raw = raw(
                DeploymentProfile.DEVELOPMENT,
                (context, principal, scope, identity) -> authorizations.incrementAndGet());
        UserContext context = context(raw, "alice");
        Object wire = context.referenceFor(new OrderItem(42, 3), scope(), Duration.ofMinutes(10));
        Assert.assertEquals(Map.of("id", 42L, "version", 3L), wire);
        Assert.assertEquals(3, context.resolveReference(wire, scope(), "OrderItem").version());
        Assert.assertEquals("OrderItem",
                context.resolveReference(wire, scope(), "OrderItem").entityType());
        Assert.assertEquals(3, authorizations.get());
        assertCode(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> context.resolveReference(
                        Map.of("id", 42.5, "version", 3), scope(), "OrderItem"));
    }

    @Test
    public void raw09ExposesSafeDowngradeMetadataWithoutSecretMaterial() {
        AeadRoundTripReferenceProvider raw = raw(DeploymentProfile.DEVELOPMENT, null);
        ReferenceStartupNotice notice = raw.startupNotice().orElseThrow();
        Assert.assertEquals("ERROR", notice.level());
        Assert.assertEquals("raw", notice.telemetryValue());
        Assert.assertEquals("raw-internal-id",
                notice.responseHeaders().get("TeaQL-Reference-Mode"));
        Assert.assertEquals("no-store", notice.responseHeaders().get("Cache-Control"));
        Assert.assertFalse(notice.toString().contains("22".repeat(32)));
    }

    private static AeadRoundTripReferenceProvider raw(
            DeploymentProfile profile, RoundTripReferenceAuthorizationPolicy policy) {
        AeadRoundTripReferenceProvider raw = provider(
                profile, "test-a", RoundTripReferenceConfiguration.RAW_ID_ACKNOWLEDGEMENT, policy);
        Assert.assertEquals(ReferenceMode.RAW, raw.mode());
        return raw;
    }

    private static AeadRoundTripReferenceProvider provider(
            DeploymentProfile profile,
            String environment,
            String acknowledgement,
            RoundTripReferenceAuthorizationPolicy policy) {
        RoundTripReferenceKey current = new RoundTripReferenceKey(
                "k1".equals(acknowledgement) ? "k1" : "k2",
                filled("k1".equals(acknowledgement) ? 0x11 : 0x22));
        StaticRoundTripReferenceKeyProvider keys = "k1".equals(acknowledgement)
                ? new StaticRoundTripReferenceKeyProvider(current)
                : new StaticRoundTripReferenceKeyProvider(
                        current, new RoundTripReferenceKey("k1", filled(0x11)));
        return AeadRoundTripReferenceProvider.create(
                        profile, "order-service", environment, keys,
                        policy == null ? (context, principal, scope, identity) -> {} : policy,
                        "k1".equals(acknowledgement) ? null : acknowledgement)
                .withClock(Clock.fixed(NOW, ZoneOffset.UTC))
                .withNonceSource(() -> filledNonce(0x33));
    }

    private static UserContext context(AeadRoundTripReferenceProvider provider, String subject) {
        TeaQLRuntime runtime = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .roundTripReferenceProvider(provider)
                .build();
        return new DefaultUserContext(runtime).withTrustedReferencePrincipal(
                new TrustedReferencePrincipal("oidc", subject, "Platform", 7));
    }

    private static ReferenceDocumentScope scope() {
        return new ReferenceDocumentScope("doc-100", "edit-order", "Order", 100, 9);
    }

    private static byte[] filled(int value) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    private static byte[] filledNonce(int value) {
        byte[] bytes = new byte[12];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    private static void assertCode(RoundTripReferenceErrorCode code, Runnable action) {
        RoundTripReferenceException error = Assert.assertThrows(
                RoundTripReferenceException.class, action::run);
        Assert.assertEquals(code, error.getCode());
    }
}
