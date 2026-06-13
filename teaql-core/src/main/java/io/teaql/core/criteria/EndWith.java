package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class EndWith extends TwoOperatorCriteria implements SearchCriteria {
    public EndWith(Expression left, Expression right) {
        super(Operator.END_WITH, left, right);
    }
}
