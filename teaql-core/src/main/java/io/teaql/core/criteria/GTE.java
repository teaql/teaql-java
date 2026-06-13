package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class GTE extends TwoOperatorCriteria implements SearchCriteria {
    public GTE(Expression left, Expression right) {
        super(Operator.GREATER_THAN_OR_EQUAL, left, right);
    }
}
