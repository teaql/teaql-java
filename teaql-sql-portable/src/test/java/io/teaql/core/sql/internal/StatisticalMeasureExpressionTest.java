package io.teaql.core.sql.internal;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatisticalMeasureExpressionTest {
    private static List<PropertyDescriptor> properties(){
        var number=new PropertyDescriptor();number.setName("id");number.setType(new SimplePropertyType(Long.class));
        var text=new PropertyDescriptor();text.setName("name");text.setType(new SimplePropertyType(String.class));
        var decimal=new PropertyDescriptor();decimal.setName("amount");decimal.setType(new SimplePropertyType(BigDecimal.class));
        return List.of(number,text,decimal);
    }
    @Test public void allAggregateFunctionsAcceptIntegerOwnerInputs(){
        for(var function:AggrFunction.values())if(function!=AggrFunction.SELF&&function!=AggrFunction.GBK){
            var expression=new AggrExpression(AggrFunction.SELF,new PropertyReference("id"));
            var input=StatisticalMeasureExpression.bind(properties(),expression);
            StatisticalMeasureExpression.validate(function,input);
            assertSame(expression,input.expression());assertEquals(Long.class,input.type());assertEquals(Set.of("id"),input.fields());
        }
    }
    @Test public void scalarParametersPreserveOriginalValuesAndExactDomains(){
        for(Object value:Arrays.asList(Long.MIN_VALUE,Long.MAX_VALUE,new BigDecimal("9007199254740992.000000001"),"text",true,null)){
            var parameter=new Parameter("value",value,Operator.EQUAL);
            var input=StatisticalMeasureExpression.bind(properties(),parameter);
            assertSame(value,parameter.getValue());assertSame(parameter,input.expression());
            assertEquals(value==null?Long.class:value.getClass(),input.type());assertTrue(input.fields().isEmpty());
        }
    }
    @Test public void soundexAndSelfKeepLogicalFieldsAndOutputTypes(){
        var expression=new FunctionApply(Operator.SOUNDS_LIKE,new AggrExpression(AggrFunction.SELF,new PropertyReference("name")));
        var input=StatisticalMeasureExpression.bind(properties(),expression);
        assertEquals(String.class,input.type());assertEquals(Set.of("name"),input.fields());assertNull(input.directField());
        var decimal=StatisticalMeasureExpression.bind(properties(),new AggrExpression(AggrFunction.SELF,new PropertyReference("amount")));
        assertEquals(BigDecimal.class,decimal.type());
    }
    @Test public void nestedAggregatesCyclesAndMalformedScalarsFailBeforeDiscovery(){
        var cycle=new AggrExpression(AggrFunction.SELF,new PropertyReference("id"));cycle.getExpressions().clear();cycle.getExpressions().add(cycle);
        var empty=new AggrExpression(AggrFunction.SELF,new PropertyReference("id"));empty.getExpressions().clear();
        for(Expression expression:List.of(cycle,new AggrExpression(AggrFunction.SUM,new PropertyReference("id")),
                empty,new FunctionApply(Operator.SOUNDS_LIKE,new PropertyReference("id")),
                new Parameter("value",List.of("SECRET-CANARY"),Operator.IN),new Parameter("value",Double.NaN,Operator.EQUAL))){
            var error=assertThrows(TeaQLRuntimeException.class,()->StatisticalMeasureExpression.bind(properties(),expression));
            assertFalse(error.getMessage().contains("CANARY"));
        }
        cycle.getExpressions().clear();
    }
    @Test public void implicitTypeCoercionAndPhysicalNamesAreRejected(){
        var text=StatisticalMeasureExpression.bind(properties(),new Parameter("value","SECRET-CANARY",Operator.EQUAL));
        for(var function:List.of(AggrFunction.SUM,AggrFunction.AVG,AggrFunction.BIT_XOR))
            assertThrows(TeaQLRuntimeException.class,()->StatisticalMeasureExpression.validate(function,text));
        assertThrows(TeaQLRuntimeException.class,()->StatisticalMeasureExpression.bind(properties(),new PropertyReference("Legacy Amount")));
        Expression deep=new PropertyReference("id");for(int i=0;i<66;i++)deep=new AggrExpression(AggrFunction.SELF,deep);
        Expression oversized=deep;assertThrows(TeaQLRuntimeException.class,()->StatisticalMeasureExpression.bind(properties(),oversized));
    }
}
