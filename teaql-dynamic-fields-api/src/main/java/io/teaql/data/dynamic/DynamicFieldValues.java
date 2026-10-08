package io.teaql.data.dynamic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DynamicFieldValues {

    private final Map<String, DynamicFieldValue> values;
    private final DynamicFieldMetadata metadata;
    private final java.util.Set<String> selectedCodes;

    public DynamicFieldValues(Map<String, DynamicFieldValue> values) {
        this(inferMetadata(values), values);
    }

    public DynamicFieldValues(DynamicFieldMetadata metadata, Map<String, DynamicFieldValue> values) {
        this.metadata = java.util.Objects.requireNonNull(metadata, "metadata");
        this.values = values.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        for (Map.Entry<String, DynamicFieldValue> entry : this.values.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().fieldCode())) throw new IllegalArgumentException("Dynamic field key/code mismatch");
            DynamicDataType declared = metadata.requireType(entry.getKey());
            if (declared != entry.getValue().dataType()) throw DynamicFieldException.typeMismatch(entry.getKey(), declared, entry.getValue().dataType());
        }
        // Presence is a view of the private payload map, not a copied field-name set per row.
        if (this.values.values().stream().allMatch(DynamicFieldValue::isLoaded)) {
            selectedCodes = this.values.keySet();
        } else {
            selectedCodes = Collections.unmodifiableSet(new java.util.AbstractSet<>() {
                @Override public java.util.Iterator<String> iterator() {
                    return DynamicFieldValues.this.values.entrySet().stream()
                            .filter(entry -> entry.getValue().isLoaded()).map(Map.Entry::getKey).iterator();
                }
                @Override public int size() {
                    return (int) DynamicFieldValues.this.values.values().stream().filter(DynamicFieldValue::isLoaded).count();
                }
                @Override public boolean contains(Object code) {
                    DynamicFieldValue value = DynamicFieldValues.this.values.get(code);
                    return value != null && value.isLoaded();
                }
            });
        }
    }

    public static DynamicFieldValues empty() {
        return new DynamicFieldValues(Collections.emptyMap());
    }

    public static DynamicFieldValues of(List<DynamicFieldValue> values) {
        Map<String, DynamicFieldValue> map = new LinkedHashMap<>();
        for (DynamicFieldValue v : values) {
            map.put(v.fieldCode(), v);
        }
        return new DynamicFieldValues(map);
    }

    public String getString(String fieldCode) {
        return requireSelected(fieldCode).stringValue();
    }

    public Number getNumber(String fieldCode) {
        return requireSelected(fieldCode).numberValue();
    }

    public Boolean getBool(String fieldCode) {
        return requireSelected(fieldCode).boolValue();
    }

    public Object get(String fieldCode) {
        return requireSelected(fieldCode).value();
    }

    public boolean isSelected(String fieldCode) {
        DynamicFieldValue value = values.get(fieldCode);
        return value != null && value.isLoaded();
    }

    public boolean isNull(String fieldCode) {
        DynamicFieldValue v = values.get(fieldCode);
        return v != null && v.state() == DynamicFieldValue.State.NULL;
    }

    /** Unlike strict typed getters, this wrapper exposes NotLoaded without I/O or an invented NULL. */
    public DynamicFieldValue field(String fieldCode) {
        DynamicFieldValue value = values.get(fieldCode);
        return value == null ? metadata.notLoaded(fieldCode) : value;
    }

    public DynamicFieldMetadata metadata() { return metadata; }

    /** Immutable loaded-code view; NULL counts as loaded, NotLoaded does not. */
    public java.util.Set<String> selectedCodes() { return selectedCodes; }

    public Map<String, DynamicFieldValue> toMap() {
        return values;
    }

    public int size() {
        return values.size();
    }

    private DynamicFieldValue requireSelected(String fieldCode) {
        DynamicFieldValue v = values.get(fieldCode);
        if (v == null || !v.isLoaded()) {
            throw DynamicFieldException.notSelected(fieldCode);
        }
        return v;
    }

    private static DynamicFieldMetadata inferMetadata(Map<String, DynamicFieldValue> values) {
        Map<String, DynamicDataType> types = new LinkedHashMap<>();
        values.forEach((code, value) -> types.put(code, value.dataType()));
        return new DynamicFieldMetadata(types);
    }
}
