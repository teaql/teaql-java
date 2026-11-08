package io.teaql.core.sqlite;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static io.teaql.core.sqlite.SqliteModelCheckedCommittedTarget.*;

/** Trusted startup infrastructure. No ordinary business save or remote request routing. */
public final class SqliteCommittedSnapshot {
    private static final Set<String> SNAPSHOT_FIELDS = Set.of("snapshot_version", "snapshot_id",
            "source_runtime_id", "source_sequence", "source_model_hash", "audit", "entity_types", "rows");
    private static final Set<String> AUDIT_FIELDS = Set.of("actor_ref", "comment", "purpose", "trace_id");
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public record Limits(int maxRows, int maxPayloadBytes) {
        public Limits { if (maxRows < 1 || maxRows == Integer.MAX_VALUE || maxPayloadBytes < 1)
            throw new IllegalArgumentException("positive bounded snapshot limits are required"); }
        public static Limits defaults() { return new Limits(100_000, 16 * 1024 * 1024); }
    }
    public enum ImportMode { REQUIRE_EMPTY, REPLACE_UNINITIALIZED }
    public record SnapshotRecord(String sourceRuntimeId, long sourceSequence, String snapshotId,
            String modelHash, String payloadSha256, String payloadJson) {
        @Override public String toString() { return "SnapshotRecord[sourceSequence=" + sourceSequence + ", payload=<redacted>]"; }
    }
    private record Table(EntityDescriptor entity, List<PropertyDescriptor> fields,
            String table, String id, String version) { }
    private final SqlExecutionAdapter sql;
    private final String source;
    private final String model;

    public SqliteCommittedSnapshot(SqlExecutionAdapter sql, String source, String model) {
        this.sql = Objects.requireNonNull(sql, "sql");
        if (blank(source) || model == null || !model.matches("sha256:[0-9a-f]{64}")) throw invalid("source/model is invalid");
        this.source = source; this.model = model;
    }

    /** N and physical rows share one read transaction. Never export a caller's uncommitted writes. */
    public SnapshotRecord exportSnapshot(UserContext context, String snapshotId,
            String comment, String purpose, Limits limits) {
        Objects.requireNonNull(context, "context"); Objects.requireNonNull(limits, "limits");
        if (blank(snapshotId) || blank(comment) || blank(purpose)) throw invalid("snapshot identity and intent are required");
        standalone();
        EntityMetaFactory metadata = EntityMetaFactory.requireFrom(context);
        AtomicReference<SnapshotRecord> result = new AtomicReference<>();
        sql.executeInTransaction(() -> {
            requireTransaction();
            // The first SELECT establishes SQLite's read snapshot, including when N=0.
            long n = position();
            List<Table> tables = tables(metadata);
            ObjectNode body = JSON.createObjectNode();
            body.put("snapshot_version", 1); body.put("snapshot_id", snapshotId);
            body.put("source_runtime_id", source); body.put("source_sequence", Long.toString(n));
            body.put("source_model_hash", model);
            body.putObject("audit").put("comment", comment).put("purpose", purpose);
            var catalog = body.putArray("entity_types"); var rows = body.putArray("rows");
            long usedBytes = 0;
            for (Table table : tables) {
                catalog.add(table.entity().getType());
                List<String> columns = new ArrayList<>(List.of(quoted(table.id()) + " AS snapshot_id", quoted(table.version()) + " AS snapshot_version"));
                for (int i = 0; i < table.fields().size(); i++) columns.add(quoted(mapping(table.fields().get(i)).column()) + " AS snapshot_field_" + i);
                int remaining = limits.maxRows() - rows.size();
                for (Map<String, Object> values : sql.queryForList("SELECT " + String.join(",", columns)
                        + " FROM " + quoted(table.table()) + " ORDER BY " + quoted(table.id()) + " LIMIT ?",
                        new Object[] {remaining + 1})) {
                    if (rows.size() == limits.maxRows()) throw invalid("snapshot row budget exceeded");
                    ObjectNode row = JSON.createObjectNode().put("entity_type", table.entity().getType())
                            .put("entity_id", integer(values.get("snapshot_id")).toString())
                            .put("version", integer(values.get("snapshot_version")).toString());
                    ObjectNode fields = row.putObject("fields");
                    for (int i = 0; i < table.fields().size(); i++) fields.set(table.fields().get(i).getName(), encode(table.fields().get(i), values.get("snapshot_field_" + i)));
                    // Same structural decoder/representability checks as the target, not Checker/Fix.
                    decodeSnapshotRow(metadata, row);
                    usedBytes += bytes(row.toString()); if (usedBytes > limits.maxPayloadBytes()) throw invalid("snapshot byte budget exceeded");
                    rows.add(row);
                }
            }
            String text = body.toString(); bounded(text, limits);
            result.set(new SnapshotRecord(source, n, snapshotId, model, digest(text), text));
        });
        return result.get();
    }

    /** Explicit destructive replacement is allowed only before this target has any stream state. */
    public SqliteCommittedEventStore.InboxOutcome initializeSnapshot(UserContext context, SnapshotRecord record, Limits limits) {
        return initializeSnapshot(context, record, ImportMode.REQUIRE_EMPTY, limits);
    }

    /** Explicit destructive replacement is allowed only before this target has any stream state. */
    public SqliteCommittedEventStore.InboxOutcome initializeSnapshot(UserContext context,
            SnapshotRecord record, ImportMode mode, Limits limits) {
        Objects.requireNonNull(context, "context"); Objects.requireNonNull(mode, "mode"); Objects.requireNonNull(limits, "limits");
        sql.restrictOrdinaryMutations();
        standalone(); JsonNode body = validate(record, limits);
        EntityMetaFactory metadata = EntityMetaFactory.requireFrom(context);
        AtomicReference<SqliteCommittedEventStore.InboxOutcome> result = new AtomicReference<>();
        sql.executeInTransaction(() -> {
            requireTransaction();
            List<Map<String, Object>> prior = sql.queryForList("SELECT snapshot_id,source_sequence,model_hash,payload_sha256,payload_json "
                    + "FROM teaql_event_snapshot WHERE source_runtime_id=?", new Object[] {source});
            if (!prior.isEmpty()) {
                Map<String, Object> old = prior.get(0);
                if (!record.snapshotId().equals(old.get("snapshot_id")) || record.sourceSequence() != integer(old.get("source_sequence"))
                        || !model.equals(old.get("model_hash")) || !record.payloadSha256().equals(old.get("payload_sha256"))
                        || !record.payloadSha256().equals(digest((String) old.get("payload_json")))) throw conflict("snapshot identity/content conflicts");
                var checkpoint = sql.queryForList("SELECT source_sequence,model_hash FROM teaql_event_checkpoint WHERE source_runtime_id=?", new Object[] {source});
                if (checkpoint.size() != 1 || integer(checkpoint.get(0).get("source_sequence")) < record.sourceSequence()
                        || !model.equals(checkpoint.get(0).get("model_hash"))) throw invalid("snapshot checkpoint is inconsistent");
                result.set(SqliteCommittedEventStore.InboxOutcome.duplicate(record.sourceSequence())); return;
            }
            for (String table : List.of("teaql_event_snapshot", "teaql_event_checkpoint", "teaql_event_inbox", "teaql_event_applied_payload", "teaql_event_outbox", "teaql_event_sequence", "teaql_event_snapshot_publication", "teaql_event_delivery_journal"))
                if (count(table) != 0) throw conflict("target already contains stream state");
            List<Table> tables = tables(metadata);
            Set<String> catalog = new TreeSet<>();
            if (!body.path("entity_types").isArray()) throw invalid("snapshot catalog is missing");
            for (JsonNode type : body.path("entity_types")) if (!type.isTextual() || !catalog.add(type.textValue())) throw invalid("snapshot catalog is invalid");
            if (!catalog.equals(new TreeSet<>(tables.stream().map(t -> t.entity().getType()).toList()))) throw invalid("snapshot catalog differs from model");
            List<Mutation> mutations = new ArrayList<>(); Set<String> identities = new HashSet<>();
            for (JsonNode row : body.path("rows")) {
                Mutation mutation = decodeSnapshotRow(metadata, row);
                if (!identities.add(mutation.table() + ":" + mutation.id())) throw invalid("duplicate snapshot row");
                mutations.add(mutation);
            }
            sql.execute("PRAGMA defer_foreign_keys=ON");
            for (Table table : tables) {
                if (mode == ImportMode.REQUIRE_EMPTY && count(table.table()) != 0) throw conflict("target domain is not empty; explicit replacement required");
                if (mode == ImportMode.REPLACE_UNINITIALIZED) sql.execute("DELETE FROM " + quoted(table.table()));
            }
            for (Mutation mutation : mutations) applyOne(sql, mutation);
            // Later triggers/rows must not rewrite earlier authoritative facts.
            for (Mutation mutation : mutations) verifyReadback(sql, mutation);
            for (Table table : tables) {
                long expected = mutations.stream().filter(m -> m.table().equals(table.table())).count();
                if (count(table.table()) != expected) throw invalid("target snapshot row count differs");
                if (!sql.queryForList("PRAGMA foreign_key_check(" + quoted(table.table()) + ")", new Object[0]).isEmpty()) throw invalid("snapshot has a dangling reference");
            }
            sql.update("INSERT INTO teaql_event_snapshot(source_runtime_id,snapshot_id,source_sequence,model_hash,payload_sha256,payload_json) VALUES (?,?,?,?,?,?)",
                    new Object[] {source, record.snapshotId(), record.sourceSequence(), model, record.payloadSha256(), record.payloadJson()});
            sql.update("INSERT INTO teaql_event_checkpoint(source_runtime_id,source_sequence,model_hash) VALUES (?,?,?)", new Object[] {source, record.sourceSequence(), model});
            result.set(SqliteCommittedEventStore.InboxOutcome.applied(record.sourceSequence()));
        });
        return result.get();
    }

    JsonNode validate(SnapshotRecord record, Limits limits) {
        if (record == null || !source.equals(record.sourceRuntimeId())) throw invalid("snapshot source is not authorized");
        if (!model.equals(record.modelHash())) throw invalid("snapshot model mismatch");
        if (blank(record.snapshotId()) || record.sourceSequence() < 0) throw invalid("snapshot identity/position is invalid");
        bounded(record.payloadJson(), limits);
        if (!digest(record.payloadJson()).equals(record.payloadSha256())) throw invalid("snapshot digest mismatch");
        JsonNode body;
        try { body = JSON.readTree(record.payloadJson()); } catch (Exception failure) { throw invalid("snapshot JSON is invalid"); }
        if (!body.isObject() || body.size() != 8 || !body.path("snapshot_version").isInt() || body.path("snapshot_version").intValue() != 1
                || !record.snapshotId().equals(body.path("snapshot_id").textValue()) || !source.equals(body.path("source_runtime_id").textValue())
                || !Long.toString(record.sourceSequence()).equals(body.path("source_sequence").textValue()) || !model.equals(body.path("source_model_hash").textValue())
                || !body.path("rows").isArray() || body.path("rows").size() > limits.maxRows()
                || blank(body.path("audit").path("comment").asText()) || blank(body.path("audit").path("purpose").asText())) throw invalid("snapshot header or rows are invalid");
        SqliteCommittedEventStore.rejectUnknownControls(body, SNAPSHOT_FIELDS);
        SqliteCommittedEventStore.rejectUnknownControls(body.path("audit"), AUDIT_FIELDS);
        return body;
    }
    private long position() {
        var positions = sql.queryForList("SELECT current_sequence FROM teaql_event_sequence WHERE source_runtime_id=?", new Object[] {source});
        long n = positions.isEmpty() ? 0 : integer(positions.get(0).get("current_sequence"));
        var latest = sql.queryForList("SELECT source_sequence,model_hash FROM teaql_event_outbox WHERE source_runtime_id=? ORDER BY source_sequence DESC LIMIT 1", new Object[] {source});
        if (n < 0 || n == 0 && !latest.isEmpty() || n > 0 && (latest.size() != 1 || integer(latest.get(0).get("source_sequence")) != n || !model.equals(latest.get(0).get("model_hash")))) throw invalid("source sequence/model is inconsistent");
        return n;
    }
    private static List<Table> tables(EntityMetaFactory metadata) {
        List<Table> result = new ArrayList<>(); Set<String> names = new HashSet<>(), physical = new HashSet<>();
        for (EntityDescriptor entity : metadata.allEntityDescriptors()) {
            if (entity.getParent() != null || entity.hasChildren() || entity.findIdProperty() == null || entity.findVersionProperty() == null) throw invalid("unsupported snapshot entity mapping");
            Mapping id = mapping(entity.findIdProperty()), version = mapping(entity.findVersionProperty());
            String physicalName = id.table().toLowerCase(Locale.ROOT);
            if (!names.add(entity.getType()) || !physical.add(physicalName) || physicalName.startsWith("teaql_event_") || !id.table().equals(version.table())) throw invalid("ambiguous/reserved snapshot table");
            var fields = entity.getOwnProperties().stream().filter(p -> !p.isId() && !p.isVersion()).toList();
            Set<String> columns = new HashSet<>(List.of(id.column(), version.column()));
            for (PropertyDescriptor field : fields) if (!id.table().equals(mapping(field).table()) || !columns.add(mapping(field).column())) throw invalid("unsupported snapshot field mapping");
            result.add(new Table(entity, fields, id.table(), id.column(), version.column()));
        }
        if (result.isEmpty()) throw invalid("snapshot requires an installed model");
        result.sort(Comparator.comparing(t -> t.entity().getType())); return result;
    }
    private static JsonNode encode(PropertyDescriptor property, Object value) {
        JsonNode wire;
        Class<?> type = property.getType() == null ? null : property.getType().javaType();
        if (value == null) wire = JSON.nullNode();
        else if (property instanceof Relation relation) wire = JSON.createObjectNode()
                .put("entity_type", relation.getReverseProperty().getOwner().getType()).put("entity_id", integer(value).toString());
        else if (type == Boolean.class || type == boolean.class) {
            long number = integer(value); if (number != 0 && number != 1) throw invalid("source Boolean storage is invalid"); wire = JSON.valueToTree(number == 1);
        } else if (type == BigDecimal.class || type == java.math.BigInteger.class) {
            if (value instanceof Double || value instanceof Float) throw invalid("source Decimal is approximate REAL"); wire = JSON.valueToTree(value.toString());
        } else if (type == Float.class || type == float.class || type == Double.class || type == double.class) {
            if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) throw invalid("source floating storage is invalid"); wire = JSON.valueToTree(number.doubleValue());
        } else if (type == byte[].class && value instanceof byte[] bytes) wire = JSON.createObjectNode().put("$binary_base64", Base64.getEncoder().encodeToString(bytes));
        else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) wire = JSON.valueToTree(integer(value).toString());
        else if (value instanceof String text) wire = JSON.valueToTree(text);
        else throw invalid("source storage representation is unsupported");
        Object decoded = decodeValue(property, wire);
        if (!sameValue(new Field(mapping(property).column(), decoded, type == BigDecimal.class || type == java.math.BigInteger.class), value)) throw invalid("source field cannot be represented losslessly");
        return wire;
    }
    private long count(String table) { return integer(sql.queryForList("SELECT COUNT(*) AS n FROM " + quoted(table), new Object[0]).get(0).get("n")); }
    private void standalone() { if (sql.hasActiveTransaction()) throw invalid("snapshot requires a standalone transaction"); }
    private void requireTransaction() { if (!sql.hasActiveTransaction()) throw invalid("snapshot adapter must provide an active SQL transaction"); }
    private static Long integer(Object value) { if (!(value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte)) throw invalid("source identity/integer storage is invalid"); return ((Number) value).longValue(); }
    private static int bytes(String text) { return text.getBytes(StandardCharsets.UTF_8).length; }
    private static void bounded(String text, Limits limits) { if (text == null || text.length() > limits.maxPayloadBytes() || bytes(text) > limits.maxPayloadBytes()) throw invalid("snapshot byte budget exceeded"); }
    private static String digest(String text) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); } }
    private static boolean blank(String text) { return text == null || text.isBlank(); }
    private static SqliteCommittedEventStore.CommittedEventException invalid(String message) { return new SqliteCommittedEventStore.CommittedEventException("INVALID_SNAPSHOT", message); }
    private static SqliteCommittedEventStore.CommittedEventException conflict(String message) { return new SqliteCommittedEventStore.CommittedEventException("SNAPSHOT_CONFLICT", message); }
}
