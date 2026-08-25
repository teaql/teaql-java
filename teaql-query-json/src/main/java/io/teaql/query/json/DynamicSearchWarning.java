package io.teaql.query.json;

import java.util.Objects;

/** A non-fatal compatibility warning produced while applying a dynamic search payload. */
public final class DynamicSearchWarning {

    public static final String UNKNOWN_FIELD = "DYNAMIC_SEARCH_UNKNOWN_FIELD";

    private final String code;
    private final String requestType;
    private final String clause;
    private final String fieldPath;

    public DynamicSearchWarning(
            String code, String requestType, String clause, String fieldPath) {
        this.code = Objects.requireNonNull(code);
        this.requestType = Objects.requireNonNull(requestType);
        this.clause = Objects.requireNonNull(clause);
        this.fieldPath = Objects.requireNonNull(fieldPath);
    }

    public String getCode() {
        return code;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getClause() {
        return clause;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    @Override
    public String toString() {
        return "DynamicSearchWarning{" +
                "code='" + code + '\'' +
                ", requestType='" + requestType + '\'' +
                ", clause='" + clause + '\'' +
                ", fieldPath='" + fieldPath + '\'' +
                '}';
    }
}
