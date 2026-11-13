package io.teaql.core.sql.internal;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.PropertyDescriptor;
import java.util.*;

/** Framework-owned scalar grouping validation and defensive AST copy. */
public final class StatisticalGroupingExpression {
    private StatisticalGroupingExpression() {}

    public static Expression copy(List<PropertyDescriptor> properties, Expression expression) {
        return copy(properties, expression, true, new IdentityHashMap<>(), 0);
    }

    /** Preserve ordinary SQL's existing provider-owned GBK transformation. */
    public static Expression copyOrdinary(List<PropertyDescriptor> properties, Expression expression) {
        return copy(properties, expression, true, new IdentityHashMap<>(), 0);
    }

    private static Expression copy(List<PropertyDescriptor> properties, Expression expression,
            boolean allowGbk, IdentityHashMap<Expression, Boolean> active, int depth) {
        if (expression == null || depth > 64 || active.put(expression, true) != null)
            throw invalid("Cyclic or oversized statistical grouping expression");
        try {
            if (expression.getClass() == PropertyReference.class) {
                String name = ((PropertyReference) expression).getPropertyName();
                property(properties, name);
                return new PropertyReference(name);
            }
            if (expression instanceof FunctionApply function && function.getExpressions().size() == 1) {
                boolean identity = expression.getClass() == AggrExpression.class && function.getOperator() == AggrFunction.SELF;
                boolean soundex = expression.getClass() == FunctionApply.class && function.getOperator() == Operator.SOUNDS_LIKE;
                boolean gbk = allowGbk && expression.getClass() == AggrExpression.class && function.getOperator() == AggrFunction.GBK;
                if (identity || soundex || gbk) {
                    Expression argument = copy(properties, function.first(), allowGbk, active, depth + 1);
                    var type = resultType(properties, argument);
                    if ((soundex || gbk) && type != String.class)
                        throw invalid("Text grouping transform requires a text field");
                    if (gbk) return new AggrExpression(AggrFunction.GBK, argument);
                    return identity ? new AggrExpression(AggrFunction.SELF, argument)
                            : new FunctionApply(Operator.SOUNDS_LIKE, argument);
                }
            }
            throw invalid("Unsupported statistical grouping expression; a scalar field transform is required");
        } finally { active.remove(expression); }
    }

    private static Class<?> resultType(List<PropertyDescriptor> properties, Expression expression) {
        if (expression instanceof PropertyReference reference) {
            var type = property(properties, reference.getPropertyName()).getType();
            return type == null ? null : type.javaType();
        }
        var function = (FunctionApply) expression;
        if (function.getOperator() == AggrFunction.GBK) return byte[].class;
        if (function.getOperator() == Operator.SOUNDS_LIKE) return String.class;
        return resultType(properties, function.first());
    }

    private static PropertyDescriptor property(List<PropertyDescriptor> properties, String name) {
        return properties.stream().filter(property -> Objects.equals(name, property.getName())).findFirst()
                .orElseThrow(() -> invalid("Unknown logical grouping field: " + name));
    }

    private static TeaQLRuntimeException invalid(String message) { return new TeaQLRuntimeException(message); }
}
