package io.teaql.core.criteria;

import io.teaql.core.FunctionApply;
import io.teaql.core.PropertyAware;
import io.teaql.core.SearchCriteria;

public class OR extends FunctionApply implements SearchCriteria, PropertyAware {
    public OR(SearchCriteria... pSubs) {
        super(LogicOperator.OR, pSubs);
    }
}
