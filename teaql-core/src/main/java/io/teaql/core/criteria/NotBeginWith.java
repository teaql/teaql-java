package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class NotBeginWith extends TwoOperatorCriteria implements SearchCriteria {
    public NotBeginWith(Expression left, Expression right) {
        super(Operator.NOT_BEGIN_WITH, left, right);
    }
}
