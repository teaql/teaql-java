package io.teaql.core.reference;

import io.teaql.core.UserContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Application-owned document load and save boundary; a protected snapshot is not a save grant. */
public interface ContextBoundDocumentService {
    record OpenRequest(String aggregateType, String businessId, String purpose, Duration lifetime) {
        public OpenRequest {
            text(aggregateType); text(businessId); text(purpose);
            if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) throw invalid();
        }
    }

    record ContextualDocument(String businessId, String documentToken, Map<String, Object> body) {
        public ContextualDocument {
            text(businessId); text(documentToken);
            body = freezeBody(Objects.requireNonNull(body, "body"));
        }
        @Override public String toString() { return "ContextualDocument[redacted]"; }
    }

    record SubmittedDocument(
            String businessId, String documentToken, String commandId, Map<String, Object> body) {
        public SubmittedDocument {
            text(businessId); text(documentToken); text(commandId);
            body = freezeBody(Objects.requireNonNull(body, "body"));
        }
        @Override public String toString() { return "SubmittedDocument[redacted]"; }
    }

    record AcceptRequest(String aggregateType, String purpose, SubmittedDocument submitted) {
        public AcceptRequest {
            text(aggregateType); text(purpose);
            Objects.requireNonNull(submitted, "submitted");
        }
        @Override public String toString() { return "AcceptRequest[redacted]"; }
    }

    record AcceptedDocument(
            ContextualDocument document, long previousRevision, long newRevision,
            List<InternalEntityIdentity> changedEntities) {
        public AcceptedDocument {
            Objects.requireNonNull(document, "document");
            changedEntities = List.copyOf(changedEntities);
        }
        @Override public String toString() { return "AcceptedDocument[redacted]"; }
    }

    ContextualDocument openDocument(
            UserContext context, TrustedReferencePrincipal principal, OpenRequest request);

    AcceptedDocument acceptDocument(
            UserContext context, TrustedReferencePrincipal principal, AcceptRequest request);

    /** Explicit opt-in to reusing an already issued document in governed presentation. */
    default boolean supportsViewPreparation() { return false; }

    /** Current authority for this issued document; must not save, reload its graph or issue a token. */
    default void validatePreparedDocument(
            UserContext context, TrustedReferencePrincipal principal, VerifiedDocumentSnapshot verified) {
        throw new RoundTripReferenceException(
                RoundTripReferenceErrorCode.PROVIDER_NOT_CONFIGURED,
                "Document service does not support governed presentation");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> freezeBody(Map<String, Object> body) {
        return (Map<String, Object>) freeze(body, 0, new AtomicInteger(100_000));
    }

    private static Object freeze(Object value, int depth, AtomicInteger remaining) {
        if (depth > 64 || remaining.decrementAndGet() < 0) throw invalid();
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof java.math.BigInteger
                || value instanceof java.math.BigDecimal) return value;
        if (value instanceof Double number && Double.isFinite(number)) return value;
        if (value instanceof Float number && Float.isFinite(number)) return value;
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) throw invalid();
                text(key);
                copy.put(key, freeze(entry.getValue(), depth + 1, remaining));
            }
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object entry : list) copy.add(freeze(entry, depth + 1, remaining));
            return Collections.unmodifiableList(copy);
        }
        throw invalid();
    }

    private static void text(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.length() > 16384) {
            throw invalid();
        }
    }

    private static RoundTripReferenceException invalid() {
        return new RoundTripReferenceException(
                RoundTripReferenceErrorCode.INVALID_REFERENCE, "INVALID_REFERENCE");
    }
}
