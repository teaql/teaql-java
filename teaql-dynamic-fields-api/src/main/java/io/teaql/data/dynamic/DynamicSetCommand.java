package io.teaql.data.dynamic;

import java.util.Objects;

public final class DynamicSetCommand {

    private final DynamicOwnerRef ownerRef;
    private final String fieldCode;
    private final DynamicDataType dataType;
    private final Object value;
    private final String purpose;
    private final String comment;

    private DynamicSetCommand(DynamicOwnerRef ownerRef, String fieldCode, DynamicDataType dataType,
                              Object value, String purpose, String comment) {
        this.ownerRef = Objects.requireNonNull(ownerRef, "ownerRef");
        this.fieldCode = Objects.requireNonNull(fieldCode, "fieldCode");
        this.dataType = Objects.requireNonNull(dataType, "dataType");
        this.value = value;
        this.purpose = purpose;
        this.comment = comment;
    }

    public static DynamicSetCommand of(DynamicOwnerRef ownerRef, String fieldCode,
                                       DynamicDataType dataType, Object value,
                                       String purpose, String comment) {
        return new DynamicSetCommand(ownerRef, fieldCode, dataType, value, purpose, comment);
    }

    public DynamicOwnerRef ownerRef() { return ownerRef; }
    public String fieldCode() { return fieldCode; }
    public DynamicDataType dataType() { return dataType; }
    public Object value() { return value; }
    public String purpose() { return purpose; }
    public String comment() { return comment; }

    @Override
    public String toString() {
        return "DynamicSetCommand{" + ownerRef + '/' + fieldCode + '=' + value + '}';
    }
}
