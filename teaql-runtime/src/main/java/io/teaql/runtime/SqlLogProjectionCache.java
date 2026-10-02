package io.teaql.runtime;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.utils.SqlLogRenderer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.WeakHashMap;

/** Private debug downgrade state: a fingerprint and already-safe values, never raw provenance. */
final class SqlLogProjectionCache {
    private record Entry(String fingerprint, ExecutionMetadata safe) {}
    private static final Map<ExecutionMetadata, Entry> ENTRIES = Collections.synchronizedMap(new WeakHashMap<>());

    static void remember(ExecutionMetadata debug, ExecutionMetadata safe) {
        String fingerprint = fingerprint(debug);
        if (fingerprint != null) ENTRIES.put(debug, new Entry(fingerprint, copy(safe)));
    }

    static ExecutionMetadata safeCopy(ExecutionMetadata debug) {
        Entry entry = ENTRIES.get(debug);
        return entry != null && entry.fingerprint().equals(fingerprint(debug)) ? copy(entry.safe()) : null;
    }

    private static String fingerprint(ExecutionMetadata m) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, m.getBackend()); add(digest, m.getOperation());
            add(digest, m.getStartedAt()); add(digest, m.getEndedAt()); add(digest, m.getElapsedUs());
            add(digest, m.getAffectedRows()); add(digest, m.getResultCount()); add(digest, m.getResultSummary());
            add(digest, m.getBackendRequestId()); add(digest, m.getParameterizedQuery()); add(digest, m.getDebugQuery());
            add(digest, m.getComment()); add(digest, m.getPurpose()); add(digest, m.getAuditReason());
            add(digest, m.getStatementOperation());
            add(digest, m.getMutationLineage().size());
            for (var node : m.getMutationLineage()) {
                add(digest, node.getKind()); add(digest, node.getName()); add(digest, node.getEntityId()); add(digest, node.getComment());
            }
            add(digest, m.getParameterLogPolicies()); add(digest, m.getParameterMasked());
            add(digest, m.isGeneratedSql()); add(digest, m.getLogMode());
            add(digest, m.getSqlOmissionReason()); add(digest, m.getExecutionOutcome());
            add(digest, m.getBatchOutcome());
            add(digest, m.getParameterCount());
            for (Object value : m.getParameters()) add(digest, SqlLogRenderer.literal(value, m.getBackend()));
            add(digest, m.getTraceChain() == null ? -1 : m.getTraceChain().size());
            if (m.getTraceChain() != null) for (var node : m.getTraceChain()) {
                add(digest, node.getKind()); add(digest, node.getName()); add(digest, node.getEntityId()); add(digest, node.getComment());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IllegalArgumentException unsupported) {
            // Unsupported values cannot establish identity; downgrade conservatively.
            return null;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void add(MessageDigest digest, Object value) {
        if (value == null) { digest.update((byte) 0); return; }
        digest.update((byte) 1);
        byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
        int size = bytes.length;
        digest.update((byte) (size >>> 24)); digest.update((byte) (size >>> 16));
        digest.update((byte) (size >>> 8)); digest.update((byte) size);
        digest.update(bytes);
    }

    private static ExecutionMetadata copy(ExecutionMetadata m) {
        var c = new ExecutionMetadata();
        c.setBackend(m.getBackend()); c.setOperation(m.getOperation());
        c.setStartedAt(m.getStartedAt()); c.setEndedAt(m.getEndedAt()); c.setElapsedUs(m.getElapsedUs());
        c.setAffectedRows(m.getAffectedRows()); c.setResultCount(m.getResultCount());
        c.setResultSummary(m.getResultSummary()); c.setBackendRequestId(m.getBackendRequestId());
        c.setParameterizedQuery(m.getParameterizedQuery()); c.setDebugQuery(m.getDebugQuery());
        c.setParameters(m.getParameters().stream().map(LogPrivacy::copyValue).toList());
        c.setParameterLogPolicies(m.getParameterLogPolicies()); c.setParameterMasked(m.getParameterMasked());
        c.setGeneratedSql(m.isGeneratedSql()); c.setLogMode(m.getLogMode());
        c.setSqlOmissionReason(m.getSqlOmissionReason()); c.setExecutionOutcome(m.getExecutionOutcome());
        c.setBatchOutcome(m.getBatchOutcome());
        c.setComment(m.getComment()); c.setPurpose(m.getPurpose()); c.setAuditReason(m.getAuditReason());
        c.setTraceChain(m.getTraceChain() == null ? null : java.util.List.copyOf(m.getTraceChain()));
        c.setMutationLineage(m.getMutationLineage()); c.setStatementOperation(m.getStatementOperation());
        return c;
    }
}
