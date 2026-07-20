package io.teaql.core.criteria;

import io.teaql.core.FunctionApply;
import io.teaql.core.PropertyAware;
import io.teaql.core.SearchCriteria;

public class AND extends FunctionApply implements SearchCriteria, PropertyAware {
    public AND(SearchCriteria... pSubs) {
        super(LogicOperator.AND, pSubs);
    }
}
