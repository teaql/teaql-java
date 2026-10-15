package io.teaql.core.sqlite;

import com.fasterxml.jackson.databind.JsonNode;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.portable.SQLPropertyUtil;
import io.teaql.dataservice.sql.SqlExecutionAdapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Startup-owned, one-source SQLite target for committed model facts. This is
 * infrastructure, not a business-facing Mutation or remote query endpoint.
 */
public final class SqliteModelCheckedCommittedTarget {
    private static final Set<String> TRACE_KINDS = Set.of(
            "OPERATION", "REQUEST", "RELATION", "ENTITY", "PROVIDER", "SQL",
            "COMMENT", "PURPOSE", "AUDIT_REASON");
    private final SqlExecutionAdapter sql;
    private final SqliteCommittedEventStore events;
    private final String sourceRuntimeId;
    private final String modelHash;

    public SqliteModelCheckedCommittedTarget(SqlExecutionAdapter sql,
            String sourceRuntimeId, String modelHash) {
        this.sql = Objects.requireNonNull(sql, "sql");
        if (sourceRuntimeId == null || sourceRuntimeId.isBlank()
                || modelHash == null || !modelHash.matches("sha256:[0-9a-f]{64}")) {
            throw invalid("target source identity or SHA-256 model hash is invalid");
        }
        this.sourceRuntimeId = sourceRuntimeId;
        this.modelHash = modelHash;
        this.events = new SqliteCommittedEventStore(sql);
    }

    public void ensureSchema() { events.ensureSchema(); }

    /** One graph, its Inbox identity, original audit payload and checkpoint commit together. */
    public SqliteCommittedEventStore.InboxOutcome apply(
            UserContext context, SqliteCommittedEventStore.OutboxRecord record) {
        Objects.requireNonNull(context, "context");
        if (record == null) throw invalid("committed event is missing");
        if (!sourceRuntimeId.equals(record.sourceRuntimeId())) {
            throw new SqliteCommittedEventStore.CommittedEventException(
                    "UNAUTHORIZED_SOURCE", "committed-event source is not authorized");
        }
        // Resolve from this Context, never from the legacy process-global fallback.
        EntityMetaFactory metadata = EntityMetaFactory.requireFrom(context);
        AtomicReference<SqliteCommittedEventStore.InboxOutcome> result = new AtomicReference<>();
        sql.executeInTransaction(() -> result.set(events.apply(record, modelHash,
                (transaction, payload) -> applyGraph(transaction, metadata, payload))));
        return result.get();
    }

    private static void applyGraph(SqlExecutionAdapter sql, EntityMetaFactory metadata, JsonNode payload) {
        JsonNode audit = payload.path("audit");
        if (!audit.isObject() || !nonblank(audit.path("comment"))
                || !nonblank(audit.path("purpose"))
                || !nonblank(payload.path("source_transaction_id"))) {
            throw invalid("source transaction or audit intent is missing");
        }
        try { OffsetDateTime.parse(payload.path("committed_at").asText()); }
        catch (RuntimeException failure) { throw invalid("commit time is invalid"); }
        List<Mutation> mutations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode fact : payload.path("mutations")) {
            Mutation mutation = decode(metadata, fact);
            if (!seen.add(mutation.entityType() + ":" + mutation.id())) {
                throw invalid("one set mutates the same entity twice");
            }
            mutations.add(mutation);
        }
        // Decode every fact before touching domain rows. The surrounding
        // savepoint also rolls back a later FK or readback failure.
        for (Mutation mutation : mutations) applyOne(sql, mutation);
    }

    private static Mutation decode(EntityMetaFactory metadata, JsonNode fact) {
        if (!fact.isObject() || !nonblank(fact.path("entity_type"))) {
            throw invalid("mutation entity type is missing");
        }
        validateTraceChain(fact.path("trace_chain"));
        String entityType = fact.path("entity_type").textValue();
        EntityDescriptor entity = metadata.allEntityDescriptors().stream()
                .filter(candidate -> entityType.equals(candidate.getType())).findFirst()
                .orElseThrow(() -> invalid("unknown entity type"));
        // Multi-table inheritance needs an explicit target mapping. Reject it
        // instead of silently writing only the leaf table.
        if (entity.getParent() != null || entity.hasChildren()) {
            throw invalid("multi-table entity target mapping is not supported");
        }
        PropertyDescriptor idProperty = entity.findIdProperty();
        PropertyDescriptor versionProperty = entity.findVersionProperty();
        if (idProperty == null || versionProperty == null) {
            throw invalid("entity has no ID or version property");
        }
        Mapping idMapping = mapping(idProperty);
        Mapping versionMapping = mapping(versionProperty);
        if (!idMapping.table().equals(versionMapping.table())) {
            throw invalid("identity and version span multiple tables");
        }
        long id = positiveId(fact.path("entity_id"));
        String operation = fact.path("operation").asText("");
        if (!List.of("create", "update", "delete", "recover").contains(operation)) {
            throw invalid("unknown mutation operation");
        }
        JsonNode baseNode = fact.path("base_version");
        Long base = "create".equals(operation) && baseNode.isNull()
                ? null : signedVersion(baseNode);
        long committed = signedVersion(fact.path("committed_version"));
        boolean valid = switch (operation) {
            case "create" -> base == null && committed == 1;
            case "update" -> base != null && base > 0 && base < Long.MAX_VALUE
                    && committed == base + 1;
            case "delete" -> base != null && base > 0 && base < Long.MAX_VALUE
                    && committed == -(base + 1);
            case "recover" -> base != null && base < 0 && base != Long.MIN_VALUE
                    && -base < Long.MAX_VALUE && committed == -base + 1;
            default -> false;
        };
        if (!valid) throw invalid("invalid version transition");
        JsonNode supplied = fact.path("fields");
        if (!supplied.isObject()) throw invalid("mutation fields are missing");
        if ("delete".equals(operation) && !supplied.isEmpty()) {
            throw invalid("delete must be a tombstone");
        }
        List<PropertyDescriptor> ownProperties = entity.getOwnProperties();
        supplied.fieldNames().forEachRemaining(name -> {
            PropertyDescriptor property = entity.findProperty(name);
            if (property == null || !ownProperties.contains(property)
                    || property.isId() || property.isVersion()) {
                throw invalid("unknown or reserved model field");
            }
        });
        List<Field> fields = new ArrayList<>();
        for (PropertyDescriptor property : ownProperties) {
            if (property.isId() || property.isVersion()) continue;
            JsonNode value = supplied.get(property.getName());
            if (value == null) {
                if ("create".equals(operation)) throw invalid("create omitted a modeled field");
                continue;
            }
            Mapping column = mapping(property);
            if (!idMapping.table().equals(column.table())) {
                throw invalid("mutation field spans multiple tables");
            }
            Class<?> type = property.getType() == null ? null : property.getType().javaType();
            fields.add(new Field(column.column(), decodeValue(property, value),
                    type == BigDecimal.class || type == java.math.BigInteger.class));
        }
        return new Mutation(entityType, idMapping.table(), idMapping.column(),
                versionMapping.column(), id, base, committed, operation, fields);
    }

    private static void validateTraceChain(JsonNode chain) {
        if (!chain.isArray()) throw invalid("mutation trace chain is missing");
        for (JsonNode node : chain) {
            JsonNode id = node.path("entity_id");
            if (!node.isObject() || node.size() != 4
                    || !TRACE_KINDS.contains(node.path("kind").asText(""))
                    || !node.path("name").isTextual()
                    || !node.path("comment").isTextual()
                    || !(id.isNull() || id.isTextual() && id.textValue().matches("[1-9][0-9]*"))) {
                throw invalid("trace node has wrong shape");
            }
        }
    }

    private static Object decodeValue(PropertyDescriptor property, JsonNode value) {
        if (value.isNull()) {
            if (property.getBoolean("required", false)) throw invalid("non-null field is null");
            return null;
        }
        if (property instanceof Relation relation) {
            if (!value.isObject() || value.size() != 2 || relation.getReverseProperty() == null
                    || relation.getReverseProperty().getOwner() == null
                    || !relation.getReverseProperty().getOwner().getType()
                            .equals(value.path("entity_type").asText())) {
                throw invalid("reference target does not match model");
            }
            return positiveId(value.path("entity_id"));
        }
        Class<?> type = property.getType() == null ? null : property.getType().javaType();
        if (type == String.class || type == java.util.UUID.class || type == java.time.LocalDate.class
                || type == java.time.LocalDateTime.class || type == java.time.Instant.class
                || type == java.util.Date.class || type == java.sql.Date.class
                || type == java.sql.Timestamp.class || type != null && type.isEnum()) {
            if (!value.isTextual()) throw invalid("text/date field has wrong type");
            return value.textValue();
        }
        if (type == Boolean.class || type == boolean.class) {
            if (!value.isBoolean()) throw invalid("Boolean field has wrong type");
            return value.booleanValue() ? 1 : 0;
        }
        if (type == Long.class || type == long.class || type == Integer.class || type == int.class
                || type == Short.class || type == short.class || type == Byte.class || type == byte.class) {
            if (!value.isTextual() || !value.textValue().matches("-?(0|[1-9][0-9]*)")
                    || value.textValue().equals("-0")) {
                throw invalid("integer field has wrong type");
            }
            long number;
            try { number = Long.parseLong(value.textValue()); }
            catch (NumberFormatException failure) { throw invalid("integer field overflows"); }
            if (type == Integer.class || type == int.class) {
                if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw invalid("integer overflows field");
            } else if (type == Short.class || type == short.class) {
                if (number < Short.MIN_VALUE || number > Short.MAX_VALUE) throw invalid("integer overflows field");
            } else if (type == Byte.class || type == byte.class) {
                if (number < Byte.MIN_VALUE || number > Byte.MAX_VALUE) throw invalid("integer overflows field");
            }
            return number;
        }
        if (type == BigDecimal.class || type == java.math.BigInteger.class) {
            if (!value.isTextual()) throw invalid("Decimal field has wrong type");
            try {
                if (type == java.math.BigInteger.class) {
                    if (!value.textValue().matches("-?(0|[1-9][0-9]*)")
                            || value.textValue().equals("-0")) {
                        throw invalid("integer field has wrong type");
                    }
                    return new java.math.BigInteger(value.textValue()).toString();
                }
                return new BigDecimal(value.textValue()).toPlainString();
            }
            catch (NumberFormatException failure) { throw invalid("Decimal field is invalid"); }
        }
        if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
            if (!value.isNumber() || !Double.isFinite(value.doubleValue())) {
                throw invalid("floating field has wrong type");
            }
            return value.doubleValue();
        }
        if (type == byte[].class && value.isObject() && value.size() == 1
                && value.path("$binary_base64").isTextual()) {
            try { return java.util.Base64.getDecoder().decode(value.path("$binary_base64").textValue()); }
            catch (IllegalArgumentException failure) { throw invalid("binary field is invalid"); }
        }
        throw invalid("unsupported modeled field type");
    }

    private static void applyOne(SqlExecutionAdapter sql, Mutation mutation) {
        String table = quoted(mutation.table());
        String idColumn = quoted(mutation.idColumn());
        String versionColumn = quoted(mutation.versionColumn());
        List<Map<String, Object>> current = sql.queryForList("SELECT " + versionColumn
                + " AS committed_version FROM " + table + " WHERE " + idColumn + " = ?",
                new Object[] {mutation.id()});
        Long actualVersion = current.isEmpty() ? null : ((Number) current.get(0).get("committed_version")).longValue();
        if (!Objects.equals(actualVersion, mutation.baseVersion())) {
            throw new SqliteCommittedEventStore.CommittedEventException("VERSION_CONFLICT",
                    "committed-event predecessor version mismatch");
        }
        List<Object> args = new ArrayList<>();
        if ("create".equals(mutation.operation())) {
            List<String> columns = new ArrayList<>(List.of(idColumn, versionColumn));
            args.add(mutation.id());
            args.add(mutation.committedVersion());
            for (Field field : mutation.fields()) {
                columns.add(quoted(field.column()));
                args.add(field.value());
            }
            sql.update("INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                    + String.join(", ", java.util.Collections.nCopies(args.size(), "?")) + ")",
                    args.toArray());
        } else {
            List<String> assignments = new ArrayList<>(List.of(versionColumn + " = ?"));
            args.add(mutation.committedVersion());
            for (Field field : mutation.fields()) {
                assignments.add(quoted(field.column()) + " = ?");
                args.add(field.value());
            }
            args.add(mutation.id());
            args.add(mutation.baseVersion());
            int changed = sql.update("UPDATE " + table + " SET " + String.join(", ", assignments)
                    + " WHERE " + idColumn + " = ? AND " + versionColumn + " = ?", args.toArray());
            if (changed != 1) {
                throw new SqliteCommittedEventStore.CommittedEventException("VERSION_CONFLICT",
                        "committed-event predecessor version mismatch");
            }
        }
        verifyReadback(sql, mutation);
    }

    private static void verifyReadback(SqlExecutionAdapter sql, Mutation mutation) {
        List<String> projections = new ArrayList<>(List.of(quoted(mutation.versionColumn()) + " AS committed_version"));
        for (int i = 0; i < mutation.fields().size(); i++) {
            projections.add(quoted(mutation.fields().get(i).column()) + " AS field_" + i);
        }
        List<Map<String, Object>> rows = sql.queryForList("SELECT " + String.join(", ", projections)
                + " FROM " + quoted(mutation.table()) + " WHERE " + quoted(mutation.idColumn()) + " = ?",
                new Object[] {mutation.id()});
        if (rows.size() != 1 || !Objects.equals(mutation.committedVersion(),
                ((Number) rows.get(0).get("committed_version")).longValue())) {
            throw invalid("target did not retain committed version");
        }
        for (int i = 0; i < mutation.fields().size(); i++) {
            if (!sameValue(mutation.fields().get(i), rows.get(0).get("field_" + i))) {
                throw invalid("target did not retain committed field value");
            }
        }
    }

    private static boolean sameValue(Field field, Object actual) {
        Object expected = field.value();
        if (expected instanceof byte[] bytes) return actual instanceof byte[] other && Arrays.equals(bytes, other);
        if (expected == null || actual == null) return expected == actual;
        if (field.decimal() || expected instanceof Number && actual instanceof Number) {
            try { return new BigDecimal(expected.toString()).compareTo(new BigDecimal(actual.toString())) == 0; }
            catch (NumberFormatException failure) { return false; }
        }
        return Objects.equals(expected, actual);
    }

    private static Mapping mapping(PropertyDescriptor property) {
        List<SQLColumn> columns = SQLPropertyUtil.getColumns(property);
        if (columns.size() != 1) throw invalid("property has no single SQLite column");
        return new Mapping(columns.get(0).getTableName(), columns.get(0).getColumnName());
    }

    private static String quoted(String identifier) {
        if (identifier == null || identifier.isBlank()) throw invalid("model has an empty SQL identifier");
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static long positiveId(JsonNode value) {
        if (!value.isTextual()) throw invalid("identity must be a decimal string");
        try {
            long id = Long.parseLong(value.textValue());
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw invalid("identity is outside the SQLite ID range");
    }

    private static long signedVersion(JsonNode value) {
        if (!value.isTextual()) throw invalid("version must be a decimal string");
        try { return Long.parseLong(value.textValue()); }
        catch (NumberFormatException failure) { throw invalid("version must be a decimal string"); }
    }

    private static boolean nonblank(JsonNode value) { return value.isTextual() && !value.textValue().isBlank(); }
    private static SqliteCommittedEventStore.CommittedEventException invalid(String reason) {
        return new SqliteCommittedEventStore.CommittedEventException(
                "INVALID_ENVELOPE", "invalid committed-event envelope: " + reason);
    }

    private record Mapping(String table, String column) { }
    private record Field(String column, Object value, boolean decimal) { }
    private record Mutation(String entityType, String table, String idColumn,
            String versionColumn, long id, Long baseVersion, long committedVersion,
            String operation, List<Field> fields) { }
}
