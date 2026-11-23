package io.teaql.runtime.memory;

import io.teaql.core.Parameter;
import io.teaql.core.PropertyReference;
import io.teaql.core.criteria.Operator;
import io.teaql.core.criteria.TwoOperatorCriteria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryMembershipCriteriaTest {
    private boolean matches(Number amount, Operator operator, Object candidates) {
        var row = new MemoryAggregationTest.Sample().amount(amount);
        return new CriteriaFilter<MemoryAggregationTest.Sample>().accept(row,
                new TwoOperatorCriteria(operator, new PropertyReference("amount"), new Parameter("amount", candidates, operator)));
    }
    @Test public void inAcceptsTheNormalizedListRatherThanComparingTheWholeList() {
        assertTrue(matches(2L, Operator.IN, List.of(1L, 2L)));
        assertFalse(matches(3L, Operator.IN, List.of(1L, 2L)));
    }
    @Test public void notInAcceptsNormalizedListAndExcludesMatches() {
        assertFalse(matches(2L, Operator.NOT_IN, List.of(1L, 2L)));
        assertTrue(matches(3L, Operator.NOT_IN, List.of(1L, 2L)));
    }
    @Test public void largeMembershipOperatorsUseTheSameValueSemantics() {
        assertTrue(matches(2L, Operator.IN_LARGE, List.of(1L, 2L)));
        assertFalse(matches(2L, Operator.NOT_IN_LARGE, List.of(1L, 2L)));
    }
    @Test public void primitiveAndObjectArraysRemainSupportedAfterParameterNormalization() {
        assertTrue(matches(2L, Operator.IN, new long[]{1L, 2L}));
        assertTrue(matches(2L, Operator.IN, new Long[]{1L, 2L}));
    }
    @Test public void emptyMembershipHasExplicitTruthValues() {
        assertFalse(matches(2L, Operator.IN, List.of()));
        assertTrue(matches(2L, Operator.NOT_IN, List.of()));
        assertFalse(matches(null, Operator.IN, List.of()));
        assertTrue(matches(null, Operator.NOT_IN, List.of()));
    }
    @Test public void nullOperandDoesNotMatchNonemptyInOrNotIn() {
        assertFalse(matches(null, Operator.IN, List.of(1L, 2L)));
        assertFalse(matches(null, Operator.NOT_IN, List.of(1L, 2L)));
    }
    @Test public void decimalMembershipDoesNotCollapseNearbyExactValues() {
        var value = new BigDecimal("9007199254740993.1");
        assertFalse(matches(value, Operator.IN, List.of(new BigDecimal("9007199254740993.2"))));
        assertTrue(matches(value, Operator.IN, List.of(new BigDecimal("9007199254740993.100"))));
    }
    @Test public void ordinaryOrderedComparisonsStillUseScalarOperands() {
        assertTrue(matches(2L, Operator.GREATER_THAN, 1L));
        assertFalse(matches(2L, Operator.LESS_THAN, 1L));
    }
}
