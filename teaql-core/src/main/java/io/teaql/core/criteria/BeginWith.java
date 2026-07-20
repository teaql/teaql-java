package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class BeginWith extends TwoOperatorCriteria implements SearchCriteria {
    public BeginWith(Expression left, Expression right) {
        super(Operator.BEGIN_WITH, left, right);
    }
}
