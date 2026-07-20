package io.teaql.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.Convert;
import io.teaql.core.utils.ObjectUtil;

public class AggregationResult {
    private String name;
    private List<AggregationItem> data;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public List<AggregationItem> getData() {
        return data;
    }

    public void setData(List<AggregationItem> pData) {
        data = pData;
    }

    public List getPropagateDimensionValues(String propertyName) {
        return data.stream()
                .map(
                        d -> {
                            Map<SimpleNamedExpression, Object> dimensions = d.getDimensions();
                            for (Map.Entry<SimpleNamedExpression, Object> entry : dimensions.entrySet()) {
                                SimpleNamedExpression dimension = entry.getKey();
                                Object value = entry.getValue();
                                if (dimension.name().equals(propertyName)) {
                                    return value;
                                }
                            }
                            return null;
                        })
                .filter(o -> o != null)
                .collect(Collectors.toList());
    }

    public Number toNumber(Number defaultValue) {
        AggregationItem first = CollectionUtil.getFirst(data);
        if (first == null) {
            return defaultValue;
        }
        Map<SimpleNamedExpression, Object> values = first.getValues();
        if (ObjectUtil.isEmpty(values)) {
            return defaultValue;
        }

        Object firstValue = CollectionUtil.getFirst(values.values());
        if (ObjectUtil.isEmpty(firstValue)) {
            return defaultValue;
        }

        if (firstValue instanceof Number) {
            return (Number) firstValue;
        }

        return Convert.convert(Number.class, firstValue);
    }

    public int toInt() {
        return toNumber(0).intValue();
    }

    public Map<Object, Number> toSimpleMap() {
        Map<Object, Number> ret = new HashMap<>();
        for (AggregationItem datum : data) {
            Map<SimpleNamedExpression, Object> values = datum.getValues();
            Map<SimpleNamedExpression, Object> dimensions = datum.getDimensions();

            if (ObjectUtil.isEmpty(dimensions)) {
                continue;
            }

            if (ObjectUtil.isEmpty(values)) {
                continue;
            }

            Object firstValue = CollectionUtil.getFirst(values.values());
            Object firstDimension = CollectionUtil.getFirst(dimensions.values());
            if (firstDimension == null) {
                continue;
            }

            Number value = Convert.convert(Number.class, firstValue);
            ret.put(firstDimension, value);
        }
        return ret;
    }

    public List<Map<String, Object>> valueList() {
        return data.stream()
                .map(
                        item -> {
                            Map<String, Object> m = new HashMap();
                            item.getValues()
                                    .forEach(
                                            (k, v) -> {
                                                m.put(k.name(), v);
                                            });

                            return m;
                        })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> toList() {
        return data.stream()
                .map(
                        item -> {
                            Map<String, Object> m = new HashMap();
                            item.getValues()
                                    .forEach(
                                            (k, v) -> {
                                                m.put(k.name(), v);
                                            });
                            item.getDimensions()
                                    .forEach(
                                            (k, v) -> {
                                                m.put(k.name(), v);
                                            });
                            return m;
                        })
                .collect(Collectors.toList());
    }
}
