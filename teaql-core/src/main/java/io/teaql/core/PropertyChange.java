package io.teaql.core;

public final class PropertyChange {
    private final String propertyName;
    private final Object oldValue;
    private final Object newValue;

    public PropertyChange(String propertyName, Object oldValue, Object newValue) {
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
