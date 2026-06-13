package io.teaql.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class AggregationItem {
    private Map<SimpleNamedExpression, Object> dimensions = new LinkedHashMap<>();
    private Map<SimpleNamedExpression, Object> values = new LinkedHashMap<>();

    public Map<SimpleNamedExpression, Object> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Map<SimpleNamedExpression, Object> pDimensions) {
        dimensions = pDimensions;
    }

    public Map<SimpleNamedExpression, Object> getValues() {
        return values;
    }

    public void setValues(Map<SimpleNamedExpression, Object> pValues) {
        values = pValues;
    }

    public void addValue(SimpleNamedExpression aggregation, Object value) {
        values.put(aggregation, value);
    }

    public void addDimension(SimpleNamedExpression dimension, Object value) {
        dimensions.put(dimension, value);
    }
}
