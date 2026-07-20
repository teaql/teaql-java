package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.SearchCriteria;

public class IsNotNull extends OneOperatorCriteria implements SearchCriteria {
    public IsNotNull(Expression expressions) {
        super(Operator.IS_NOT_NULL, expressions);
    }
}
