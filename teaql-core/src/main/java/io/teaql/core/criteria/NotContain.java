package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class NotContain extends TwoOperatorCriteria implements SearchCriteria {
    public NotContain(Expression left, Expression right) {
        super(Operator.NOT_CONTAIN, left, right);
    }
}
