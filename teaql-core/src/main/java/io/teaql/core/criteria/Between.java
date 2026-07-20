package io.teaql.core.criteria;

import io.teaql.core.Expression;
import io.teaql.core.FunctionApply;
import io.teaql.core.SearchCriteria;

public class Between extends FunctionApply implements SearchCriteria {
    public Between(Expression expression1, Expression expression2, Expression expression3) {
        super(Operator.BETWEEN, expression1, expression2, expression3);
    }
}
