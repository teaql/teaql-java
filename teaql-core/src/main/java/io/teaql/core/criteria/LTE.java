package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class LTE extends TwoOperatorCriteria implements SearchCriteria {
    public LTE(Expression left, Expression right) {
        super(Operator.LESS_THAN_OR_EQUAL, left, right);
    }
}
