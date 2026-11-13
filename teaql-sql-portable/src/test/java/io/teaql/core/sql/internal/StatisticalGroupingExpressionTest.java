package io.teaql.core.sql.internal;

import io.teaql.core.*;
import io.teaql.core.criteria.Operator;
import io.teaql.core.meta.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatisticalGroupingExpressionTest {
    private List<PropertyDescriptor> properties() {
        var name=new PropertyDescriptor();name.setName("name");name.setType(new SimplePropertyType(String.class));
        return List.of(name);
    }
    @Test public void copiedGroupingTreeDoesNotShareMutableFunctionArguments() {
        var original=new FunctionApply(Operator.SOUNDS_LIKE,new PropertyReference("name"));
        var copy=StatisticalGroupingExpression.copy(properties(),original);
        original.getExpressions().set(0,new PropertyReference("missing"));
        assertNotSame(original,copy);assertEquals(List.of("name"),copy.properties(null));
    }
    @Test public void cyclicDeepAggregateAndLiteralGroupingExpressionsFailClosed() {
        var cycle=new FunctionApply(Operator.SOUNDS_LIKE,new PropertyReference("name"));cycle.getExpressions().set(0,cycle);
        Expression deep=new PropertyReference("name");for(int i=0;i<70;i++)deep=new FunctionApply(Operator.SOUNDS_LIKE,deep);
        for(var expression:List.of(cycle,deep,new AggrExpression(AggrFunction.SUM,new PropertyReference("name"))))
            assertThrows(TeaQLRuntimeException.class,()->StatisticalGroupingExpression.copy(properties(),expression));
    }
    @Test public void ordinaryAndLayeredGbkGroupingBothRetainTheBinaryTransform() {
        var original = new AggrExpression(AggrFunction.GBK, new PropertyReference("name"));
        var copied = StatisticalGroupingExpression.copyOrdinary(properties(), original);
        assertNotSame(original, copied);
        assertEquals(AggrFunction.GBK, ((AggrExpression) copied).getOperator());
        assertEquals(List.of("name"), copied.properties(null));
        var layered = StatisticalGroupingExpression.copy(properties(), original);
        assertEquals(AggrFunction.GBK, ((AggrExpression) layered).getOperator());
        assertNotSame(original, layered);
    }
    @Test public void binaryTransformCannotBeConsumedAsTextByAnOuterTransform() {
        var encoded = new AggrExpression(AggrFunction.GBK, new PropertyReference("name"));
        for (var expression : List.of(new AggrExpression(AggrFunction.GBK, encoded),
                new FunctionApply(Operator.SOUNDS_LIKE, encoded))) {
            assertThrows(TeaQLRuntimeException.class, () -> StatisticalGroupingExpression.copy(properties(), expression));
        }
    }
}
