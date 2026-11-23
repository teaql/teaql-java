package io.teaql.core;

public final class PropertyChange {
    private final String propertyName;
    private final Object oldValue;
    private final Object newValue;
    private final boolean oldValueLoaded;

    public PropertyChange(String propertyName, Object oldValue, Object newValue) {
        this(propertyName, oldValue, newValue, true);
    }

    public PropertyChange(String propertyName, Object oldValue, Object newValue, boolean oldValueLoaded) {
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldValueLoaded = oldValueLoaded;
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

    public boolean wasOldValueLoaded() {
        return oldValueLoaded;
    }
}
