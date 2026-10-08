package io.teaql.data.dynamic;

import java.util.Objects;

public final class DynamicFieldValue {

    public enum State { VALUE, NULL, NOT_LOADED }

    private final String fieldCode;
    private final DynamicDataType dataType;
    private final Object value;
    private final State state;

    private DynamicFieldValue(String fieldCode, DynamicDataType dataType, Object value) {
        this(fieldCode, dataType, value, value == null ? State.NULL : State.VALUE);
    }

    private DynamicFieldValue(String fieldCode, DynamicDataType dataType, Object value, State state) {
        this.fieldCode = Objects.requireNonNull(fieldCode, "fieldCode");
        this.dataType = dataType;
        this.value = value;
        this.state = state;
    }

    public static DynamicFieldValue ofString(String code, String value) {
        return new DynamicFieldValue(code, DynamicDataType.STRING, value);
    }

    public static DynamicFieldValue ofNumber(String code, Number value) {
        return new DynamicFieldValue(code, DynamicDataType.NUMBER, value);
    }

    public static DynamicFieldValue ofBool(String code, Boolean value) {
        return new DynamicFieldValue(code, DynamicDataType.BOOL, value);
    }

    public static DynamicFieldValue ofDateTime(String code, Object value) {
        return new DynamicFieldValue(code, DynamicDataType.DATE_TIME, value);
    }

    public static DynamicFieldValue ofEnum(String code, String value) {
        return new DynamicFieldValue(code, DynamicDataType.ENUM, value);
    }

    public static DynamicFieldValue ofNull(String code, DynamicDataType dataType) {
        return new DynamicFieldValue(code, dataType, null);
    }

    public static DynamicFieldValue notLoaded(String code, DynamicDataType dataType) {
        return new DynamicFieldValue(code, dataType, null, State.NOT_LOADED);
    }

    public State state() { return state; }
    public boolean isLoaded() { return state != State.NOT_LOADED; }

    public String fieldCode() {
        return fieldCode;
    }

    public DynamicDataType dataType() {
        return dataType;
    }

    public Object value() {
        return value;
    }

    public String stringValue() {
        return (String) value;
    }

    public Number numberValue() {
        return (Number) value;
    }

    public Boolean boolValue() {
        return (Boolean) value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynamicFieldValue that)) return false;
        return fieldCode.equals(that.fieldCode)
                && dataType == that.dataType
                && state == that.state
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldCode, dataType, value, state);
    }

    @Override
    public String toString() {
        return "DynamicFieldValue{" + fieldCode + '=' + value + " (" + dataType + ", " + state + ")}";
    }
}
