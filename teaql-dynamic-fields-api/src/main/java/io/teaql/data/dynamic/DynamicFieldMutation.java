package io.teaql.data.dynamic;

/** Explicit extension intent; Set(null) is a stored NULL, never deletion. */
public record DynamicFieldMutation(String code, DynamicDataType dataType, Kind kind, Object value) {
    public enum Kind { SET, DELETE }
    public DynamicFieldMutation {
        if (code == null || code.isBlank() || code.startsWith("#") || code.startsWith("_"))
            throw new DynamicFieldException("DYNAMIC_FIELD_INVALID_CODE", "Expected a bare dynamic field code");
        java.util.Objects.requireNonNull(dataType, "dataType");
        java.util.Objects.requireNonNull(kind, "kind");
        if (kind == Kind.DELETE && value != null) throw new IllegalArgumentException("Delete has no value");
        if (value != null && kind == Kind.SET) {
            boolean valid = switch (dataType) {
                case STRING, ENUM -> value instanceof String;
                case NUMBER -> value instanceof Number;
                case BOOL -> value instanceof Boolean;
                case DATE_TIME -> value instanceof Number || value instanceof java.util.Date || value instanceof java.time.temporal.TemporalAccessor;
            };
            if (!valid) throw new DynamicFieldException("DYNAMIC_FIELD_TYPE_MISMATCH", "Invalid dynamic field type: " + code);
        }
    }
    public static DynamicFieldMutation set(String code, DynamicDataType type, Object value) {
        return new DynamicFieldMutation(code, type, Kind.SET, value);
    }
    public static DynamicFieldMutation delete(String code, DynamicDataType type) {
        return new DynamicFieldMutation(code, type, Kind.DELETE, null);
    }
    public DynamicFieldValue asLoadedValue() {
        if (kind != Kind.SET) throw new IllegalStateException("Delete is not a loaded value");
        if (value == null) return DynamicFieldValue.ofNull(code, dataType);
        return switch (dataType) {
            case STRING -> DynamicFieldValue.ofString(code, (String) value);
            case ENUM -> DynamicFieldValue.ofEnum(code, (String) value);
            case NUMBER -> DynamicFieldValue.ofNumber(code, (Number) value);
            case BOOL -> DynamicFieldValue.ofBool(code, (Boolean) value);
            case DATE_TIME -> DynamicFieldValue.ofDateTime(code, value);
        };
    }
    @Override public String toString() { return "DynamicFieldMutation[" + code + ", " + kind + ", " + dataType + "]"; }
}
