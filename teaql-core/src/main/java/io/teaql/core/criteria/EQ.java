package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class EQ extends TwoOperatorCriteria implements SearchCriteria {

    public EQ(Expression left, Expression right) {
        super(Operator.EQUAL, left, right);
    }
}
