package io.teaql.data.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * In-memory implementation of {@link DynamicFieldsProvider}.
 *
 * <p><b>WARNING:</b> This implementation stores all field definitions and values in memory.
 * It is intended as a <b>technical feasibility demonstration</b> only.
 * Data will be lost when the JVM shuts down. Do NOT use in production.
 * Use a persistent implementation (e.g., TeaQL DB-backed provider) for real deployments.</p>
 */
public class InMemoryDynamicFieldsProvider implements DynamicFieldsProvider {

    private static final Logger LOG = Logger.getLogger(InMemoryDynamicFieldsProvider.class.getName());

    private final AtomicLong idSequence = new AtomicLong(1);

    // key: scope/ownerType/code -> DynamicFieldDef
    private final Map<String, DynamicFieldDef> fieldDefs = new ConcurrentHashMap<>();

    // key: scope/ownerType/ownerId/fieldId -> value object
    private final Map<String, Object> fieldValues = new ConcurrentHashMap<>();

    // key: fieldId -> DynamicFieldDef (for reverse lookup)
    private final Map<Long, DynamicFieldDef> fieldDefsById = new ConcurrentHashMap<>();

    public InMemoryDynamicFieldsProvider() {
        LOG.warning("╔══════════════════════════════════════════════════════════════╗");
        LOG.warning("║  InMemoryDynamicFieldsProvider initialized.                 ║");
        LOG.warning("║  This is an IN-MEMORY implementation for DEMO purposes only.║");
        LOG.warning("║  All dynamic field data will be LOST on JVM shutdown.        ║");
        LOG.warning("║  Replace with a persistent provider for production use.      ║");
        LOG.warning("╚══════════════════════════════════════════════════════════════╝");
    }

    // ─── Field Definition Management ───────────────────────────────────

    /**
     * Registers a field definition. If a definition with the same scope/ownerType/code
     * already exists, it will be replaced.
     */
    public DynamicFieldDef registerFieldDef(DynamicFieldDef def) {
        Objects.requireNonNull(def, "def");
        Objects.requireNonNull(def.getScope(), "def.scope");
        Objects.requireNonNull(def.getOwnerType(), "def.ownerType");
        Objects.requireNonNull(def.getCode(), "def.code");
        Objects.requireNonNull(def.getDataType(), "def.dataType");

        if (def.getId() == 0) {
            def.setId(idSequence.getAndIncrement());
        }
        if (def.getStatus() == null) {
            def.setStatus(DynamicFieldStatus.ACTIVE);
        }

        String key = defKey(def.getScope(), def.getOwnerType(), def.getCode());
        fieldDefs.put(key, def);
        fieldDefsById.put(def.getId(), def);
        return def;
    }

    // ─── DynamicFieldsProvider Implementation ──────────────────────────

    @Override
    public DynamicFieldDef loadFieldDef(DynamicFieldContext ctx, DynamicFieldRef ref) {
        String key = defKey(
                DynamicFieldScope.of(ctx.scopeType(), ctx.scopeId()),
                ref.ownerType(),
                ref.code());
        return fieldDefs.get(key);
    }

    @Override
    public List<DynamicFieldDef> listFieldDefs(DynamicFieldContext ctx, String ownerType) {
        String prefix = ctx.scopeType() + "/" + ctx.scopeId() + "/" + ownerType + "/";
        List<DynamicFieldDef> result = new ArrayList<>();
        for (Map.Entry<String, DynamicFieldDef> entry : fieldDefs.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }
        result.sort((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()));
        return result;
    }

    @Override
    public DynamicFieldValues loadValues(DynamicFieldContext ctx, DynamicOwnerRef ownerRef,
                                         DynamicFieldSelection selection) {
        Map<DynamicOwnerRef, DynamicFieldValues> batch =
                loadValues(ctx, Collections.singletonList(ownerRef), selection);
        return batch.getOrDefault(ownerRef, DynamicFieldValues.empty());
    }

    @Override
    public Map<DynamicOwnerRef, DynamicFieldValues> loadValues(
            DynamicFieldContext ctx, List<DynamicOwnerRef> ownerRefs,
            DynamicFieldSelection selection) {

        List<DynamicFieldDef> allDefs = null;
        if (selection.isSelectAll() && !ownerRefs.isEmpty()) {
            allDefs = listFieldDefs(ctx, ownerRefs.get(0).ownerType());
        }

        Map<DynamicOwnerRef, DynamicFieldValues> result = new HashMap<>();
        for (DynamicOwnerRef ownerRef : ownerRefs) {
            List<DynamicFieldValue> values = new ArrayList<>();

            if (selection.isSelectAll()) {
                // Load all active fields
                List<DynamicFieldDef> defs = (allDefs != null && ownerRef.ownerType().equals(ownerRefs.get(0).ownerType()))
                        ? allDefs : listFieldDefs(ctx, ownerRef.ownerType());
                for (DynamicFieldDef def : defs) {
                    if (!def.isActive()) continue;
                    String vKey = valueKey(ctx, ownerRef, def.getId());
                    Object val = fieldValues.get(vKey);
                    values.add(toFieldValue(def.getCode(), def.getDataType(), val));
                }
                result.put(ownerRef, DynamicFieldValues.of(values));
                continue;
            }
            // Load selected fields
            for (DynamicFieldSelection.DynamicFieldSelectionEntry entry : selection.getEntries()) {
                DynamicFieldRef ref = DynamicFieldRef.of(
                        DynamicFieldScope.of(ctx.scopeType(), ctx.scopeId()),
                        ownerRef.ownerType(),
                        entry.code());
                DynamicFieldDef def = loadFieldDef(ctx, ref);
                if (def == null) continue;
                String vKey = valueKey(ctx, ownerRef, def.getId());
                Object val = fieldValues.get(vKey);
                values.add(toFieldValue(entry.code(), entry.dataType(), val));
            }
            result.put(ownerRef, DynamicFieldValues.of(values));
        }
        return result;
    }

    @Override
    public void saveValue(DynamicFieldContext ctx, DynamicSetCommand command) {
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

        String vKey = valueKey(ctx, command.ownerRef(), def.getId());
        fieldValues.remove(vKey);
        if (command.value() != null) {
            fieldValues.put(vKey, command.value());
        }
    }

    @Override
    public void deleteValue(DynamicFieldContext ctx, DynamicValueRef valueRef) {
        DynamicFieldDef def = fieldDefsById.get(valueRef.fieldId());
        if (def == null) {
            return;
        }
        String vKey = valueKey(ctx, valueRef.ownerRef(), valueRef.fieldId());
        fieldValues.remove(vKey);
    }

    @Override
    public DynamicFieldCapabilities capabilities() {
        return DynamicFieldCapabilities.builder()
                .sourceOfTruth(false)
                .supportsTransaction(false)
                .supportsBatchLoad(true)
                .supportsTypedValue(true)
                .supportsBasicPermission(true)
                .supportsBasicAudit(false)
                .build();
    }

    // ─── Internal Helpers ──────────────────────────────────────────────

    private static String defKey(DynamicFieldScope scope, String ownerType, String code) {
        return scope.scopeType() + "/" + scope.scopeId() + "/" + ownerType + "/" + code;
    }

    private static String valueKey(DynamicFieldContext ctx, DynamicOwnerRef ownerRef, long fieldId) {
        return ctx.scopeType() + "/" + ctx.scopeId() + "/"
                + ownerRef.ownerType() + "/" + ownerRef.ownerId() + "/" + fieldId;
    }

    private static DynamicFieldValue toFieldValue(String code, DynamicDataType dataType, Object val) {
        if (val == null) {
            return DynamicFieldValue.ofNull(code, dataType);
        }
        return switch (dataType) {
            case STRING -> DynamicFieldValue.ofString(code, val.toString());
            case NUMBER -> DynamicFieldValue.ofNumber(code, (Number) val);
            case BOOL -> DynamicFieldValue.ofBool(code, (Boolean) val);
            case DATE_TIME -> DynamicFieldValue.ofDateTime(code, val);
            case ENUM -> DynamicFieldValue.ofEnum(code, val.toString());
        };
    }
}
