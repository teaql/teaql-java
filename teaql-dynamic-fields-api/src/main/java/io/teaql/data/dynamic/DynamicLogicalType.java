package io.teaql.data.dynamic;

public enum DynamicLogicalType {
    PLAIN_TEXT(DynamicDataType.STRING),
    EMAIL(DynamicDataType.STRING),
    PHONE(DynamicDataType.STRING),
    URL(DynamicDataType.STRING),
    CURRENCY(DynamicDataType.NUMBER),
    PERCENTAGE(DynamicDataType.NUMBER),
    TAG(DynamicDataType.STRING),
    COLOR(DynamicDataType.STRING),
    RICH_TEXT(DynamicDataType.STRING);

    private final DynamicDataType baseType;

    DynamicLogicalType(DynamicDataType baseType) {
        this.baseType = baseType;
    }

    public DynamicDataType baseType() {
        return baseType;
    }
}
