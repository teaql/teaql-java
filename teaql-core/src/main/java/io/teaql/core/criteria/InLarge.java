package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class InLarge extends TwoOperatorCriteria implements SearchCriteria {
    public InLarge(Expression left, Expression right) {
        super(Operator.IN_LARGE, left, right);
    }
}
