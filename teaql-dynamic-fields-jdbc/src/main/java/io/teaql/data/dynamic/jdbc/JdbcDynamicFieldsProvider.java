package io.teaql.data.dynamic.jdbc;

import io.teaql.data.dynamic.*;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.provider.jdbc.JdbcSqlExecutor;

import javax.sql.DataSource;
import java.util.*;
import java.util.logging.Logger;

/**
 * JDBC-based implementation of {@link DynamicFieldsProvider}.
 *
 * <p>Stores dynamic field definitions and values in two internal tables:
 * {@code teaql_dynamic_field_def} and {@code teaql_dynamic_field_value}.
 * Reuses the same DataSource/JDBC connection as the main application.</p>
 *
 * <p>Call {@link #ensureSchema()} once at application startup to auto-create the tables.</p>
 */
public class JdbcDynamicFieldsProvider implements DynamicFieldsProvider {

    private static final Logger LOG = Logger.getLogger(JdbcDynamicFieldsProvider.class.getName());

    private final SqlExecutionAdapter executor;

    // ─── SQL Constants ─────────────────────────────────────────────────

    private static final String SQL_LOAD_FIELD_DEF =
            "SELECT * FROM teaql_dynamic_field_def "
            + "WHERE scope_type = ? AND scope_id = ? AND owner_type = ? AND code = ?";

    private static final String SQL_LIST_FIELD_DEFS =
            "SELECT * FROM teaql_dynamic_field_def "
            + "WHERE scope_type = ? AND scope_id = ? AND owner_type = ? "
            + "ORDER BY display_order";

    private static final String SQL_LOAD_VALUES_SINGLE =
            "SELECT v.field_id, d.code, d.data_type, "
            + "v.string_value, v.number_value, v.bool_value, v.datetime_value, v.enum_value "
            + "FROM teaql_dynamic_field_value v "
            + "JOIN teaql_dynamic_field_def d ON v.field_id = d.id "
            + "WHERE v.scope_type = ? AND v.scope_id = ? "
            + "AND v.owner_type = ? AND v.owner_id = ?";

    // SQL_LOAD_VALUES_BATCH is built dynamically due to IN (...) clause

    private static final String SQL_UPDATE_VALUE =
            "UPDATE teaql_dynamic_field_value "
            + "SET string_value=?, number_value=?, bool_value=?, datetime_value=?, enum_value=?, "
            + "version=version+1, updated_by=?, updated_at=? "
            + "WHERE scope_type=? AND scope_id=? AND owner_type=? AND owner_id=? AND field_id=?";

    private static final String SQL_INSERT_VALUE =
            "INSERT INTO teaql_dynamic_field_value ("
            + "scope_type, scope_id, owner_type, owner_id, field_id, "
            + "string_value, number_value, bool_value, datetime_value, enum_value, "
            + "version, updated_by, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)";

    private static final String SQL_DELETE_VALUE =
            "DELETE FROM teaql_dynamic_field_value "
            + "WHERE scope_type=? AND scope_id=? AND owner_type=? AND owner_id=? AND field_id=?";

    private static final String SQL_INSERT_FIELD_DEF =
            "INSERT INTO teaql_dynamic_field_def ("
            + "id, scope_type, scope_id, owner_type, code, name, description, "
            + "data_type, logical_type, required, visible, editable, "
            + "filterable, sortable, searchable, exportable, importable, auditable, "
            + "privacy_level, mask_rule, default_value, status, display_order, "
            + "version, created_by, created_at, updated_by, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)";

    // ─── Constructors ──────────────────────────────────────────────────

    /**
     * Constructs from an existing SqlExecutionAdapter.
     */
    public JdbcDynamicFieldsProvider(SqlExecutionAdapter executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Constructs from a DataSource (convenience).
     * Internally creates a JdbcSqlExecutor.
     */
    public JdbcDynamicFieldsProvider(DataSource dataSource) {
        this(new JdbcSqlExecutor(dataSource));
    }

    /**
     * Creates the required tables and indexes if they don't exist.
     * Should be called once at application startup.
     */
    public void ensureSchema() {
        DynamicFieldsSchema.ensureSchema(executor);
    }

    // ─── Field Definition Management ───────────────────────────────────

    /**
     * Registers a field definition. Assigns an ID via teaql_id_space if not set.
     */
    public DynamicFieldDef registerFieldDef(DynamicFieldContext ctx, DynamicFieldDef def) {
        Objects.requireNonNull(def, "def");
        if (def.getId() == 0) {
            def.setId(ctx.nextId("DynamicFieldDef"));
        }
        if (def.getStatus() == null) {
            def.setStatus(DynamicFieldStatus.ACTIVE);
        }
        long now = System.currentTimeMillis();
        String userId = ctx != null ? ctx.userId() : null;
        executor.update(SQL_INSERT_FIELD_DEF, new Object[]{
                def.getId(),
                def.getScope().scopeType(), def.getScope().scopeId(),
                def.getOwnerType(), def.getCode(), def.getName(), def.getDescription(),
                def.getDataType().name(), def.getLogicalType() != null ? def.getLogicalType().name() : null,
                def.isRequired() ? 1 : 0, def.isVisible() ? 1 : 0, def.isEditable() ? 1 : 0,
                def.isFilterable() ? 1 : 0, def.isSortable() ? 1 : 0, def.isSearchable() ? 1 : 0,
                def.isExportable() ? 1 : 0, def.isImportable() ? 1 : 0, def.isAuditable() ? 1 : 0,
                def.getPrivacyLevel(), def.getMaskRule(), def.getDefaultValue(),
                def.getStatus().name(), def.getDisplayOrder(),
                userId, now, userId, now
        });
        return def;
    }

    // ─── DynamicFieldsProvider Implementation ──────────────────────────

    @Override
    public DynamicFieldDef loadFieldDef(DynamicFieldContext ctx, DynamicFieldRef ref) {
        List<Map<String, Object>> rows = executor.queryForList(SQL_LOAD_FIELD_DEF, new Object[]{
                ref.scope().scopeType(), ref.scope().scopeId(),
                ref.ownerType(), ref.code()
        });
        if (rows.isEmpty()) {
            return null;
        }
        return mapToFieldDef(rows.get(0));
    }

    @Override
    public List<DynamicFieldDef> listFieldDefs(DynamicFieldContext ctx, String ownerType) {
        List<Map<String, Object>> rows = executor.queryForList(SQL_LIST_FIELD_DEFS, new Object[]{
                ctx.scopeType(), ctx.scopeId(), ownerType
        });
        List<DynamicFieldDef> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapToFieldDef(row));
        }
        return result;
    }

    @Override
    public DynamicFieldValues loadValues(DynamicFieldContext ctx, DynamicOwnerRef ownerRef,
                                         DynamicFieldSelection selection) {
        List<Map<String, Object>> rows = executor.queryForList(SQL_LOAD_VALUES_SINGLE, new Object[]{
                ctx.scopeType(), ctx.scopeId(),
                ownerRef.ownerType(), ownerRef.ownerId()
        });
        return buildFieldValues(rows, selection);
    }

    @Override
    public Map<DynamicOwnerRef, DynamicFieldValues> loadValues(
            DynamicFieldContext ctx, List<DynamicOwnerRef> ownerRefs,
            DynamicFieldSelection selection) {

        if (ownerRefs.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build IN clause dynamically
        String ownerType = ownerRefs.get(0).ownerType();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT v.owner_id, v.field_id, d.code, d.data_type, ");
        sql.append("v.string_value, v.number_value, v.bool_value, v.datetime_value, v.enum_value ");
        sql.append("FROM teaql_dynamic_field_value v ");
        sql.append("JOIN teaql_dynamic_field_def d ON v.field_id = d.id ");
        sql.append("WHERE v.scope_type = ? AND v.scope_id = ? ");
        sql.append("AND v.owner_type = ? AND v.owner_id IN (");

        Object[] params = new Object[3 + ownerRefs.size()];
        params[0] = ctx.scopeType();
        params[1] = ctx.scopeId();
        params[2] = ownerType;
        for (int i = 0; i < ownerRefs.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
            params[3 + i] = ownerRefs.get(i).ownerId();
        }
        sql.append(")");

        List<Map<String, Object>> rows = executor.queryForList(sql.toString(), params);

        // Group by owner_id
        Map<Long, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            long ownerId = ((Number) row.get("owner_id")).longValue();
            grouped.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(row);
        }

        // Build result map
        Map<DynamicOwnerRef, DynamicFieldValues> result = new LinkedHashMap<>();
        for (DynamicOwnerRef ref : ownerRefs) {
            List<Map<String, Object>> ownerRows = grouped.getOrDefault(ref.ownerId(), Collections.emptyList());
            result.put(ref, buildFieldValues(ownerRows, selection));
        }
        return result;
    }

    @Override
    public void saveValue(DynamicFieldContext ctx, DynamicSetCommand command) {
        // Look up the field def to get its id
        DynamicFieldRef ref = DynamicFieldRef.of(
                DynamicFieldScope.of(ctx.scopeType(), ctx.scopeId()),
                command.ownerRef().ownerType(),
                command.fieldCode());
        DynamicFieldDef def = loadFieldDef(ctx, ref);
        if (def == null) {
            throw DynamicFieldException.notFound(command.fieldCode());
        }
        if (!def.isActive()) {
            throw new DynamicFieldException("DYNAMIC_FIELD_NOT_ACTIVE",
                    "Dynamic field '" + command.fieldCode() + "' is not active (status: " + def.getStatus() + ")");
        }
        if (!def.isEditable()) {
            throw DynamicFieldException.notEditable(command.fieldCode());
        }

        long now = System.currentTimeMillis();
        String userId = ctx.userId();

        // Extract typed values
        String stringVal = null;
        Long numberVal = null;
        Integer boolVal = null;
        Long datetimeVal = null;
        String enumVal = null;

        if (command.value() != null) {
            switch (command.dataType()) {
                case STRING -> stringVal = command.value().toString();
                case NUMBER -> numberVal = ((Number) command.value()).longValue();
                case BOOL -> boolVal = ((Boolean) command.value()) ? 1 : 0;
                case DATE_TIME -> datetimeVal = (command.value() instanceof Number n) ? n.longValue() : System.currentTimeMillis();
                case ENUM -> enumVal = command.value().toString();
            }
        }

        // Try UPDATE first
        int updated = executor.update(SQL_UPDATE_VALUE, new Object[]{
                stringVal, numberVal, boolVal, datetimeVal, enumVal,
                userId, now,
                ctx.scopeType(), ctx.scopeId(),
                command.ownerRef().ownerType(), command.ownerRef().ownerId(),
                def.getId()
        });

        // If no row existed, INSERT
        if (updated == 0) {
            executor.update(SQL_INSERT_VALUE, new Object[]{
                    ctx.scopeType(), ctx.scopeId(),
                    command.ownerRef().ownerType(), command.ownerRef().ownerId(),
                    def.getId(),
                    stringVal, numberVal, boolVal, datetimeVal, enumVal,
                    userId, now
            });
        }
    }

    @Override
    public void deleteValue(DynamicFieldContext ctx, DynamicValueRef valueRef) {
        // We need scope info from context
        executor.update(SQL_DELETE_VALUE, new Object[]{
                ctx.scopeType(), ctx.scopeId(),
                valueRef.ownerRef().ownerType(), valueRef.ownerRef().ownerId(),
                valueRef.fieldId()
        });
    }

    @Override
    public DynamicFieldCapabilities capabilities() {
        return DynamicFieldCapabilities.builder()
                .sourceOfTruth(true)
                .supportsTransaction(false)  // TODO: add transaction support
                .supportsBatchLoad(true)
                .supportsTypedValue(true)
                .supportsBasicPermission(true)
                .supportsBasicAudit(false)   // TODO: add audit trail
                .build();
    }

    // ─── Internal Helpers ──────────────────────────────────────────────

    private DynamicFieldValues buildFieldValues(List<Map<String, Object>> rows,
                                                 DynamicFieldSelection selection) {
        List<DynamicFieldValue> values = new ArrayList<>();
        Set<String> selectedCodes = null;

        if (!selection.isSelectAll()) {
            selectedCodes = new HashSet<>();
            for (DynamicFieldSelection.DynamicFieldSelectionEntry entry : selection.getEntries()) {
                selectedCodes.add(entry.code());
            }
        }

        for (Map<String, Object> row : rows) {
            String code = (String) row.get("code");
            String dataTypeStr = (String) row.get("data_type");

            // Filter by selection if not selectAll
            if (selectedCodes != null && !selectedCodes.contains(code)) {
                continue;
            }

            DynamicDataType dataType = DynamicDataType.valueOf(dataTypeStr);
            values.add(extractFieldValue(code, dataType, row));
        }

        return DynamicFieldValues.of(values);
    }

    private DynamicFieldValue extractFieldValue(String code, DynamicDataType dataType,
                                                 Map<String, Object> row) {
        return switch (dataType) {
            case STRING -> {
                Object v = row.get("string_value");
                yield v != null ? DynamicFieldValue.ofString(code, v.toString())
                                : DynamicFieldValue.ofNull(code, DynamicDataType.STRING);
            }
            case NUMBER -> {
                Object v = row.get("number_value");
                yield v != null ? DynamicFieldValue.ofNumber(code, ((Number) v).longValue())
                                : DynamicFieldValue.ofNull(code, DynamicDataType.NUMBER);
            }
            case BOOL -> {
                Object v = row.get("bool_value");
                yield v != null ? DynamicFieldValue.ofBool(code, ((Number) v).intValue() != 0)
                                : DynamicFieldValue.ofNull(code, DynamicDataType.BOOL);
            }
            case DATE_TIME -> {
                Object v = row.get("datetime_value");
                yield v != null ? DynamicFieldValue.ofDateTime(code, ((Number) v).longValue())
                                : DynamicFieldValue.ofNull(code, DynamicDataType.DATE_TIME);
            }
            case ENUM -> {
                Object v = row.get("enum_value");
                yield v != null ? DynamicFieldValue.ofEnum(code, v.toString())
                                : DynamicFieldValue.ofNull(code, DynamicDataType.ENUM);
            }
        };
    }

    private DynamicFieldDef mapToFieldDef(Map<String, Object> row) {
        DynamicFieldDef def = new DynamicFieldDef();
        def.setId(((Number) row.get("id")).longValue());
        def.setScope(DynamicFieldScope.of(
                (String) row.get("scope_type"),
                (String) row.get("scope_id")));
        def.setOwnerType((String) row.get("owner_type"));
        def.setCode((String) row.get("code"));
        def.setName((String) row.get("name"));
        def.setDescription((String) row.get("description"));
        def.setDataType(DynamicDataType.valueOf((String) row.get("data_type")));
        String logicalType = (String) row.get("logical_type");
        if (logicalType != null) {
            def.setLogicalType(DynamicLogicalType.valueOf(logicalType));
        }
        def.setRequired(intToBool(row.get("required")));
        def.setVisible(intToBool(row.get("visible")));
        def.setEditable(intToBool(row.get("editable")));
        def.setFilterable(intToBool(row.get("filterable")));
        def.setSortable(intToBool(row.get("sortable")));
        def.setSearchable(intToBool(row.get("searchable")));
        def.setExportable(intToBool(row.get("exportable")));
        def.setImportable(intToBool(row.get("importable")));
        def.setAuditable(intToBool(row.get("auditable")));
        def.setPrivacyLevel((String) row.get("privacy_level"));
        def.setMaskRule((String) row.get("mask_rule"));
        def.setDefaultValue((String) row.get("default_value"));
        def.setStatus(DynamicFieldStatus.valueOf((String) row.get("status")));
        Object displayOrder = row.get("display_order");
        if (displayOrder instanceof Number n) {
            def.setDisplayOrder(n.intValue());
        }
        Object version = row.get("version");
        if (version instanceof Number n) {
            def.setVersion(n.longValue());
        }
        def.setCreatedBy((String) row.get("created_by"));
        def.setUpdatedBy((String) row.get("updated_by"));
        return def;
    }

    private static boolean intToBool(Object value) {
        if (value == null) return false;
        return ((Number) value).intValue() != 0;
    }
}
