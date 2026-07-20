package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class NotIn extends TwoOperatorCriteria implements SearchCriteria {
    public NotIn(Expression left, Expression right) {
        super(Operator.NOT_IN, left, right);
    }
}
