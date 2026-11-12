package io.teaql.core.sql.expression;

import io.teaql.core.AggrFunction;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AggrExpressionParserTest {
    @Test public void distinctCountKeepsItsOwnOperatorAndRejectsAllRows() {
        var parser = new AggrExpressionParser();
        assertEquals("count(DISTINCT measure)", parser.genAggrSQL(AggrFunction.COUNT_DISTINCT, "measure"));
        org.junit.Assert.assertThrows(io.teaql.core.TeaQLRuntimeException.class,
                () -> parser.genAggrSQL(AggrFunction.COUNT_DISTINCT, "*"));
    }
    @Test public void bitwiseOperatorsUseTheSameCompilerPathAsOtherMeasures() {
        var parser = new AggrExpressionParser();
        assertEquals("bit_and(measure)", parser.genAggrSQL(AggrFunction.BIT_AND, "measure"));
        assertEquals("bit_or(measure)", parser.genAggrSQL(AggrFunction.BIT_OR, "measure"));
        assertEquals("bit_xor(measure)", parser.genAggrSQL(AggrFunction.BIT_XOR, "measure"));
    }

    @Test public void varianceAndStandardDeviationRetainTheirPopulationAndSampleMeanings() {
        var parser = new AggrExpressionParser();
        assertEquals("stddev(measure)", parser.genAggrSQL(AggrFunction.STDDEV, "measure"));
        assertEquals("stddev_pop(measure)", parser.genAggrSQL(AggrFunction.STDDEV_POP, "measure"));
        assertEquals("var_samp(measure)", parser.genAggrSQL(AggrFunction.VAR_SAMP, "measure"));
        assertEquals("var_pop(measure)", parser.genAggrSQL(AggrFunction.VAR_POP, "measure"));
    }
}
