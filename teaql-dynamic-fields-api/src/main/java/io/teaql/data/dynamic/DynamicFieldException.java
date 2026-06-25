package io.teaql.data.dynamic;

public class DynamicFieldException extends RuntimeException {

    private final String errorCode;

    public DynamicFieldException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }

    public static DynamicFieldException notFound(String fieldCode) {
        return new DynamicFieldException("DYNAMIC_FIELD_NOT_FOUND",
                "Dynamic field not found: " + fieldCode);
    }

    public static DynamicFieldException typeMismatch(String fieldCode, DynamicDataType expected, DynamicDataType actual) {
        return new DynamicFieldException("DYNAMIC_FIELD_TYPE_MISMATCH",
                "Type mismatch for field '" + fieldCode + "': expected " + expected + " but was " + actual);
    }

    public static DynamicFieldException notSelected(String fieldCode) {
        return new DynamicFieldException("DYNAMIC_FIELD_NOT_SELECTED",
                "Dynamic field not selected: " + fieldCode);
    }

    public static DynamicFieldException notVisible(String fieldCode) {
        return new DynamicFieldException("DYNAMIC_FIELD_NOT_VISIBLE",
                "Dynamic field not visible: " + fieldCode);
    }

    public static DynamicFieldException notEditable(String fieldCode) {
        return new DynamicFieldException("DYNAMIC_FIELD_NOT_EDITABLE",
                "Dynamic field not editable: " + fieldCode);
    }

    public static DynamicFieldException ownerTypeMismatch(String expected, String actual) {
        return new DynamicFieldException("DYNAMIC_FIELD_OWNER_TYPE_MISMATCH",
                "Owner type mismatch: expected '" + expected + "' but was '" + actual + "'");
    }

    public static DynamicFieldException scopeNotConfigured(String scopeType) {
        return new DynamicFieldException("DYNAMIC_FIELD_SCOPE_NOT_CONFIGURED",
                "Dynamic field scope not configured: " + scopeType);
    }

    public static DynamicFieldException providerUnsupported(String operation) {
        return new DynamicFieldException("DYNAMIC_FIELD_PROVIDER_UNSUPPORTED",
                "Provider does not support operation: " + operation);
    }

    public static DynamicFieldException intentRequired() {
        return new DynamicFieldException("DYNAMIC_FIELD_INTENT_REQUIRED",
                "Strict intent mode requires purpose and comment");
    }
}
