package io.teaql.core.sql.internal;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.sql.SQLColumnResolver;
import io.teaql.core.sql.expression.ExpressionHelper;
import java.util.*;

/** Bounded, value-free typing for built-in bitwise operands; no AST mutation. */
public final class StatisticalIntegerOperand {
    private StatisticalIntegerOperand() {}
    public static boolean bitwise(AggrFunction function) {
        return function==AggrFunction.BIT_AND || function==AggrFunction.BIT_OR || function==AggrFunction.BIT_XOR;
    }
    /** True means a scalar parameter, which an overloaded provider may need to type. */
    public static boolean parameter(List<PropertyDescriptor> properties,Expression operand,SQLColumnResolver resolver) {
        for(int depth=0;depth<=64;depth++) {
            if(operand==null)throw invalid("bitwise expression requires a signed 64-bit integer operand");
            if(operand instanceof PropertyReference reference) {
                var property=properties.stream().filter(p->Objects.equals(p.getName(),reference.getPropertyName())).findFirst().orElse(null);
                Class<?> type=property==null||property.getType()==null?null:property.getType().javaType();
                if(type==null || !Set.of(Byte.class,Short.class,Integer.class,Long.class,
                        byte.class,short.class,int.class,long.class).contains(type))
                    throw invalid("bitwise aggregate requires an integer field");
                return false;
            }
            // A registered application parser owns the semantics of its custom
            // expression. Preserve that trusted SPI instead of guessing its type.
            if(!ExpressionHelper.hasBuiltinParser(operand,resolver))return false;
            if(operand instanceof AggrExpression self && self.getOperator()==AggrFunction.SELF) {
                if(self.getExpressions().size()!=1)throw invalid("bitwise SELF expects one operand");
                operand=self.first();continue;
            }
            if(operand instanceof Parameter parameter) {
                Operator operator=parameter.getOperator();
                if(operator!=null && (operator.hasMultiValue() || Set.of(Operator.CONTAIN,Operator.NOT_CONTAIN,
                        Operator.BEGIN_WITH,Operator.NOT_BEGIN_WITH,Operator.END_WITH,Operator.NOT_END_WITH).contains(operator)))
                    throw invalid("bitwise expression requires a signed 64-bit integer operand");
                Object value=parameter.getValue();
                if(value==null || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)return true;
            }
            throw invalid("bitwise expression requires a signed 64-bit integer operand");
        }
        throw invalid("bitwise operand exceeds 64 wrappers");
    }
    private static TeaQLRuntimeException invalid(String message) {return new TeaQLRuntimeException(message);}
}
