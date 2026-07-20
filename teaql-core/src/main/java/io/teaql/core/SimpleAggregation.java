package io.teaql.core;

import java.util.Objects;

public class SimpleAggregation implements Expression {
    private String name;
    private SearchRequest aggregateRequest;

    private boolean singleNumber;

    public SimpleAggregation(String name, SearchRequest pAggregateRequest) {
        this.name = name;
        aggregateRequest = pAggregateRequest;
    }

    public SimpleAggregation(String name, SearchRequest aggregateRequest, boolean singleNumber) {
        this.name = name;
        this.aggregateRequest = aggregateRequest;
        this.singleNumber = singleNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public SearchRequest getAggregateRequest() {
        return aggregateRequest;
    }

    public void setAggregateRequest(SearchRequest pAggregateRequest) {
        aggregateRequest = pAggregateRequest;
    }

    public boolean isSingleNumber() {
        return singleNumber;
    }

    public void setSingleNumber(boolean pSingleNumber) {
        singleNumber = pSingleNumber;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof SimpleAggregation that)) return false;
        return isSingleNumber() == that.isSingleNumber()
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getAggregateRequest(), that.getAggregateRequest());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getAggregateRequest(), isSingleNumber());
    }
}
