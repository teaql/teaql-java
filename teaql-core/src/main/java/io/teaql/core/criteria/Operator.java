package io.teaql.core.criteria;

import io.teaql.core.PropertyFunction;

public enum Operator implements PropertyFunction {
    EQUAL,
    NOT_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    END_WITH,
    NOT_END_WITH,
    BEGIN_WITH,
    NOT_BEGIN_WITH,
    CONTAIN,
    NOT_CONTAIN,
    IS_NOT_NULL,
    IS_NULL,
    IN,
    NOT_IN,
    IN_LARGE,
    NOT_IN_LARGE,
    BETWEEN,
    SOUNDS_LIKE;

    static final String IS_NULL_EXPR = "__is_null__";
    static final String IS_NOT_NULL_EXPR = "__is_not_null__";

    public static Operator operatorByValue(String value) {

        if (IS_NULL_EXPR.equalsIgnoreCase(value)) {
            return IS_NULL;
        }
        if (IS_NOT_NULL_EXPR.equalsIgnoreCase(value)) {
            return IS_NOT_NULL;
        }
        return null;
    }

    public boolean hasOneOperator() {
        return this == IS_NULL || this == IS_NOT_NULL;
    }

    public boolean hasTwoOperator() {
        return this != IS_NULL && this != IS_NOT_NULL && this != BETWEEN;
    }

    public boolean hasMultiValue() {
        return this == IN || this == NOT_IN || this == IN_LARGE || this == NOT_IN_LARGE;
    }

    public boolean isBetween() {
        return this == BETWEEN;
    }
}
