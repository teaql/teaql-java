package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class GT extends TwoOperatorCriteria implements SearchCriteria {
    public GT(Expression left, Expression right) {
        super(Operator.GREATER_THAN, left, right);
    }
}
