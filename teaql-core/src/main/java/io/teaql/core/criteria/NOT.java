package io.teaql.core.criteria;

import io.teaql.core.FunctionApply;
import io.teaql.core.SearchCriteria;

public class NOT extends FunctionApply implements SearchCriteria {
    public NOT(SearchCriteria sub) {
        super(LogicOperator.NOT, sub);
    }
}
