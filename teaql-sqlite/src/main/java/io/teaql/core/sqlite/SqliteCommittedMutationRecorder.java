package io.teaql.core.sqlite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.GeneratedSchemaBootstrap;
import io.teaql.core.CommittedEventSchemaContributor;
import io.teaql.core.MutationIntent;
import io.teaql.core.SchemaExecutor;
import io.teaql.core.TraceNode;
import io.teaql.core.UserContext;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.runtime.CommittedMutationFact;
import io.teaql.runtime.CommittedMutationRecorder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** SQLite source adapter that records one graph-save result inside its write transaction. */
public final class SqliteCommittedMutationRecorder
        implements CommittedMutationRecorder, CommittedEventSchemaContributor {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final SqlExecutionAdapter adapter;
    private final SqliteCommittedEventStore events;
    private final String sourceRuntimeId;
    private final String modelHash;

    public SqliteCommittedMutationRecorder(SqlExecutionAdapter adapter,
            String sourceRuntimeId, String modelHash) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.events = new SqliteCommittedEventStore(adapter);
        if (sourceRuntimeId == null || sourceRuntimeId.isBlank()
                || modelHash == null || !modelHash.matches("sha256:[0-9a-f]{64}")) {
            throw new IllegalArgumentException("A source runtime ID and accepted SHA-256 model hash are required");
        }
        this.sourceRuntimeId = sourceRuntimeId;
        this.modelHash = modelHash;
    }

    @Override
    public void ensureSchema(UserContext context, SchemaExecutor.Invocation invocation) {
        SchemaExecutor.Invocation.requireContextOwned(invocation);
        Objects.requireNonNull(context, "context");
        events.ensureSchema();
    }
    public SqliteCommittedEventStore eventStore() { return events; }

    @Override
    public void record(UserContext context, Object transactionResource,
            MutationIntent intent, List<CommittedMutationFact> facts) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(intent, "intent");
        if (transactionResource != adapter || !adapter.hasActiveTransaction()) {
            throw new IllegalStateException("Committed graph capture requires the source provider transaction");
        }
        if (facts == null || facts.isEmpty()) {
            throw new IllegalArgumentException("Committed graph capture requires at least one fact");
        }
        String changeSetId = UUID.randomUUID().toString();
        String committedAt = Instant.now().toString();
        String actor = context.getAttribute(GeneratedSchemaBootstrap.AUDIT_ACTOR_ATTRIBUTE, String.class);
        events.append(sourceRuntimeId, changeSetId, modelHash, sequence -> {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("protocol_version", 1);
            envelope.put("change_set_id", changeSetId);
            envelope.put("source_runtime_id", sourceRuntimeId);
            envelope.put("source_sequence", Long.toString(sequence));
            envelope.put("source_model_hash", modelHash);
            envelope.put("source_transaction_id", changeSetId);
            envelope.put("committed_at", committedAt);
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("actor_ref", actor);
            audit.put("comment", intent.comment());
            // MutationIntent currently has no separate business purpose field.
            audit.put("purpose", "replicate committed graph");
            envelope.put("audit", audit);
            List<Map<String, Object>> mutations = new ArrayList<>(facts.size());
            for (CommittedMutationFact fact : facts) {
                Map<String, Object> mutation = new LinkedHashMap<>();
                mutation.put("entity_type", fact.entityType());
                mutation.put("entity_id", fact.entityId());
                mutation.put("operation", fact.operation());
                mutation.put("base_version", fact.baseVersion());
                mutation.put("committed_version", fact.committedVersion());
                mutation.put("fields", fact.fields());
                List<Map<String, Object>> chain = new ArrayList<>();
                for (TraceNode node : fact.traceChain()) {
                    Map<String, Object> step = new LinkedHashMap<>();
                    step.put("kind", node.getKind().name());
                    step.put("name", node.getName());
                    step.put("entity_id", node.getEntityId() == null ? null : node.getEntityId().toString());
                    step.put("comment", node.getComment());
                    chain.add(step);
                }
                mutation.put("trace_chain", chain);
                mutations.add(mutation);
            }
            envelope.put("mutations", mutations);
            try {
                return JSON.writeValueAsString(envelope);
            } catch (JsonProcessingException failure) {
                // Do not attach values or serializer causes to public errors.
                throw new IllegalStateException("Committed mutation payload encoding failed");
            }
        });
    }
}
