package io.teaql.core;

public final class DataServiceCapabilities {
    private boolean query;
    private boolean streamingQuery;
    private boolean mutation;
    private boolean batchMutation;
    private boolean aggregation;
    private boolean transaction;
    private boolean schema;
    private boolean relationLoad;
    private boolean relationMutation;
    private boolean fullTextSearch;
    private boolean returning;

    public boolean isQuery() { return query; }
    public void setQuery(boolean query) { this.query = query; }

    public boolean isStreamingQuery() { return streamingQuery; }
    public void setStreamingQuery(boolean streamingQuery) { this.streamingQuery = streamingQuery; }

    public boolean isMutation() { return mutation; }
    public void setMutation(boolean mutation) { this.mutation = mutation; }

    public boolean isBatchMutation() { return batchMutation; }
    public void setBatchMutation(boolean batchMutation) { this.batchMutation = batchMutation; }

    public boolean isAggregation() { return aggregation; }
    public void setAggregation(boolean aggregation) { this.aggregation = aggregation; }

    public boolean isTransaction() { return transaction; }
    public void setTransaction(boolean transaction) { this.transaction = transaction; }

    public boolean isSchema() { return schema; }
    public void setSchema(boolean schema) { this.schema = schema; }

    public boolean isRelationLoad() { return relationLoad; }
    public void setRelationLoad(boolean relationLoad) { this.relationLoad = relationLoad; }

    public boolean isRelationMutation() { return relationMutation; }
    public void setRelationMutation(boolean relationMutation) { this.relationMutation = relationMutation; }

    public boolean isFullTextSearch() { return fullTextSearch; }
    public void setFullTextSearch(boolean fullTextSearch) { this.fullTextSearch = fullTextSearch; }

    public boolean isReturning() { return returning; }
    public void setReturning(boolean returning) { this.returning = returning; }
}
