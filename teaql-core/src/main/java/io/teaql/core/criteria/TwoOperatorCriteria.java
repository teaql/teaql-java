package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.FunctionApply;
import io.teaql.core.PropertyFunction;
import io.teaql.core.SearchCriteria;

public class TwoOperatorCriteria extends FunctionApply implements SearchCriteria {
    public TwoOperatorCriteria(PropertyFunction operator, Expression left, Expression right) {
        super(operator, left, right);
    }
}
