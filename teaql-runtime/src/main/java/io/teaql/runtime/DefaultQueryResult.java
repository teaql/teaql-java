package io.teaql.runtime;

import io.teaql.core.AggregationResult;
import io.teaql.core.QueryResult;
import io.teaql.core.SmartList;

public class DefaultQueryResult implements QueryResult {
    private final SmartList<?> result;
    private final AggregationResult aggregationResult;

    public DefaultQueryResult(SmartList<?> result) {
        this(result, null);
    }

    public DefaultQueryResult(SmartList<?> result, AggregationResult aggregationResult) {
        this.result = result;
        this.aggregationResult = aggregationResult;
    }

    public SmartList<?> getResult() {
        return result;
    }

    public AggregationResult getAggregationResult() {
        return aggregationResult;
    }
}
