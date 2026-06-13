package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class LT extends TwoOperatorCriteria implements SearchCriteria {
    public LT(Expression left, Expression right) {
        super(Operator.LESS_THAN, left, right);
    }
}
