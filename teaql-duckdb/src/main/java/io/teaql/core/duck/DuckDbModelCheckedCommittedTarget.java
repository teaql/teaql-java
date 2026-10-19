package io.teaql.core.duck;

import com.fasterxml.jackson.databind.JsonNode;
import io.teaql.core.UserContext;
import io.teaql.core.duck.DuckDbCommittedInbox.Failure;
import io.teaql.core.duck.DuckDbCommittedInbox.Record;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.Relation;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.portable.SQLPropertyUtil;
import io.teaql.dataservice.sql.SqlExecutionAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model-checked, one-source DuckDB target for committed v1 graph facts.
 * It writes only declared model columns and verifies the physical result
 * before the Inbox transaction is acknowledged.
 */
public final class DuckDbModelCheckedCommittedTarget {
    private static final Set<String> TRACE_KINDS = Set.of(
            "OPERATION", "REQUEST", "RELATION", "ENTITY", "PROVIDER", "SQL",
            "COMMENT", "PURPOSE", "AUDIT_REASON");
    private static final Pattern DECLARED_DECIMAL = Pattern.compile(
            "(?i)\\s*(?:NUMERIC|DECIMAL)\\s*\\(\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*\\)\\s*");
    private final SqlExecutionAdapter sql;
    private final DuckDbCommittedInbox inbox;

    private enum Kind { SCALAR, DECIMAL, DATE, INSTANT, BYTES }
    private record Mapping(String table, String column, String declaredType) { }
    private record Field(String column, Object value, Kind kind) {
        String bindExpression() { return kind == Kind.INSTANT ? "make_timestamptz(? * 1000)" : "?"; }
        String readExpression() {
            String name = quoted(column);
            return switch (kind) {
                case DATE -> "CAST(" + name + " AS VARCHAR)";
                case INSTANT -> "epoch_ms(" + name + ")";
                default -> name;
            };
        }
    }
    private record Mutation(String entityType, String table, String idColumn,
            String versionColumn, long id, Long baseVersion, long committedVersion,
            String operation, List<Field> fields) { }

    public DuckDbModelCheckedCommittedTarget(SqlExecutionAdapter sql,
            String sourceRuntimeId, String modelHash) {
        this.sql = Objects.requireNonNull(sql, "sql");
        this.inbox = new DuckDbCommittedInbox(sql, sourceRuntimeId, modelHash);
    }

    /** Infrastructure schema only; domain schema belongs to Context.ensureSchema(). */
    public void ensureInboxSchema() { inbox.ensureSchema(); }

    public DuckDbCommittedInbox.Outcome apply(UserContext context, Record record) {
        Objects.requireNonNull(context, "context");
        EntityMetaFactory metadata = EntityMetaFactory.requireFrom(context);
        return inbox.apply(record, (transaction, payload) -> applyGraph(transaction, metadata, payload));
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
            Mutation mutation = decode(sql, metadata, fact);
            if (!seen.add(mutation.entityType() + ":" + mutation.id())) {
                throw invalid("one set mutates the same entity twice");
            }
            mutations.add(mutation);
        }
        // Every fact is validated before the first domain write. A subsequent
        // SQL or readback error escapes to the Inbox owner for whole rollback.
        for (Mutation mutation : mutations) applyOne(sql, mutation);
    }

    private static Mutation decode(SqlExecutionAdapter sql, EntityMetaFactory metadata, JsonNode fact) {
        if (!fact.isObject() || !nonblank(fact.path("entity_type"))) {
            throw invalid("mutation entity type is missing");
        }
        validateTraceChain(fact.path("trace_chain"));
        String entityType = fact.path("entity_type").textValue();
        EntityDescriptor entity = metadata.allEntityDescriptors().stream()
                .filter(candidate -> entityType.equals(candidate.getType())).findFirst()
                .orElseThrow(() -> invalid("unknown entity type"));
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
        requirePhysical(sql, idMapping, "BIGINT", null, null);
        requirePhysical(sql, versionMapping, "BIGINT", null, null);
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
            fields.add(decodeField(sql, property, column, value));
        }
        return new Mutation(entityType, idMapping.table(), idMapping.column(),
                versionMapping.column(), id, base, committed, operation, fields);
    }

    private static Field decodeField(SqlExecutionAdapter sql, PropertyDescriptor property,
            Mapping mapping, JsonNode value) {
        if (value.isNull() && property.getBoolean("required", false)) {
            throw invalid("non-null field is null");
        }
        if (property instanceof Relation relation) {
            requirePhysical(sql, mapping, "BIGINT", null, null);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.SCALAR);
            if (!value.isObject() || value.size() != 2 || relation.getReverseProperty() == null
                    || relation.getReverseProperty().getOwner() == null
                    || !relation.getReverseProperty().getOwner().getType()
                            .equals(value.path("entity_type").asText())) {
                throw invalid("reference target does not match model");
            }
            return new Field(mapping.column(), positiveId(value.path("entity_id")), Kind.SCALAR);
        }
        Class<?> type = property.getType() == null ? null : property.getType().javaType();
        if (type == LocalDate.class || type == java.sql.Date.class) {
            requirePhysical(sql, mapping, "DATE", null, null);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.DATE);
            if (!value.isTextual()) throw invalid("Date field has wrong type");
            try {
                LocalDate date = LocalDate.parse(value.textValue());
                if (!date.toString().equals(value.textValue())) throw invalid("Date field is not canonical");
                return new Field(mapping.column(), date, Kind.DATE);
            } catch (java.time.format.DateTimeParseException failure) {
                throw invalid("Date field has wrong type");
            }
        }
        if (type == java.time.Instant.class || type == java.util.Date.class
                || type == java.sql.Timestamp.class) {
            requirePhysical(sql, mapping, "TIMESTAMP WITH TIME ZONE", null, null);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.INSTANT);
            long millis = canonicalLong(value, "Timestamp field has wrong type");
            try { Math.multiplyExact(millis, 1000L); }
            catch (ArithmeticException overflow) { throw invalid("Timestamp exceeds DuckDB instant range"); }
            return new Field(mapping.column(), millis, Kind.INSTANT);
        }
        if (type == java.time.LocalDateTime.class) {
            throw invalid("zone-free LocalDateTime has no committed instant mapping");
        }
        if (type == String.class || type == java.util.UUID.class || type != null && type.isEnum()) {
            requireTextPhysical(sql, mapping);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.SCALAR);
            if (!value.isTextual()) throw invalid("text field has wrong type");
            String text = value.textValue();
            String maxText = property.getStr("max", null);
            if (maxText != null) {
                try {
                    if (text.codePointCount(0, text.length()) > Integer.parseInt(maxText)) {
                        throw invalid("text exceeds modeled length");
                    }
                } catch (NumberFormatException malformed) { throw invalid("modeled text length is invalid"); }
            }
            return new Field(mapping.column(), text, Kind.SCALAR);
        }
        if (type == Boolean.class || type == boolean.class) {
            String physical = requireBooleanPhysical(sql, mapping);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.SCALAR);
            if (!value.isBoolean()) throw invalid("Boolean field has wrong type");
            return new Field(mapping.column(), "BOOLEAN".equals(physical)
                    ? value.booleanValue() : value.booleanValue() ? 1 : 0, Kind.SCALAR);
        }
        if (type == Long.class || type == long.class || type == Integer.class || type == int.class
                || type == Short.class || type == short.class || type == Byte.class || type == byte.class) {
            String physical = type == Long.class || type == long.class ? "BIGINT"
                    : type == Integer.class || type == int.class ? "INTEGER"
                    : type == Short.class || type == short.class ? "SMALLINT" : "TINYINT";
            requirePhysical(sql, mapping, physical, null, null);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.SCALAR);
            long number = canonicalLong(value, "integer field has wrong type");
            if (type == Integer.class || type == int.class) {
                if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw invalid("integer overflows field");
            } else if (type == Short.class || type == short.class) {
                if (number < Short.MIN_VALUE || number > Short.MAX_VALUE) throw invalid("integer overflows field");
            } else if (type == Byte.class || type == byte.class) {
                if (number < Byte.MIN_VALUE || number > Byte.MAX_VALUE) throw invalid("integer overflows field");
            }
            return new Field(mapping.column(), number, Kind.SCALAR);
        }
        if (type == BigDecimal.class) {
            Matcher declaration = DECLARED_DECIMAL.matcher(mapping.declaredType());
            if (!declaration.matches()) throw invalid("Decimal lacks modeled precision/scale");
            int precision;
            int scale;
            try {
                precision = Integer.parseInt(declaration.group(1));
                scale = Integer.parseInt(declaration.group(2));
            } catch (NumberFormatException malformed) { throw invalid("modeled Decimal precision/scale is invalid"); }
            if (precision < 1 || scale > precision) throw invalid("modeled Decimal precision/scale is invalid");
            requirePhysical(sql, mapping, "DECIMAL", precision, scale);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.DECIMAL);
            if (!value.isTextual()) throw invalid("Decimal field has wrong type");
            BigDecimal decimal;
            try { decimal = new BigDecimal(value.textValue()); }
            catch (NumberFormatException malformed) { throw invalid("Decimal field is invalid"); }
            BigDecimal normalized = decimal.stripTrailingZeros();
            long integerDigits = decimal.signum() == 0 ? 0
                    : Math.max(0L, (long) normalized.precision() - normalized.scale());
            long fractionalDigits = Math.max(0L, normalized.scale());
            if (integerDigits > precision - scale || fractionalDigits > scale) {
                throw invalid("Decimal exceeds modeled precision/scale");
            }
            return new Field(mapping.column(), decimal, Kind.DECIMAL);
        }
        if (type == Double.class || type == double.class) {
            requirePhysical(sql, mapping, "DOUBLE", null, null);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.SCALAR);
            if (!value.isNumber() || !Double.isFinite(value.doubleValue())) {
                throw invalid("floating field has wrong type");
            }
            return new Field(mapping.column(), value.doubleValue(), Kind.SCALAR);
        }
        if (type == byte[].class) {
            requirePhysical(sql, mapping, "BLOB", null, null);
            if (value.isNull()) return new Field(mapping.column(), null, Kind.BYTES);
            if (!value.isObject() || value.size() != 1
                    || !value.path("$binary_base64").isTextual()) {
                throw invalid("binary field has wrong type");
            }
            try {
                return new Field(mapping.column(), java.util.Base64.getDecoder()
                        .decode(value.path("$binary_base64").textValue()), Kind.BYTES);
            } catch (IllegalArgumentException malformed) { throw invalid("binary field is invalid"); }
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
        Long actualVersion = current.isEmpty() ? null
                : ((Number) current.get(0).get("committed_version")).longValue();
        if (!Objects.equals(actualVersion, mutation.baseVersion())) {
            throw new Failure("VERSION_CONFLICT", "committed-event predecessor version mismatch");
        }
        List<Object> args = new ArrayList<>();
        if ("create".equals(mutation.operation())) {
            List<String> columns = new ArrayList<>(List.of(idColumn, versionColumn));
            List<String> binds = new ArrayList<>(List.of("?", "?"));
            args.add(mutation.id());
            args.add(mutation.committedVersion());
            for (Field field : mutation.fields()) {
                columns.add(quoted(field.column()));
                binds.add(field.bindExpression());
                args.add(field.value());
            }
            if (sql.update("INSERT INTO " + table + " (" + String.join(", ", columns)
                    + ") VALUES (" + String.join(", ", binds) + ")", args.toArray()) != 1) {
                throw new Failure("TARGET_FAILURE", "committed target insert count mismatch");
            }
        } else {
            List<String> assignments = new ArrayList<>(List.of(versionColumn + " = ?"));
            args.add(mutation.committedVersion());
            for (Field field : mutation.fields()) {
                assignments.add(quoted(field.column()) + " = " + field.bindExpression());
                args.add(field.value());
            }
            args.add(mutation.id());
            args.add(mutation.baseVersion());
            int changed = sql.update("UPDATE " + table + " SET " + String.join(", ", assignments)
                    + " WHERE " + idColumn + " = ? AND " + versionColumn + " = ?", args.toArray());
            if (changed != 1) {
                throw new Failure("VERSION_CONFLICT", "committed-event predecessor version mismatch");
            }
        }
        verifyReadback(sql, mutation);
    }

    private static void verifyReadback(SqlExecutionAdapter sql, Mutation mutation) {
        List<String> projections = new ArrayList<>(List.of(
                quoted(mutation.versionColumn()) + " AS committed_version"));
        for (int i = 0; i < mutation.fields().size(); i++) {
            projections.add(mutation.fields().get(i).readExpression() + " AS field_" + i);
        }
        List<Map<String, Object>> rows = sql.queryForList("SELECT " + String.join(", ", projections)
                + " FROM " + quoted(mutation.table()) + " WHERE " + quoted(mutation.idColumn()) + " = ?",
                new Object[] {mutation.id()});
        if (rows.size() != 1 || !Objects.equals(mutation.committedVersion(),
                ((Number) rows.get(0).get("committed_version")).longValue())) {
            throw invalid("target did not retain committed version");
        }
        for (int i = 0; i < mutation.fields().size(); i++) {
            Field field = mutation.fields().get(i);
            if (!sameValue(field, rows.get(0).get("field_" + i))) {
                throw invalid("target did not retain committed field value");
            }
        }
    }

    private static boolean sameValue(Field field, Object actual) {
        Object expected = field.value();
        if (expected == null || actual == null) return expected == actual;
        if (field.kind() == Kind.BYTES) {
            return actual instanceof byte[] bytes && Arrays.equals((byte[]) expected, bytes);
        }
        if (field.kind() == Kind.DATE) return expected.toString().equals(actual.toString());
        if (field.kind() == Kind.INSTANT) {
            return actual instanceof Number number && number.longValue() == (long) expected;
        }
        if (field.kind() == Kind.DECIMAL) {
            return actual instanceof BigDecimal decimal
                    && ((BigDecimal) expected).compareTo(decimal) == 0;
        }
        if (expected instanceof Number number && actual instanceof Number other) {
            return new BigDecimal(number.toString()).compareTo(new BigDecimal(other.toString())) == 0;
        }
        return Objects.equals(expected, actual);
    }

    private static Mapping mapping(PropertyDescriptor property) {
        List<SQLColumn> columns = SQLPropertyUtil.getColumns(property);
        if (columns.size() != 1 || columns.get(0).getType() == null) {
            throw invalid("property has no single declared SQL column");
        }
        SQLColumn column = columns.get(0);
        return new Mapping(column.getTableName(), column.getColumnName(), column.getType());
    }

    private static void requireTextPhysical(SqlExecutionAdapter sql, Mapping mapping) {
        if (!mapping.declaredType().matches("(?i)\\s*(?:VARCHAR(?:\\([0-9]+\\))?|TEXT)\\s*")) {
            throw invalid("text column declaration is unsupported");
        }
        requirePhysical(sql, mapping, "VARCHAR", null, null);
    }

    private static String requireBooleanPhysical(SqlExecutionAdapter sql, Mapping mapping) {
        String type = mapping.declaredType().trim().toUpperCase(java.util.Locale.ROOT);
        if (!type.equals("BOOLEAN") && !type.equals("TINYINT")) {
            throw invalid("Boolean column declaration is unsupported");
        }
        requirePhysical(sql, mapping, type, null, null);
        return type;
    }

    private static void requirePhysical(SqlExecutionAdapter sql, Mapping mapping,
            String expectedType, Integer precision, Integer scale) {
        String declared = mapping.declaredType().trim().toUpperCase(java.util.Locale.ROOT);
        if (expectedType.equals("TIMESTAMP WITH TIME ZONE")) {
            if (!declared.equals("TIMESTAMPTZ") && !declared.equals(expectedType)) {
                throw invalid("model Instant column is not an absolute timestamp");
            }
        } else if (!expectedType.equals("DECIMAL") && !expectedType.equals("VARCHAR")
                && !declared.equals(expectedType)) {
            throw invalid("modeled SQL type does not match field kind");
        }
        List<Map<String, Object>> rows = sql.queryForList(
                "SELECT data_type, numeric_precision, numeric_scale FROM information_schema.columns "
                        + "WHERE table_name = ? AND column_name = ?",
                new Object[] {mapping.table(), mapping.column()});
        if (rows.size() != 1) throw invalid("physical model column is missing or ambiguous");
        Map<String, Object> row = rows.get(0);
        String physical = (String) row.get("data_type");
        if (physical == null || !(expectedType.equals("DECIMAL")
                ? physical.startsWith("DECIMAL(") : physical.equals(expectedType))) {
            throw invalid("physical column has incompatible SQL type");
        }
        if (precision != null && (number(row.get("numeric_precision")) != precision
                || number(row.get("numeric_scale")) != scale)) {
            throw invalid("physical Decimal precision/scale differs from model");
        }
    }

    private static void validateTraceChain(JsonNode chain) {
        if (!chain.isArray()) throw invalid("mutation trace chain is missing");
        for (JsonNode node : chain) {
            JsonNode id = node.path("entity_id");
            if (!node.isObject() || node.size() != 4
                    || !TRACE_KINDS.contains(node.path("kind").asText(""))
                    || !nonblank(node.path("name"))
                    || !nonblank(node.path("comment"))
                    || !(id.isNull() || id.isTextual() && id.textValue().matches("[1-9][0-9]*"))) {
                throw invalid("trace node has wrong shape");
            }
        }
    }

    private static long positiveId(JsonNode value) {
        long id = canonicalLong(value, "identity is outside the DuckDB ID range");
        if (id <= 0) throw invalid("identity must be positive");
        return id;
    }

    private static long signedVersion(JsonNode value) {
        return canonicalLong(value, "version has wrong type");
    }

    private static long canonicalLong(JsonNode value, String reason) {
        if (!value.isTextual() || !value.textValue().matches("-?(0|[1-9][0-9]*)")
                || value.textValue().equals("-0")) throw invalid(reason);
        try { return Long.parseLong(value.textValue()); }
        catch (NumberFormatException overflow) { throw invalid(reason); }
    }

    private static String quoted(String identifier) {
        if (identifier == null || identifier.isBlank()) throw invalid("model has an empty SQL identifier");
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static long number(Object value) {
        if (!(value instanceof Number number)) throw invalid("physical numeric metadata is missing");
        return number.longValue();
    }
    private static boolean nonblank(JsonNode value) {
        return value.isTextual() && !value.textValue().isBlank();
    }
    private static Failure invalid(String reason) {
        return new Failure("INVALID_ENVELOPE", "invalid committed-event envelope: " + reason);
    }
}
