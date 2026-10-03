package io.teaql.runtime;

import io.teaql.core.AggregationResult;
import io.teaql.core.QueryResult;
import io.teaql.core.SmartList;

public class DefaultQueryResult implements QueryResult {
    private final SmartList<?> result;
    private final AggregationResult aggregationResult;
    private final java.util.List<io.teaql.core.ExecutionMetadata> statements;

    public DefaultQueryResult(SmartList<?> result) {
        this(result, null);
    }

    public DefaultQueryResult(SmartList<?> result, AggregationResult aggregationResult) {
        this(result, aggregationResult, java.util.List.of());
    }

    public DefaultQueryResult(SmartList<?> result, AggregationResult aggregationResult,
            java.util.List<io.teaql.core.ExecutionMetadata> statements) {
        this.result = result;
        this.aggregationResult = aggregationResult;
        this.statements = java.util.List.copyOf(statements);
    }

    @Override public java.util.List<io.teaql.core.ExecutionMetadata> statements() { return statements; }

    public SmartList<?> getResult() {
        return result;
    }

    public AggregationResult getAggregationResult() {
        return aggregationResult;
    }
}
