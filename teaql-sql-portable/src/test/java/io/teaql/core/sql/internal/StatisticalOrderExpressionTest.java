package io.teaql.core.sql.internal;

import io.teaql.core.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatisticalOrderExpressionTest {
    @Test public void gbkIsASortModifierOverAnUnchangedSelectedKey() {
        var field = new PropertyReference("caption");
        var sort = StatisticalOrderExpression.parse(new AggrExpression(AggrFunction.GBK,
                new AggrExpression(AggrFunction.SELF, field)));
        assertTrue(sort.gbk()); assertEquals(field,sort.key()); assertTrue(sort.matches(field));
        assertTrue(sort.matches(new AggrExpression(AggrFunction.SELF,field)));
        assertFalse(sort.matches(new PropertyReference("unselected")));
    }
    @Test public void cyclesAndExcessiveSelfDepthFailWithoutInspectingOperands() {
        var cycle=new AggrExpression(AggrFunction.SELF,new PropertyReference("PRIVATE-GBK-CANARY"));
        cycle.getExpressions().set(0,cycle);
        Expression deep=new PropertyReference("PRIVATE-GBK-CANARY");
        for(int i=0;i<70;i++)deep=new AggrExpression(AggrFunction.SELF,deep);
        Expression[] values={cycle,deep,new AggrExpression(AggrFunction.GBK,cycle)};
        for(var value:values){
            var error=assertThrows(TeaQLRuntimeException.class,()->StatisticalOrderExpression.parse(value));
            assertTrue(error.getMessage().contains("depth"));assertFalse(error.getMessage().contains("CANARY"));
        }
    }
    @Test public void invalidArityAndMissingExpressionsHaveValueFreeDiagnostics() {
        var bad=new AggrExpression(AggrFunction.GBK,new PropertyReference("PRIVATE-GBK-CANARY"));
        bad.getExpressions().add(new PropertyReference("another"));
        var error=assertThrows(TeaQLRuntimeException.class,()->StatisticalOrderExpression.parse(bad));
        assertTrue(error.getMessage().contains("exactly one"));assertFalse(error.getMessage().contains("CANARY"));
        assertThrows(TeaQLRuntimeException.class,()->StatisticalOrderExpression.parse(null));
    }
}
