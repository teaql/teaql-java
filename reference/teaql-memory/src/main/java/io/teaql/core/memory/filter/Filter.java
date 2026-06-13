package io.teaql.core.memory.filter;

import io.teaql.core.Entity;
import io.teaql.core.SearchCriteria;

public interface Filter<T extends Entity> {
    boolean accept(T entity, SearchCriteria searchCriteria);
}
