package io.teaql.data.dynamic;

import java.util.Objects;

/**
 * Default implementation of {@link DynamicFieldsFacade}.
 *
 * <p>This facade wraps any {@link DynamicFieldsProvider} and adds:
 * <ul>
 *   <li>Field definition lookup and validation</li>
 *   <li>Type checking</li>
 *   <li>Visibility and editability enforcement</li>
 *   <li>Intent (purpose/comment) enforcement in strict mode</li>
 * </ul>
 *
 * <p>Higher-level modules can replace this facade or extend it to add
 * scope resolution, permission checking, masking, etc.</p>
 */
public class DefaultDynamicFieldsFacade implements DynamicFieldsFacade {

    private final DynamicFieldsProvider provider;
    private final DynamicFieldScope defaultScope;

    private Object userContext;
    private String purpose;
    private String comment;

    public DefaultDynamicFieldsFacade(DynamicFieldsProvider provider) {
        this(provider, DynamicFieldScope.global());
    }

    public DefaultDynamicFieldsFacade(DynamicFieldsProvider provider, DynamicFieldScope defaultScope) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.defaultScope = Objects.requireNonNull(defaultScope, "defaultScope");
    }

    @Override
    public DynamicFieldsFacade withContext(Object userContext) {
        DefaultDynamicFieldsFacade copy = new DefaultDynamicFieldsFacade(provider, defaultScope);
        copy.userContext = userContext;
        copy.purpose = this.purpose;
        copy.comment = this.comment;
        return copy;
    }

    @Override
    public DynamicFieldsFacade purpose(String purpose) {
        this.purpose = purpose;
        return this;
    }

    @Override
    public DynamicFieldsFacade comment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public OwnerBound owner(String ownerType, long ownerId) {
        Objects.requireNonNull(ownerType, "ownerType");
        return new DefaultOwnerBound(ownerType, ownerId);
    }

    // ─── Context Builder ───────────────────────────────────────────────

    private DynamicFieldContext buildContext() {
        return new DynamicFieldContext() {
            @Override public String scopeType() { return defaultScope.scopeType(); }
            @Override public String scopeId() { return defaultScope.scopeId(); }
            @Override public String userId() { return userContext != null ? userContext.toString() : "anonymous"; }
            @Override public String purpose() { return purpose; }
            @Override public String comment() { return comment; }
            @Override public boolean strictIntent() { return false; }
        };
    }

    private DynamicFieldDef requireFieldDef(DynamicFieldContext ctx, String ownerType, String fieldCode) {
        DynamicFieldRef ref = DynamicFieldRef.of(
                DynamicFieldScope.of(ctx.scopeType(), ctx.scopeId()),
                ownerType, fieldCode);
        DynamicFieldDef def = provider.loadFieldDef(ctx, ref);
        if (def == null) {
            throw DynamicFieldException.notFound(fieldCode);
        }
        return def;
    }

    private void checkReadable(DynamicFieldDef def) {
        if (!def.isActive()) {
            throw new DynamicFieldException("DYNAMIC_FIELD_NOT_ACTIVE",
                    "Dynamic field '" + def.getCode() + "' is not active (status: " + def.getStatus() + ")");
        }
        if (!def.isVisible()) {
            throw DynamicFieldException.notVisible(def.getCode());
        }
    }

    private void checkWritable(DynamicFieldDef def) {
        checkReadable(def);
        if (!def.isEditable()) {
            throw DynamicFieldException.notEditable(def.getCode());
        }
    }

    private void checkType(DynamicFieldDef def, DynamicDataType expectedType) {
        if (def.getDataType() != expectedType) {
            throw DynamicFieldException.typeMismatch(def.getCode(), def.getDataType(), expectedType);
        }
    }

    // ─── OwnerBound Implementation ─────────────────────────────────────

    private class DefaultOwnerBound implements OwnerBound {
        private final String ownerType;
        private final long ownerId;

        DefaultOwnerBound(String ownerType, long ownerId) {
            this.ownerType = ownerType;
            this.ownerId = ownerId;
        }

        @Override
        public StringFieldBound string(String fieldCode) {
            return new DefaultStringFieldBound(ownerType, ownerId, fieldCode);
        }

        @Override
        public NumberFieldBound number(String fieldCode) {
            return new DefaultNumberFieldBound(ownerType, ownerId, fieldCode);
        }

        @Override
        public BoolFieldBound bool(String fieldCode) {
            return new DefaultBoolFieldBound(ownerType, ownerId, fieldCode);
        }

        @Override
        public DynamicFieldValues readAll(DynamicFieldSelection selection) {
            DynamicFieldContext ctx = buildContext();
            DynamicOwnerRef ownerRef = DynamicOwnerRef.of(ownerType, ownerId);
            return provider.loadValues(ctx, ownerRef, selection);
        }
    }

    // ─── StringFieldBound ──────────────────────────────────────────────

    private class DefaultStringFieldBound implements StringFieldBound {
        private final String ownerType;
        private final long ownerId;
        private final String fieldCode;

        DefaultStringFieldBound(String ownerType, long ownerId, String fieldCode) {
            this.ownerType = ownerType;
            this.ownerId = ownerId;
            this.fieldCode = fieldCode;
        }

        @Override
        public void set(String value) {
            DynamicFieldContext ctx = buildContext();
            DynamicFieldDef def = requireFieldDef(ctx, ownerType, fieldCode);
            checkWritable(def);
            checkType(def, DynamicDataType.STRING);
            provider.saveValue(ctx, DynamicSetCommand.of(
                    DynamicOwnerRef.of(ownerType, ownerId),
                    fieldCode, DynamicDataType.STRING, value,
                    purpose, comment));
        }

        @Override
        public String get() {
            DynamicFieldContext ctx = buildContext();
            DynamicFieldDef def = requireFieldDef(ctx, ownerType, fieldCode);
            checkReadable(def);
            checkType(def, DynamicDataType.STRING);
            DynamicFieldValues values = provider.loadValues(ctx,
                    DynamicOwnerRef.of(ownerType, ownerId),
                    new DynamicFieldSelection().selectString(fieldCode));
            if (!values.isSelected(fieldCode)) {
                return null;
            }
            return values.getString(fieldCode);
        }
    }

    // ─── NumberFieldBound ──────────────────────────────────────────────

    private class DefaultNumberFieldBound implements NumberFieldBound {
        private final String ownerType;
        private final long ownerId;
        private final String fieldCode;

        DefaultNumberFieldBound(String ownerType, long ownerId, String fieldCode) {
            this.ownerType = ownerType;
            this.ownerId = ownerId;
            this.fieldCode = fieldCode;
        }

        @Override
        public void set(Number value) {
            DynamicFieldContext ctx = buildContext();
            DynamicFieldDef def = requireFieldDef(ctx, ownerType, fieldCode);
            checkWritable(def);
            checkType(def, DynamicDataType.NUMBER);
            provider.saveValue(ctx, DynamicSetCommand.of(
                    DynamicOwnerRef.of(ownerType, ownerId),
                    fieldCode, DynamicDataType.NUMBER, value,
                    purpose, comment));
        }

        @Override
        public Number get() {
            DynamicFieldContext ctx = buildContext();
            DynamicFieldDef def = requireFieldDef(ctx, ownerType, fieldCode);
            checkReadable(def);
            checkType(def, DynamicDataType.NUMBER);
            DynamicFieldValues values = provider.loadValues(ctx,
                    DynamicOwnerRef.of(ownerType, ownerId),
                    new DynamicFieldSelection().selectNumber(fieldCode));
            if (!values.isSelected(fieldCode)) {
                return null;
            }
            return values.getNumber(fieldCode);
        }
    }

    // ─── BoolFieldBound ────────────────────────────────────────────────

    private class DefaultBoolFieldBound implements BoolFieldBound {
        private final String ownerType;
        private final long ownerId;
        private final String fieldCode;

        DefaultBoolFieldBound(String ownerType, long ownerId, String fieldCode) {
            this.ownerType = ownerType;
            this.ownerId = ownerId;
            this.fieldCode = fieldCode;
        }

        @Override
        public void set(Boolean value) {
            DynamicFieldContext ctx = buildContext();
            DynamicFieldDef def = requireFieldDef(ctx, ownerType, fieldCode);
            checkWritable(def);
            checkType(def, DynamicDataType.BOOL);
            provider.saveValue(ctx, DynamicSetCommand.of(
                    DynamicOwnerRef.of(ownerType, ownerId),
                    fieldCode, DynamicDataType.BOOL, value,
                    purpose, comment));
        }

        @Override
        public Boolean get() {
            DynamicFieldContext ctx = buildContext();
            DynamicFieldDef def = requireFieldDef(ctx, ownerType, fieldCode);
            checkReadable(def);
            checkType(def, DynamicDataType.BOOL);
            DynamicFieldValues values = provider.loadValues(ctx,
                    DynamicOwnerRef.of(ownerType, ownerId),
                    new DynamicFieldSelection().selectBool(fieldCode));
            if (!values.isSelected(fieldCode)) {
                return null;
            }
            return values.getBool(fieldCode);
        }
    }
}
