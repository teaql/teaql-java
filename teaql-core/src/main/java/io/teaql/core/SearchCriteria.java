package io.teaql.core;

import io.teaql.core.criteria.AND;
import io.teaql.core.criteria.NOT;
import io.teaql.core.criteria.OR;

public interface SearchCriteria extends Expression {
    String TRUE = "true";
    String FALSE = "false";

    static SearchCriteria and(SearchCriteria... sub) {
        return new AND(sub);
    }

    static SearchCriteria or(SearchCriteria... sub) {
        return new OR(sub);
    }

    static SearchCriteria not(SearchCriteria sub) {
        return new NOT(sub);
    }
}
