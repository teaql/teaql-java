package io.teaql.core.sql.internal;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Metadata-only scalar binding before property discovery, SQL or parameter binding. */
public final class StatisticalMeasureExpression {
    private StatisticalMeasureExpression() {}
    public record Input(Expression expression, Class<?> type, Set<String> fields, String directField) {}
    public static Input bind(List<PropertyDescriptor> properties, Expression expression) {
        return bind(properties,expression,new IdentityHashMap<>(),0);
    }
    private static Input bind(List<PropertyDescriptor> properties, Expression expression,
            IdentityHashMap<Expression,Boolean> active,int depth) {
        if(expression==null || depth>64 || active.put(expression,true)!=null)
            throw invalid("Cyclic or oversized statistical measure operand");
        try {
            if(expression instanceof PropertyReference field) {
                var property=properties.stream().filter(p->Objects.equals(p.getName(),field.getPropertyName()))
                        .findFirst().orElseThrow(()->invalid("Unknown logical statistical operand field"));
                if(property.getType()==null||property.getType().javaType()==null)throw invalid("Statistical operand has no modeled type");
                return new Input(expression,property.getType().javaType(),Set.of(field.getPropertyName()),field.getPropertyName());
            }
            if(expression.getClass()==AggrExpression.class && ((AggrExpression)expression).getOperator()==AggrFunction.SELF) {
                var function=(AggrExpression)expression;
                if(function.getExpressions().size()!=1)throw invalid("Statistical SELF requires one operand");
                var child=bind(properties,function.first(),active,depth+1);
                return new Input(expression,child.type,child.fields,null);
            }
            if(expression.getClass()==FunctionApply.class && ((FunctionApply)expression).getOperator()==Operator.SOUNDS_LIKE) {
                var function=(FunctionApply)expression;
                if(function.getExpressions().size()!=1)throw invalid("Statistical SOUNDEX requires one operand");
                var child=bind(properties,function.first(),active,depth+1);
                if(child.type!=String.class)throw invalid("SOUNDEX measure operand requires text");
                return new Input(expression,String.class,child.fields,null);
            }
            if(expression.getClass()==Parameter.class) {
                var parameter=(Parameter)expression;
                if(parameter.getOperator()!=null && parameter.getOperator()!=Operator.EQUAL)
                    throw invalid("Statistical measure requires a scalar parameter without filter transformations");
                Object value=parameter.getValue();Class<?> type=value==null?Long.class:value.getClass();
                if(value instanceof Double d && !Double.isFinite(d)
                        || value instanceof Float f && !Float.isFinite(f))
                    throw invalid("Statistical measure requires a finite scalar parameter");
                if(!Set.of(Byte.class,Short.class,Integer.class,Long.class,Float.class,Double.class,BigDecimal.class,
                        String.class,Boolean.class,LocalDate.class,LocalDateTime.class,Instant.class).contains(type))
                    throw invalid("Statistical measure requires a supported scalar parameter");
                return new Input(expression,type,Set.of(),null);
            }
            throw invalid("Statistical measure requires a scalar operand, not a nested aggregate or subquery");
        } finally {active.remove(expression);}
    }
    public static void validate(AggrFunction function,Input input) {
        Class<?> type=input.type;
        boolean integer=Set.of(Byte.class,Short.class,Integer.class,Long.class,byte.class,short.class,int.class,long.class).contains(type);
        boolean numeric=integer||Set.of(Float.class,Double.class,BigDecimal.class,float.class,double.class).contains(type);
        if(Set.of(AggrFunction.SUM,AggrFunction.AVG,AggrFunction.STDDEV,AggrFunction.STDDEV_POP,
                AggrFunction.VAR_SAMP,AggrFunction.VAR_POP).contains(function) && !numeric)
            throw invalid("Numeric aggregate requires a numeric scalar operand");
        if(StatisticalIntegerOperand.bitwise(function) && !integer)
            throw invalid("bitwise aggregate requires an integer field or scalar operand");
        if(Set.of(AggrFunction.MIN,AggrFunction.MAX).contains(function) && (type==Boolean.class||type==boolean.class
                ||Map.class.isAssignableFrom(type)||Collection.class.isAssignableFrom(type)))
            throw invalid("Ordered aggregate requires an ordered scalar operand");
        if(function==AggrFunction.SELF||function==AggrFunction.GBK)
            throw invalid("Statistical measure requires an aggregate function");
    }
    private static TeaQLRuntimeException invalid(String message){return new TeaQLRuntimeException(message);}
}
