package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class NotEndWith extends TwoOperatorCriteria implements SearchCriteria {
    public NotEndWith(Expression left, Expression right) {
        super(Operator.NOT_END_WITH, left, right);
    }
}
