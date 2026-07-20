package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class Contain extends TwoOperatorCriteria implements SearchCriteria {
    public Contain(Expression left, Expression right) {
        super(Operator.CONTAIN, left, right);
    }
}
