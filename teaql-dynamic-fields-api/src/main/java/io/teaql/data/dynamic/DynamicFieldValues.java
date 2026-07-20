package io.teaql.data.dynamic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DynamicFieldValues {

    private final Map<String, DynamicFieldValue> values;

    public DynamicFieldValues(Map<String, DynamicFieldValue> values) {
        this.values = new LinkedHashMap<>(values);
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
        return values.containsKey(fieldCode);
    }

    public boolean isNull(String fieldCode) {
        DynamicFieldValue v = values.get(fieldCode);
        return v != null && v.value() == null;
    }

    public Map<String, DynamicFieldValue> toMap() {
        return Collections.unmodifiableMap(values);
    }

    public int size() {
        return values.size();
    }

    private DynamicFieldValue requireSelected(String fieldCode) {
        DynamicFieldValue v = values.get(fieldCode);
        if (v == null) {
            throw DynamicFieldException.notSelected(fieldCode);
        }
        return v;
    }
}
