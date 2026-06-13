package io.teaql.core;

import java.util.Objects;

/**
 * @author Jackytin constant expression
 */
public class Constant implements Expression {
    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object pValue) {
        value = pValue;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof Constant constant)) return false;
        return Objects.equals(getValue(), constant.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}
