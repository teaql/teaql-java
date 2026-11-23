package io.teaql.core.sql.internal;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.*;
import io.teaql.core.sql.*;
import io.teaql.core.sql.expression.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatisticalIntegerOperandTest {
    private static final class Custom implements Expression {
        @Override public List<String> properties(UserContext context){throw new AssertionError("Do not introspect trusted custom expressions");}
    }
    private SQLColumnResolver resolver() {
        Map<Class,SQLExpressionParser> parsers=new HashMap<>();
        parsers.put(PropertyReference.class,new PropertyParser());parsers.put(AggrExpression.class,new AggrExpressionParser());
        parsers.put(Parameter.class,new ParameterParser());parsers.put(FunctionApply.class,new FunctionApplyParser());
        parsers.put(Custom.class,new SQLExpressionParser<Custom>() {
            @Override public Class<Custom> type(){return Custom.class;}
            @Override public String toSql(UserContext c,Custom e,String table,Map<String,Object> parameters,SQLColumnResolver resolver){return "7";}
        });
        return new SQLColumnResolver() {
            @Override public List<SQLColumn> getPropertyColumns(String table,String property){throw new AssertionError("Typing must not render SQL");}
            @Override public Map<Class,SQLExpressionParser> getExpressionParsers(){return parsers;}
        };
    }
    private List<PropertyDescriptor> properties(Class<?> type) {
        var p=new PropertyDescriptor();p.setName("value");p.setType(new SimplePropertyType(type));return List.of(p);
    }
    private Expression self(Expression expression,int depth) {
        for(int i=0;i<depth;i++)expression=new AggrExpression(AggrFunction.SELF,expression);return expression;
    }
    @Test public void nestedSelfPreservesFieldTypesWithoutChangingTheTree() {
        for(Class<?> type:List.of(Long.class,Integer.class,Short.class,Byte.class,Boolean.class,Double.class,
                java.math.BigDecimal.class,String.class,java.time.LocalDate.class)) {
            for(int depth:List.of(0,1,2,64)) {
                Expression operand=self(new PropertyReference("value"),depth);
                Expression expected=self(new PropertyReference("value"),depth);
                if(Set.of(Long.class,Integer.class,Short.class,Byte.class).contains(type))
                    assertFalse(StatisticalIntegerOperand.parameter(properties(type),operand,resolver()));
                else assertTrue(assertThrows(TeaQLRuntimeException.class,()->StatisticalIntegerOperand.parameter(properties(type),operand,resolver()))
                        .getMessage().contains("requires an integer field"));
                assertEquals(expected,operand);
            }
        }
    }
    @Test public void scalarParametersPreserveIntegersAndRejectCoercionWithoutEchoingValues() {
        for(Long value:Arrays.asList(Long.MIN_VALUE,Long.MAX_VALUE,null)) {
            var parameter=new Parameter("input",value,Operator.EQUAL);
            assertTrue(StatisticalIntegerOperand.parameter(List.of(),self(parameter,2),resolver()));assertEquals(value,parameter.getValue());
        }
        for(Object value:List.of(1.0,true,java.math.BigDecimal.ONE,"PRIVATE-BITWISE-CANARY")) {
            var parameter=new Parameter("input",value,Operator.EQUAL);
            var error=assertThrows(TeaQLRuntimeException.class,()->StatisticalIntegerOperand.parameter(List.of(),parameter,resolver()));
            assertTrue(error.getMessage().contains("signed 64-bit integer operand"));assertFalse(error.getMessage().contains("CANARY"));
        }
        for(var parameter:List.of(new Parameter("input",1L,Operator.CONTAIN),new Parameter("input",new long[]{1,2},Operator.IN)))
            assertThrows(TeaQLRuntimeException.class,()->StatisticalIntegerOperand.parameter(List.of(),parameter,resolver()));
    }
    @Test public void malformedCyclesAndOversizedSelfFailBeforeRecursiveCompilation() {
        var empty=new AggrExpression(AggrFunction.SELF,new PropertyReference("value"));empty.getExpressions().clear();
        var many=new AggrExpression(AggrFunction.SELF,new PropertyReference("value"));many.getExpressions().add(new PropertyReference("other"));
        var cycle=new AggrExpression(AggrFunction.SELF,new PropertyReference("value"));cycle.getExpressions().set(0,cycle);
        for(var operand:List.of(empty,many,cycle,self(new PropertyReference("value"),66)))
            assertThrows(TeaQLRuntimeException.class,()->StatisticalIntegerOperand.parameter(properties(Long.class),operand,resolver()));
    }
    @Test public void registeredCustomParserKeepsItsTrustedSourceExtensionPath() {
        var resolver=resolver();var custom=new Custom();
        assertFalse(StatisticalIntegerOperand.parameter(List.of(),self(custom,2),resolver));
        assertEquals("7",ExpressionHelper.toSql(null,custom,"t",new HashMap<>(),resolver));
    }
}
