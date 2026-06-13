package io.teaql.runtime.memory;

import io.teaql.core.Entity;
import io.teaql.core.SearchCriteria;

public interface Filter<T extends Entity> {
    boolean accept(T entity, SearchCriteria criteria);
}
