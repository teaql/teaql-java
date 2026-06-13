package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class IsNull extends OneOperatorCriteria implements SearchCriteria {
    public IsNull(Expression expression) {
        super(Operator.IS_NULL, expression);
    }
}
