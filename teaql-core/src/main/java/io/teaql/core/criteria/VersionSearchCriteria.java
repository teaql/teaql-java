package io.teaql.core.criteria;

import java.util.List;
import java.util.Objects;

import io.teaql.core.SearchCriteria;
import io.teaql.core.UserContext;

public class VersionSearchCriteria implements SearchCriteria {
    private SearchCriteria searchCriteria;

    public VersionSearchCriteria(SearchCriteria pSearchCriteria) {
        searchCriteria = pSearchCriteria;
    }

    @Override
    public List<String> properties(UserContext ctx) {
        return searchCriteria.properties(ctx);
    }

    public SearchCriteria getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(SearchCriteria pSearchCriteria) {
        searchCriteria = pSearchCriteria;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof VersionSearchCriteria that)) return false;
        return Objects.equals(getSearchCriteria(), that.getSearchCriteria());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSearchCriteria());
    }
}
