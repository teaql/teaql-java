package io.teaql.core;

import java.util.Map;
import java.util.Objects;

/** Immutable readonly property names/types; no values, presence markers or fixed slots. */
public final class DynamicPropertyMetadata {
    private final Map<String, Class<?>> types;
    private final int hash;

    public DynamicPropertyMetadata(Map<String, Class<?>> types) {
        for (Map.Entry<String, Class<?>> entry : types.entrySet()) {
            String name = entry.getKey();
            if (name == null || !name.startsWith("_") || name.substring(1).isBlank()) {
                throw new IllegalArgumentException("Dynamic property metadata requires '_' names");
            }
            Class<?> type = Objects.requireNonNull(entry.getValue(), "dynamic property type");
            if (type.isPrimitive()) {
                throw new IllegalArgumentException("Dynamic property metadata requires boxed types");
            }
        }
        this.types = Map.copyOf(types);
        this.hash = this.types.hashCode();
    }

    public Map<String, Class<?>> types() { return types; }
    public Class<?> type(String name) { return types.get(name); }

    /** Validation never prints the value; absence is not validated as a NULL assignment. */
    public void validate(String name, Object value) {
        Class<?> type = type(name);
        if (type != null && value != null && !type.isInstance(value)) {
            throw new IllegalArgumentException("Invalid dynamic property type: " + name);
        }
    }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof DynamicPropertyMetadata metadata && types.equals(metadata.types);
    }
    @Override public int hashCode() { return hash; }
}
