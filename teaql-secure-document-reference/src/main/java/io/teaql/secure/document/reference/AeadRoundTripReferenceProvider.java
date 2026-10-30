package io.teaql.secure.document.reference;

import io.teaql.core.UserContext;
import io.teaql.core.reference.ExternalEntityReference;
import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.BoundDocumentSnapshot;
import io.teaql.core.reference.BlobSelectionClaim;
import io.teaql.core.reference.BlobSelectionTarget;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.ReferenceMode;
import io.teaql.core.reference.ResolvedRoundTripReference;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import io.teaql.core.reference.RoundTripReferenceProvider;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.reference.VerifiedDocumentSnapshot;
import io.teaql.core.reference.VerifiedBoundDocumentSnapshot;
import io.teaql.core.reference.VerifiedBlobSelection;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Context-bound AES-256-GCM implementation of TeaQL's tqr1, tqd1, tqd2 and tqs1 formats. */
public final class AeadRoundTripReferenceProvider implements RoundTripReferenceProvider {
    private static final String PREFIX = "tqr1";
    private static final String DOCUMENT_PREFIX = "tqd1";
    private static final String BOUND_DOCUMENT_PREFIX = "tqd2";
    private static final String SELECTION_PREFIX = "tqs1";
    private static final byte[] HKDF_SALT = "teaql.round-trip-reference.v1".getBytes(StandardCharsets.US_ASCII);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_TEXT_BYTES = 4096;
    private static final int MAX_TOKEN_LENGTH = 24000;
    private static final int MAX_DOCUMENT_TOKEN_LENGTH = 256000;

    private final DeploymentProfile profile;
    private final ReferenceMode mode;
    private final String service;
    private final String environment;
    private final RoundTripReferenceKeyProvider keys;
    private final RoundTripReferenceAuthorizationPolicy authorization;
    private Clock clock;
    private Supplier<byte[]> nonceSource;

    private AeadRoundTripReferenceProvider(
            DeploymentProfile profile,
            ReferenceMode mode,
            String service,
            String environment,
            RoundTripReferenceKeyProvider keys,
            RoundTripReferenceAuthorizationPolicy authorization) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.service = requireText(service);
        this.environment = requireText(environment);
        this.keys = Objects.requireNonNull(keys, "keys");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        requireKey(keys.currentKey());
        this.clock = Clock.systemUTC();
        this.nonceSource = () -> {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            return nonce;
        };
    }

    public static AeadRoundTripReferenceProvider governed(
            DeploymentProfile profile,
            String service,
            String environment,
            RoundTripReferenceKeyProvider keys,
            RoundTripReferenceAuthorizationPolicy authorization) {
        return create(profile, service, environment, keys, authorization, null);
    }

    public static AeadRoundTripReferenceProvider fromProcessEnvironment(
            DeploymentProfile profile,
            String service,
            String environment,
            RoundTripReferenceKeyProvider keys,
            RoundTripReferenceAuthorizationPolicy authorization) {
        AeadRoundTripReferenceProvider provider = create(
                profile, service, environment, keys, authorization,
                System.getenv(RoundTripReferenceConfiguration.RAW_ID_ENV));
        provider.startupNotice().ifPresent(notice -> System.err.printf(
                "%s: %s; deployment_profile=%s; %s=%s%n",
                notice.level(), notice.message(), notice.deploymentProfile(),
                notice.telemetryKey(), notice.telemetryValue()));
        return provider;
    }

    static AeadRoundTripReferenceProvider create(
            DeploymentProfile profile,
            String service,
            String environment,
            RoundTripReferenceKeyProvider keys,
            RoundTripReferenceAuthorizationPolicy authorization,
            String acknowledgement) {
        boolean raw = RoundTripReferenceConfiguration.RAW_ID_ACKNOWLEDGEMENT.equals(acknowledgement);
        if (raw && profile == DeploymentProfile.PRODUCTION) {
            throw configuration("raw entity references cannot be enabled in production", null);
        }
        return new AeadRoundTripReferenceProvider(
                profile, raw ? ReferenceMode.RAW : ReferenceMode.GOVERNED,
                service, environment, keys, authorization);
    }

    AeadRoundTripReferenceProvider withClock(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        return this;
    }

    AeadRoundTripReferenceProvider withNonceSource(Supplier<byte[]> nonceSource) {
        this.nonceSource = Objects.requireNonNull(nonceSource, "nonceSource");
        return this;
    }

    public Optional<ReferenceStartupNotice> startupNotice() {
        if (mode != ReferenceMode.RAW) return Optional.empty();
        return Optional.of(new ReferenceStartupNotice(
                "ERROR",
                "TeaQL raw internal entity reference mode is active for local diagnostics",
                profile.label(),
                "teaql.reference.mode",
                "raw",
                Map.of("TeaQL-Reference-Mode", "raw-internal-id", "Cache-Control", "no-store")));
    }

    @Override
    public ReferenceMode mode() { return mode; }

    @Override
    public String issueDocumentSnapshot(
            UserContext context, TrustedReferencePrincipal principal,
            DocumentSnapshot snapshot, Duration lifetime) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(snapshot, "snapshot");
        requireCurrentPrincipal(context, principal);
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) throw invalid(null);
        authorizeDocument(context, principal, snapshot);
        try {
            RoundTripReferenceKey key = requireKey(keys.currentKey());
            Instant issuedAt = clock.instant();
            Instant expiresAt = issuedAt.plus(lifetime);
            byte[] plaintext = DocumentSnapshotWire.encode(snapshot,
                    actorFingerprint(key, principal),
                    issuedAt.getEpochSecond(), expiresAt.getEpochSecond());
            byte[] nonce = nonceSource.get();
            if (nonce == null || nonce.length != 12) {
                throw configuration("nonce must contain 12 bytes", null);
            }
            byte[] encrypted = documentCipher(Cipher.ENCRYPT_MODE, key, nonce,
                    snapshot.scope().purpose()).doFinal(plaintext);
            byte[] envelope = ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array();
            String token = DOCUMENT_PREFIX + "." + key.keyId() + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(envelope);
            if (token.length() > MAX_DOCUMENT_TOKEN_LENGTH) throw invalid(null);
            return token;
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    @Override
    public VerifiedDocumentSnapshot consumeDocumentSnapshot(
            UserContext context, TrustedReferencePrincipal principal, String token,
            String expectedAggregateType, String expectedPurpose) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(principal, "principal");
        requireCurrentPrincipal(context, principal);
        requireText(expectedAggregateType);
        requireText(expectedPurpose);
        try {
            if (token == null || token.length() > MAX_DOCUMENT_TOKEN_LENGTH) throw invalid(null);
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !DOCUMENT_PREFIX.equals(parts[0])
                    || parts[1].isEmpty() || parts[2].isEmpty()) throw invalid(null);
            if (!parts[2].matches("[A-Za-z0-9_-]+")) throw invalid(null);
            RoundTripReferenceKey key = keys.keyById(parts[1]);
            if (key == null) throw invalid(null);
            byte[] envelope = Base64.getUrlDecoder().decode(parts[2]);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(envelope)
                    .equals(parts[2])) throw invalid(null);
            if (envelope.length < 28) throw invalid(null);
            byte[] plaintext = documentCipher(Cipher.DECRYPT_MODE, key,
                    Arrays.copyOfRange(envelope, 0, 12), expectedPurpose)
                    .doFinal(envelope, 12, envelope.length - 12);
            var decoded = DocumentSnapshotWire.decode(plaintext);
            long now = clock.instant().getEpochSecond();
            if (decoded.expiresAt() <= now) {
                throw new RoundTripReferenceException(RoundTripReferenceErrorCode.EXPIRED,
                        "ROUND_TRIP_REFERENCE_EXPIRED");
            }
            if (decoded.issuedAt() > Math.addExact(now, 60)) throw invalid(null);
            DocumentSnapshot snapshot = decoded.snapshot();
            if (!MessageDigest.isEqual(decoded.actorFingerprint(), actorFingerprint(key, principal))
                    || !snapshot.scope().purpose().equals(expectedPurpose)
                    || !snapshot.scope().aggregateType().equals(expectedAggregateType)) {
                throw new RoundTripReferenceException(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                        "ROUND_TRIP_REFERENCE_SCOPE_MISMATCH");
            }
            authorizeDocument(context, principal, snapshot);
            return new VerifiedDocumentSnapshot(snapshot, key.keyId(),
                    Instant.ofEpochSecond(decoded.issuedAt()),
                    Instant.ofEpochSecond(decoded.expiresAt()));
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    @Override
    public String issueBoundDocumentSnapshot(
            UserContext context, TrustedReferencePrincipal principal,
            BoundDocumentSnapshot document, Duration lifetime) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(document, "document");
        requireCurrentPrincipal(context, principal);
        if (mode != ReferenceMode.GOVERNED || lifetime == null
                || lifetime.isZero() || lifetime.isNegative()) throw invalid(null);
        authorizeDocument(context, principal, document.snapshot());
        try {
            RoundTripReferenceKey key = requireKey(keys.currentKey());
            Instant issuedAt = clock.instant();
            Instant expiresAt = issuedAt.plus(lifetime);
            byte[] plaintext = DocumentSnapshotWire.encodeBound(document,
                    actorFingerprint(key, principal),
                    issuedAt.getEpochSecond(), expiresAt.getEpochSecond());
            byte[] nonce = nonceSource.get();
            if (nonce == null || nonce.length != 12) {
                throw configuration("nonce must contain 12 bytes", null);
            }
            byte[] encrypted = boundDocumentCipher(Cipher.ENCRYPT_MODE, key, nonce,
                    document.snapshot().scope().purpose()).doFinal(plaintext);
            byte[] envelope = ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array();
            String token = BOUND_DOCUMENT_PREFIX + "." + key.keyId() + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(envelope);
            if (token.length() > MAX_DOCUMENT_TOKEN_LENGTH) throw invalid(null);
            return token;
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    @Override
    public VerifiedBoundDocumentSnapshot consumeBoundDocumentSnapshot(
            UserContext context, TrustedReferencePrincipal principal, String token,
            String expectedAggregateType, String expectedPurpose) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(principal, "principal");
        requireCurrentPrincipal(context, principal);
        requireText(expectedAggregateType);
        requireText(expectedPurpose);
        try {
            if (mode != ReferenceMode.GOVERNED || token == null
                    || token.length() > MAX_DOCUMENT_TOKEN_LENGTH) throw invalid(null);
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !BOUND_DOCUMENT_PREFIX.equals(parts[0])
                    || parts[1].isEmpty() || parts[2].isEmpty()
                    || !parts[2].matches("[A-Za-z0-9_-]+")) throw invalid(null);
            RoundTripReferenceKey key = keys.keyById(parts[1]);
            if (key == null) throw invalid(null);
            byte[] envelope = Base64.getUrlDecoder().decode(parts[2]);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(envelope)
                    .equals(parts[2]) || envelope.length < 28) throw invalid(null);
            byte[] plaintext = boundDocumentCipher(Cipher.DECRYPT_MODE, key,
                    Arrays.copyOfRange(envelope, 0, 12), expectedPurpose)
                    .doFinal(envelope, 12, envelope.length - 12);
            var decoded = DocumentSnapshotWire.decodeBound(plaintext);
            long now = clock.instant().getEpochSecond();
            if (decoded.expiresAt() <= now) {
                throw new RoundTripReferenceException(RoundTripReferenceErrorCode.EXPIRED,
                        "ROUND_TRIP_REFERENCE_EXPIRED");
            }
            if (decoded.issuedAt() > Math.addExact(now, 60)) throw invalid(null);
            DocumentSnapshot snapshot = decoded.document().snapshot();
            if (!MessageDigest.isEqual(decoded.actorFingerprint(), actorFingerprint(key, principal))
                    || !snapshot.scope().purpose().equals(expectedPurpose)
                    || !snapshot.scope().aggregateType().equals(expectedAggregateType)) {
                throw new RoundTripReferenceException(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                        "ROUND_TRIP_REFERENCE_SCOPE_MISMATCH");
            }
            authorizeDocument(context, principal, snapshot);
            return new VerifiedBoundDocumentSnapshot(decoded.document(), key.keyId(),
                    Instant.ofEpochSecond(decoded.issuedAt()),
                    Instant.ofEpochSecond(decoded.expiresAt()));
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    @Override
    public String issueBlobSelection(
            UserContext context, TrustedReferencePrincipal principal,
            String documentToken, BlobSelectionClaim claim, Duration lifetime) {
        Objects.requireNonNull(claim, "claim");
        if (lifetime == null || lifetime.isNegative() || lifetime.isZero()) throw invalid(null);
        var target = claim.target();
        var document = consumeBoundDocumentSnapshot(context, principal, documentToken,
                target.scope().aggregateType(), target.scope().purpose());
        requireSelectionTarget(document.document(), target);
        try {
            RoundTripReferenceKey key = requireKey(keys.currentKey());
            Instant issuedAt = clock.instant();
            Instant expiresAt = issuedAt.plus(lifetime);
            if (expiresAt.isAfter(document.expiresAt())) throw invalid(null);
            byte[] plaintext = BlobSelectionWire.encode(claim, actorFingerprint(key, principal),
                    issuedAt.getEpochSecond(), expiresAt.getEpochSecond());
            byte[] nonce = nonceSource.get();
            if (nonce == null || nonce.length != 12) {
                throw configuration("nonce must contain 12 bytes", null);
            }
            byte[] encrypted = selectionCipher(Cipher.ENCRYPT_MODE, key, nonce,
                    target.scope().purpose()).doFinal(plaintext);
            byte[] envelope = ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array();
            String token = SELECTION_PREFIX + "." + key.keyId() + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(envelope);
            if (token.length() > MAX_DOCUMENT_TOKEN_LENGTH) throw invalid(null);
            return token;
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    @Override
    public VerifiedBlobSelection consumeBlobSelection(
            UserContext context, TrustedReferencePrincipal principal,
            String documentToken, String token, BlobSelectionTarget expectedTarget) {
        Objects.requireNonNull(expectedTarget, "expectedTarget");
        var document = consumeBoundDocumentSnapshot(context, principal, documentToken,
                expectedTarget.scope().aggregateType(), expectedTarget.scope().purpose());
        requireSelectionTarget(document.document(), expectedTarget);
        try {
            if (token == null || token.length() > MAX_DOCUMENT_TOKEN_LENGTH) throw invalid(null);
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !SELECTION_PREFIX.equals(parts[0])
                    || parts[1].isEmpty() || parts[2].isEmpty()
                    || !parts[2].matches("[A-Za-z0-9_-]+")) throw invalid(null);
            RoundTripReferenceKey key = keys.keyById(parts[1]);
            if (key == null) throw invalid(null);
            byte[] envelope = Base64.getUrlDecoder().decode(parts[2]);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(envelope)
                    .equals(parts[2]) || envelope.length < 28) throw invalid(null);
            byte[] plaintext = selectionCipher(Cipher.DECRYPT_MODE, key,
                    Arrays.copyOfRange(envelope, 0, 12), expectedTarget.scope().purpose())
                    .doFinal(envelope, 12, envelope.length - 12);
            var decoded = BlobSelectionWire.decode(plaintext);
            long now = clock.instant().getEpochSecond();
            if (decoded.expiresAt() <= now) {
                throw new RoundTripReferenceException(RoundTripReferenceErrorCode.EXPIRED,
                        "ROUND_TRIP_REFERENCE_EXPIRED");
            }
            if (decoded.issuedAt() > Math.addExact(now, 60)
                    || decoded.expiresAt() > document.expiresAt().getEpochSecond()) throw invalid(null);
            if (!MessageDigest.isEqual(decoded.actorFingerprint(), actorFingerprint(key, principal))
                    || !decoded.claim().target().equals(expectedTarget)) {
                throw new RoundTripReferenceException(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                        "ROUND_TRIP_REFERENCE_SCOPE_MISMATCH");
            }
            return new VerifiedBlobSelection(decoded.claim(), key.keyId(),
                    Instant.ofEpochSecond(decoded.issuedAt()),
                    Instant.ofEpochSecond(decoded.expiresAt()));
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    private static void requireSelectionTarget(
            BoundDocumentSnapshot document, BlobSelectionTarget target) {
        if (!document.snapshot().scope().equals(target.scope())) {
            throw new RoundTripReferenceException(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                    "ROUND_TRIP_REFERENCE_SCOPE_MISMATCH");
        }
        if (target.kind() == BlobSelectionTarget.Kind.EXISTING_ROW) {
            boolean entity = document.snapshot().entities().stream().anyMatch(value ->
                    value.rowKey().equals(target.targetKey())
                            && value.identity().entityType().equals(target.targetEntity())
                            && value.writableFields().contains(target.canonicalField()));
            boolean binding = document.bindings().stream().anyMatch(value ->
                    value.rowKey().equals(target.targetKey())
                            && value.canonicalField().equals(target.canonicalField())
                            && value.writable());
            if (entity && binding) return;
        } else if (document.snapshot().relations().stream().anyMatch(value ->
                value.completeness() == DocumentSnapshot.Completeness.COMPLETE
                        && value.writable()
                        && value.allowedMutations().contains(
                                DocumentSnapshot.MutationKind.CREATE_CHILD))) {
            // The application must still bind the clientKey to its approved new row and field.
            return;
        }
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.DOCUMENT_PROJECTION_VIOLATION,
                "DOCUMENT_PROJECTION_VIOLATION");
    }

    private void authorizeDocument(UserContext context, TrustedReferencePrincipal principal,
                                   DocumentSnapshot snapshot) {
        authorization.authorize(context, principal, snapshot.scope(), snapshot.aggregate());
        for (var entity : snapshot.entities()) {
            authorization.authorize(context, principal, snapshot.scope(), entity.identity());
        }
    }

    private static void requireCurrentPrincipal(
            UserContext context, TrustedReferencePrincipal principal) {
        if (!context.trustedReferencePrincipal().equals(principal)) {
            throw new RoundTripReferenceException(RoundTripReferenceErrorCode.CONTEXT_MISMATCH,
                    "ROUND_TRIP_REFERENCE_CONTEXT_MISMATCH");
        }
    }

    private Cipher documentCipher(int mode, RoundTripReferenceKey key, byte[] nonce, String purpose)
            throws GeneralSecurityException {
        return documentCipher(mode, key, nonce, purpose,
                DOCUMENT_PREFIX, "document-snapshot-encryption");
    }

    private Cipher boundDocumentCipher(int mode, RoundTripReferenceKey key, byte[] nonce,
                                       String purpose) throws GeneralSecurityException {
        return documentCipher(mode, key, nonce, purpose,
                BOUND_DOCUMENT_PREFIX, "document-snapshot-v2-encryption");
    }

    private Cipher selectionCipher(int mode, RoundTripReferenceKey key, byte[] nonce,
                                   String purpose) throws GeneralSecurityException {
        return documentCipher(mode, key, nonce, purpose,
                SELECTION_PREFIX, "blob-selection-v1-encryption");
    }

    private Cipher documentCipher(int mode, RoundTripReferenceKey key, byte[] nonce,
                                  String purpose, String prefix, String label)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(deriveKey(key, label), "AES"),
                new GCMParameterSpec(128, nonce));
        cipher.updateAAD(DocumentSnapshotWire.aad(
                prefix, key.keyId(), service, environment, purpose));
        return cipher;
    }

    @Override
    public ExternalEntityReference issue(
            UserContext context,
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            InternalEntityIdentity identity,
            Duration lifetime) {
        validate(principal, scope, identity);
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) throw invalid(null);
        authorization.authorize(context, principal, scope, identity);
        if (mode == ReferenceMode.RAW) {
            return new ExternalEntityReference.Raw(identity.id(), identity.version());
        }
        try {
            RoundTripReferenceKey key = requireKey(keys.currentKey());
            Instant issuedAt = clock.instant();
            Instant expiresAt = issuedAt.plus(lifetime);
            byte[] fingerprint = actorFingerprint(key, principal);
            byte[] plaintext = encodePayload(identity, scope, fingerprint, issuedAt, expiresAt);
            byte[] nonce = nonceSource.get();
            if (nonce == null || nonce.length != 12) throw configuration("nonce must contain 12 bytes", null);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, key, nonce, scope.purpose());
            byte[] encrypted = cipher.doFinal(plaintext);
            byte[] envelope = ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array();
            return new ExternalEntityReference.Governed(
                    PREFIX + "." + key.keyId() + "."
                            + Base64.getUrlEncoder().withoutPadding().encodeToString(envelope));
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    @Override
    public ResolvedRoundTripReference resolve(
            UserContext context,
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            ExternalEntityReference reference,
            String expectedEntityType) {
        requireText(expectedEntityType);
        ResolvedRoundTripReference resolved;
        if (mode == ReferenceMode.RAW && reference instanceof ExternalEntityReference.Raw raw) {
            resolved = new ResolvedRoundTripReference(
                    new InternalEntityIdentity(expectedEntityType, raw.id(), raw.version()),
                    ReferenceMode.RAW, null, null, null);
        } else if (mode == ReferenceMode.GOVERNED
                && reference instanceof ExternalEntityReference.Governed governed) {
            resolved = resolveGoverned(principal, scope, governed.token(), expectedEntityType);
        } else {
            throw invalid(null);
        }
        authorization.authorize(context, principal, scope, resolved.identity());
        return resolved;
    }

    private ResolvedRoundTripReference resolveGoverned(
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            String token,
            String expectedEntityType) {
        try {
            String[] parts = token.split("\\.", -1);
            if (token.length() > MAX_TOKEN_LENGTH || parts.length != 3 || !PREFIX.equals(parts[0])) {
                throw invalid(null);
            }
            RoundTripReferenceKey key = keys.keyById(parts[1]);
            if (key == null) throw invalid(null);
            byte[] envelope = Base64.getUrlDecoder().decode(parts[2]);
            if (envelope.length < 28) throw invalid(null);
            byte[] nonce = Arrays.copyOfRange(envelope, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(envelope, 12, envelope.length);
            DecodedPayload payload = decodePayload(
                    cipher(Cipher.DECRYPT_MODE, key, nonce, scope.purpose()).doFinal(encrypted));
            Instant now = clock.instant();
            if (!payload.expiresAt().isAfter(now)) {
                throw new RoundTripReferenceException(RoundTripReferenceErrorCode.EXPIRED,
                        "ROUND_TRIP_REFERENCE_EXPIRED");
            }
            if (payload.issuedAt().isAfter(now.plusSeconds(60))) throw invalid(null);
            byte[] expectedActor = actorFingerprint(key, principal);
            if (!MessageDigest.isEqual(payload.actorFingerprint(), expectedActor)
                    || !payload.documentId().equals(scope.documentId())
                    || !payload.aggregateType().equals(scope.aggregateType())
                    || payload.aggregateId() != scope.aggregateId()
                    || payload.aggregateRevision() != scope.aggregateRevision()
                    || !payload.identity().entityType().equals(expectedEntityType)) {
                throw new RoundTripReferenceException(RoundTripReferenceErrorCode.SCOPE_MISMATCH,
                        "ROUND_TRIP_REFERENCE_SCOPE_MISMATCH");
            }
            return new ResolvedRoundTripReference(
                    payload.identity(), ReferenceMode.GOVERNED, key.keyId(),
                    payload.issuedAt(), payload.expiresAt());
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    private Cipher cipher(int mode, RoundTripReferenceKey key, byte[] nonce, String purpose)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(deriveKey(key, "entity-reference-encryption"), "AES"),
                new GCMParameterSpec(128, nonce));
        cipher.updateAAD(encodeTexts(PREFIX, key.keyId(), service, environment, purpose));
        return cipher;
    }

    private byte[] actorFingerprint(
            RoundTripReferenceKey key, TrustedReferencePrincipal principal)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(deriveKey(key, "actor-fingerprint"), "HmacSHA256"));
        ByteArrayOutputStream canonical = new ByteArrayOutputStream();
        writeText(canonical, principal.authenticationRealm());
        writeText(canonical, principal.subject());
        writeText(canonical, principal.domainRootType());
        canonical.writeBytes(ByteBuffer.allocate(8).putLong(principal.domainRootId()).array());
        return mac.doFinal(canonical.toByteArray());
    }

    private byte[] deriveKey(RoundTripReferenceKey key, String label)
            throws GeneralSecurityException {
        byte[] info = encodeTexts(label, service, environment, key.keyId());
        Mac extract = Mac.getInstance("HmacSHA256");
        extract.init(new SecretKeySpec(HKDF_SALT, "HmacSHA256"));
        byte[] pseudoRandomKey = extract.doFinal(key.keyBytes());
        Mac expand = Mac.getInstance("HmacSHA256");
        expand.init(new SecretKeySpec(pseudoRandomKey, "HmacSHA256"));
        expand.update(info);
        expand.update((byte) 1);
        return expand.doFinal();
    }

    private static byte[] encodePayload(
            InternalEntityIdentity identity,
            ReferenceDocumentScope scope,
            byte[] fingerprint,
            Instant issuedAt,
            Instant expiresAt) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(fingerprint);
        writeText(output, identity.entityType());
        output.writeBytes(ByteBuffer.allocate(16).putLong(identity.id()).putLong(identity.version()).array());
        writeText(output, scope.documentId());
        writeText(output, scope.aggregateType());
        output.writeBytes(ByteBuffer.allocate(32)
                .putLong(scope.aggregateId()).putLong(scope.aggregateRevision())
                .putLong(issuedAt.getEpochSecond()).putLong(expiresAt.getEpochSecond()).array());
        return output.toByteArray();
    }

    private static DecodedPayload decodePayload(byte[] bytes) {
        try {
            ByteBuffer input = ByteBuffer.wrap(bytes);
            byte[] fingerprint = new byte[32];
            input.get(fingerprint);
            String entityType = readText(input);
            long id = input.getLong();
            long version = input.getLong();
            String documentId = readText(input);
            String aggregateType = readText(input);
            long aggregateId = input.getLong();
            long revision = input.getLong();
            Instant issuedAt = Instant.ofEpochSecond(input.getLong());
            Instant expiresAt = Instant.ofEpochSecond(input.getLong());
            if (input.hasRemaining()) throw invalid(null);
            return new DecodedPayload(
                    fingerprint, new InternalEntityIdentity(entityType, id, version), documentId,
                    aggregateType, aggregateId, revision, issuedAt, expiresAt);
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (Exception error) {
            throw invalid(error);
        }
    }

    private static byte[] encodeTexts(String... values) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (String value : values) writeText(output, value);
        return output.toByteArray();
    }

    private static void writeText(ByteArrayOutputStream output, String value) {
        byte[] bytes = requireText(value).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0xffff) throw invalid(null);
        output.write((bytes.length >>> 8) & 0xff);
        output.write(bytes.length & 0xff);
        output.writeBytes(bytes);
    }

    private static String readText(ByteBuffer input) {
        if (input.remaining() < 2) throw invalid(null);
        int length = Short.toUnsignedInt(input.getShort());
        if (input.remaining() < length) throw invalid(null);
        byte[] bytes = new byte[length];
        input.get(bytes);
        return requireText(new String(bytes, StandardCharsets.UTF_8));
    }

    private static String requireText(String value) {
        if (value == null || value.isEmpty() || !value.equals(value.trim())
                || value.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
            throw invalid(null);
        }
        return value;
    }

    private static RoundTripReferenceKey requireKey(RoundTripReferenceKey key) {
        if (key == null) throw configuration("current reference key is missing", null);
        return key;
    }

    private static void validate(
            TrustedReferencePrincipal principal,
            ReferenceDocumentScope scope,
            InternalEntityIdentity identity) {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(identity, "identity");
    }

    private static RoundTripReferenceException invalid(Throwable cause) {
        return cause == null
                ? new RoundTripReferenceException(
                        RoundTripReferenceErrorCode.INVALID_REFERENCE, "ROUND_TRIP_REFERENCE_INVALID")
                : new RoundTripReferenceException(
                        RoundTripReferenceErrorCode.INVALID_REFERENCE,
                        "ROUND_TRIP_REFERENCE_INVALID", cause);
    }

    private static RoundTripReferenceException configuration(String message, Throwable cause) {
        return cause == null
                ? new RoundTripReferenceException(RoundTripReferenceErrorCode.CONFIGURATION_INVALID, message)
                : new RoundTripReferenceException(
                        RoundTripReferenceErrorCode.CONFIGURATION_INVALID, message, cause);
    }

    private record DecodedPayload(
            byte[] actorFingerprint,
            InternalEntityIdentity identity,
            String documentId,
            String aggregateType,
            long aggregateId,
            long aggregateRevision,
            Instant issuedAt,
            Instant expiresAt) {}
}
