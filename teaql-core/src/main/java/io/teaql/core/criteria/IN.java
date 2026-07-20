package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class IN extends TwoOperatorCriteria implements SearchCriteria {
    public IN(Expression left, Expression right) {
        super(Operator.IN, left, right);
    }
}
