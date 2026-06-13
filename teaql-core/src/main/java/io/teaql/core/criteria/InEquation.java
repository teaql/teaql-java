package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class InEquation extends TwoOperatorCriteria implements SearchCriteria {

    public InEquation(Expression left, Expression right) {
        super(Operator.NOT_EQUAL, left, right);
    }
}
