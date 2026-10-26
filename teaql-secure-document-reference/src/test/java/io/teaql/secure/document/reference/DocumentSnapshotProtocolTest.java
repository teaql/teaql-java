package io.teaql.secure.document.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.BoundDocumentSnapshot;
import io.teaql.core.reference.BlobSelectionClaim;
import io.teaql.core.reference.BlobSelectionTarget;
import io.teaql.core.reference.DocumentBlobBindingSnapshot;
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
import java.nio.ByteBuffer;
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

    private static BoundDocumentSnapshot boundSnapshot() {
        var original = snapshot();
        var item = original.entities().get(0);
        var entity = new DocumentSnapshot.Entity(item.rowKey(), item.identity(),
                Set.of("product_code", "quantity", "attachment"),
                Set.of("quantity", "attachment"), item.allowedMutations());
        var document = new DocumentSnapshot(original.modelFingerprint(), original.businessId(),
                original.scope(), original.aggregate(), List.of(entity), original.relations());
        return new BoundDocumentSnapshot(document,
                List.of(new DocumentBlobBindingSnapshot("item:0", "attachment", "OrderItem",
                        BlobRef.of("blob_A"), -1L, true, true)));
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

    @Test
    public void tqd2RoundTripsEncryptedBindingAndRejectsOldProtocolAndForeignContext() {
        AtomicBoolean allowed = new AtomicBoolean(true);
        var provider = provider("test", "k1", false, (c, p, s, i) -> {
            if (!allowed.get()) throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                    "ROUND_TRIP_REFERENCE_AUTHORIZATION_REQUIRED");
        }, NOW);
        var alice = context(provider, "alice");
        String token = alice.issueBoundDocumentSnapshot(boundSnapshot(), Duration.ofSeconds(600));
        Assert.assertTrue(token.startsWith("tqd2.k1."));
        Assert.assertFalse(token.contains("blob_A"));
        Assert.assertEquals(boundSnapshot(), alice.consumeBoundDocumentSnapshot(
                token, "Order", "edit-order").document());
        Assert.assertEquals("k1", alice.consumeBoundDocumentSnapshot(
                token, "Order", "edit-order").keyId());
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> alice.consumeDocumentSnapshot(token, "Order", "edit-order"));
        String legacy = alice.issueDocumentSnapshot(snapshot(), Duration.ofSeconds(600));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> alice.consumeBoundDocumentSnapshot(legacy, "Order", "edit-order"));
        code(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> context(provider, "bob").consumeBoundDocumentSnapshot(
                        token, "Order", "edit-order"));
        code(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> alice.consumeBoundDocumentSnapshot(token, "Invoice", "edit-order"));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> alice.consumeBoundDocumentSnapshot(token, "Order", "other-purpose"));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> context(provider("production", "k1", false,
                        (c, p, s, i) -> {}, NOW), "alice")
                        .consumeBoundDocumentSnapshot(token, "Order", "edit-order"));
        allowed.set(false);
        code(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                () -> alice.consumeBoundDocumentSnapshot(token, "Order", "edit-order"));
    }

    @Test
    public void tqd2RotationExpiryAndMalformedBindingWireFailClosed() {
        var provider = provider("test", "k1", false, (c, p, s, i) -> {}, NOW);
        var alice = context(provider, "alice");
        String token = alice.issueBoundDocumentSnapshot(boundSnapshot(), Duration.ofSeconds(60));
        var rotated = provider("test", "k2", true, (c, p, s, i) -> {}, NOW.plusSeconds(30));
        Assert.assertEquals(boundSnapshot(), context(rotated, "alice")
                .consumeBoundDocumentSnapshot(token, "Order", "edit-order").document());
        var expired = provider("test", "k2", true, (c, p, s, i) -> {}, NOW.plusSeconds(61));
        code(RoundTripReferenceErrorCode.EXPIRED,
                () -> context(expired, "alice").consumeBoundDocumentSnapshot(
                        token, "Order", "edit-order"));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> alice.consumeBoundDocumentSnapshot(token + "=", "Order", "edit-order"));

        byte[] actor = new byte[32];
        Arrays.fill(actor, (byte) 0x11);
        byte[] wire = DocumentSnapshotWire.encodeBound(boundSnapshot(), actor, 10, 20);
        Assert.assertEquals(boundSnapshot(), DocumentSnapshotWire.decodeBound(wire).document());
        byte[] trailing = Arrays.copyOf(wire, wire.length + 1);
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> DocumentSnapshotWire.decodeBound(trailing));
        byte[] invalidFlag = wire.clone();
        invalidFlag[invalidFlag.length - 1] = 2;
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> DocumentSnapshotWire.decodeBound(invalidFlag));
        int base = DocumentSnapshotWire.encode(boundSnapshot().snapshot(), actor, 10, 20).length;
        int hasKeyOffset = base + 4;
        for (int index = 0; index < 3; index++) {
            hasKeyOffset += 4 + ByteBuffer.wrap(wire, hasKeyOffset, 4).getInt();
        }
        byte[] invalidKeyFlag = wire.clone();
        invalidKeyFlag[hasKeyOffset] = 2;
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> DocumentSnapshotWire.decodeBound(invalidKeyFlag));
        byte[] oldVersion = wire.clone();
        oldVersion[0] = 1;
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> DocumentSnapshotWire.decodeBound(oldVersion));
        byte[] duplicated = Arrays.copyOf(wire, wire.length + wire.length - base - 4);
        ByteBuffer.wrap(duplicated, base, 4).putInt(2);
        System.arraycopy(wire, base + 4, duplicated, wire.length, wire.length - base - 4);
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> DocumentSnapshotWire.decodeBound(duplicated));
        code(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> new BoundDocumentSnapshot(snapshot(), boundSnapshot().bindings()));
    }

    @Test
    public void tqs1BindsCurrentTqd2ActorTargetCommandAndExpiryWithoutEarlyConsumption()
            throws Exception {
        AtomicBoolean allowed = new AtomicBoolean(true);
        var provider = provider("test", "k1", false, (c, p, s, i) -> {
            if (!allowed.get()) throw new RoundTripReferenceException(
                    RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                    "ROUND_TRIP_REFERENCE_AUTHORIZATION_REQUIRED");
        }, NOW);
        var alice = context(provider, "alice");
        String document = alice.issueBoundDocumentSnapshot(
                boundSnapshot(), Duration.ofSeconds(300));
        var target = new BlobSelectionTarget(boundSnapshot().snapshot().scope(),
                "OrderItem", "attachment", BlobSelectionTarget.Kind.EXISTING_ROW,
                "item:0", "command-1");
        var claim = new BlobSelectionClaim(target, BlobRef.of("blob_B"));
        String selection = alice.issueBlobSelection(document, claim, Duration.ofSeconds(120));
        Assert.assertEquals("e7c81ca089b0a3e0585faefcc275aeb44b456d666d9a4a17949c92b1891089d7",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(document.getBytes(java.nio.charset.StandardCharsets.US_ASCII))));
        Assert.assertEquals("31303e3e251dd0e8963bf2a4d8897d1eccde63a485aaf287f88f29d40757ba96",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(selection.getBytes(java.nio.charset.StandardCharsets.US_ASCII))));
        Assert.assertTrue(selection.startsWith("tqs1.k1."));
        Assert.assertFalse(selection.contains("blob_B"));
        Assert.assertEquals(claim, alice.consumeBlobSelection(document, selection, target).claim());
        Assert.assertEquals(claim, alice.consumeBlobSelection(document, selection, target).claim());
        code(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> alice.consumeBlobSelection(document, selection,
                        new BlobSelectionTarget(target.scope(), target.targetEntity(),
                                target.canonicalField(), target.kind(), target.targetKey(),
                                "other-command")));
        code(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> context(provider, "bob").consumeBlobSelection(document, selection, target));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> alice.issueBlobSelection(document, claim, Duration.ofSeconds(301)));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> alice.issueBlobSelection(
                        alice.issueDocumentSnapshot(snapshot(), Duration.ofSeconds(300)),
                        claim, Duration.ofSeconds(120)));
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> alice.consumeBlobSelection(document, document, target));
        var expired = provider("test", "k1", false, (c, p, s, i) -> {},
                NOW.plusSeconds(121));
        code(RoundTripReferenceErrorCode.EXPIRED,
                () -> context(expired, "alice").consumeBlobSelection(
                        document, selection, target));
        var rotated = provider("test", "k2", true, (c, p, s, i) -> {},
                NOW.plusSeconds(30));
        Assert.assertEquals(claim, context(rotated, "alice")
                .consumeBlobSelection(document, selection, target).claim());
        allowed.set(false);
        code(RoundTripReferenceErrorCode.AUTHORIZATION_REQUIRED,
                () -> alice.consumeBlobSelection(document, selection, target));
    }

    @Test
    public void tqs1NewClientKeyAndMalformedWireAreExplicit() {
        var provider = provider("test", "k1", false, (c, p, s, i) -> {}, NOW);
        var alice = context(provider, "alice");
        String document = alice.issueBoundDocumentSnapshot(
                boundSnapshot(), Duration.ofSeconds(300));
        var target = new BlobSelectionTarget(boundSnapshot().snapshot().scope(),
                "OrderItem", "attachment", BlobSelectionTarget.Kind.NEW_CLIENT_KEY,
                "client-row-7", "command-new");
        var claim = new BlobSelectionClaim(target, BlobRef.of("blob_C"));
        String selection = alice.issueBlobSelection(document, claim, Duration.ofSeconds(120));
        Assert.assertEquals(claim, alice.consumeBlobSelection(document, selection, target).claim());
        code(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                () -> alice.consumeBlobSelection(document, selection,
                        new BlobSelectionTarget(target.scope(), target.targetEntity(),
                                target.canonicalField(), BlobSelectionTarget.Kind.EXISTING_ROW,
                                "item:0", target.commandId())));
        byte[] actor = new byte[32];
        byte[] wire = BlobSelectionWire.encode(claim, actor, 10, 20);
        Assert.assertEquals(claim, BlobSelectionWire.decode(wire).claim());
        byte[] trailing = Arrays.copyOf(wire, wire.length + 1);
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> BlobSelectionWire.decode(trailing));
        byte[] wrongVersion = wire.clone();
        wrongVersion[0] = 2;
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> BlobSelectionWire.decode(wrongVersion));
        byte[] truncated = Arrays.copyOf(wire, wire.length - 1);
        code(RoundTripReferenceErrorCode.INVALID_REFERENCE,
                () -> BlobSelectionWire.decode(truncated));
        var noCreate = new BoundDocumentSnapshot(new DocumentSnapshot(
                boundSnapshot().snapshot().modelFingerprint(),
                boundSnapshot().snapshot().businessId(),
                boundSnapshot().snapshot().scope(),
                boundSnapshot().snapshot().aggregate(),
                boundSnapshot().snapshot().entities(), List.of()),
                boundSnapshot().bindings());
        String noCreateDocument = alice.issueBoundDocumentSnapshot(
                noCreate, Duration.ofSeconds(300));
        code(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> alice.issueBlobSelection(
                        noCreateDocument, claim, Duration.ofSeconds(120)));
        var readOnly = new BoundDocumentSnapshot(boundSnapshot().snapshot(),
                List.of(new DocumentBlobBindingSnapshot("item:0", "attachment", "OrderItem",
                        BlobRef.of("blob_A"), 1L, true, false)));
        String readOnlyDocument = alice.issueBoundDocumentSnapshot(
                readOnly, Duration.ofSeconds(300));
        var existing = new BlobSelectionTarget(target.scope(), "OrderItem", "attachment",
                BlobSelectionTarget.Kind.EXISTING_ROW, "item:0", "command-new");
        code(RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                () -> alice.issueBlobSelection(readOnlyDocument,
                        new BlobSelectionClaim(existing, BlobRef.of("blob_C")),
                        Duration.ofSeconds(120)));
    }
}
