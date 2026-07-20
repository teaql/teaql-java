package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.FunctionApply;
import io.teaql.core.SearchCriteria;

public class OneOperatorCriteria extends FunctionApply implements SearchCriteria {
    public OneOperatorCriteria(Operator operator, Expression expression) {
        super(operator, expression);
    }
}
