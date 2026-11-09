package io.teaql.runtime.memory;

import io.teaql.core.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Numeric aggregation for the existing memory provider, without SQL or I/O. */
final class MemoryAggregations {
    private MemoryAggregations() {}

    static AggregationResult calculate(Aggregations request, List<? extends Entity> rows) {
        if (request == null || request.getSelectedExpressions().isEmpty()) return null;
        // Validate before scanning rows: unsupported expressions cannot appear to
        // work simply because an empty input never visits an operand.
        for (var selected : request.getAggregates()) aggregate(selected.getExpression());
        for (var dimension : request.getDimensions()) dimension(dimension.getExpression());

        var groups = new LinkedHashMap<List<Object>, List<Entity>>();
        if (request.getDimensions().isEmpty()) groups.put(List.of(), new ArrayList<>(rows));
        else for (var row : rows) {
            // Stream.toList permits a real null dimension; List.copyOf does not.
            var key = request.getDimensions().stream().map(d -> value(dimension(d.getExpression()), row)).toList();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        var data = new ArrayList<AggregationItem>();
        groups.forEach((key, members) -> {
            var item = new AggregationItem();
            for (int i = 0; i < key.size(); i++) item.addDimension(request.getDimensions().get(i), key.get(i));
            for (var selected : request.getAggregates()) item.addValue(selected, evaluate(aggregate(selected.getExpression()), members));
            data.add(item);
        });
        var result = new AggregationResult(); result.setName(request.getName()); result.setData(data); return result;
    }

    private static AggrExpression aggregate(Expression expression) {
        if (!(expression instanceof AggrExpression aggregate) || aggregate.getExpressions().size() != 1)
            throw unsupported(expression);
        if (aggregate.getOperator() != AggrFunction.COUNT && aggregate.getOperator() != AggrFunction.SUM)
            throw unsupported(aggregate.getOperator());
        operand(aggregate.first());
        if (aggregate.getOperator() == AggrFunction.SUM && star(aggregate.first())) throw unsupported("SUM(*)");
        return aggregate;
    }

    private static Expression dimension(Expression expression) {
        if (expression instanceof AggrExpression aggregate) {
            if (aggregate.getOperator() != AggrFunction.SELF || aggregate.getExpressions().size() != 1)
                throw unsupported(aggregate.getOperator());
            expression = aggregate.first();
        }
        operand(expression);
        if (star(expression)) throw unsupported("group by *");
        return expression;
    }

    private static void operand(Expression expression) {
        if (!(expression instanceof PropertyReference) && !(expression instanceof Parameter)) throw unsupported(expression);
    }

    private static boolean star(Expression expression) {
        return expression instanceof PropertyReference property && "*".equals(property.getPropertyName());
    }

    private static Object value(Expression expression, Entity row) {
        Object value = expression instanceof PropertyReference property ? row.getProperty(property.getPropertyName()) : ((Parameter) expression).getValue();
        return value instanceof Entity entity ? entity.getId() : value;
    }

    private static Object evaluate(AggrExpression aggregate, List<Entity> rows) {
        if (aggregate.getOperator() == AggrFunction.COUNT) {
            if (star(aggregate.first())) return (long) rows.size();
            long count = 0; for (var row : rows) if (value(aggregate.first(), row) != null) count++; return count;
        }
        BigDecimal sum = null;
        for (var row : rows) {
            var value = value(aggregate.first(), row);
            if (value == null) continue;
            var term = decimal(value); sum = sum == null ? term : sum.add(term);
        }
        // A nullable exact number, not an empty-list default and not a string.
        return sum;
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof BigInteger integer) return new BigDecimal(integer);
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
            return BigDecimal.valueOf(((Number) value).longValue());
        if (value instanceof Float number && Float.isFinite(number)) return new BigDecimal(Float.toString(number));
        if (value instanceof Double number && Double.isFinite(number)) return BigDecimal.valueOf(number);
        throw new TeaQLRuntimeException("Memory SUM requires a finite numeric value, not " + value.getClass().getName());
    }

    private static TeaQLRuntimeException unsupported(Object expression) {
        return new TeaQLRuntimeException("Unsupported memory aggregate expression: " + expression);
    }
}
