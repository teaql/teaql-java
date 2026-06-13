package io.teaql.core;

import java.util.ArrayList;
import java.util.List;

public class Aggregations {
    String name;
    List<SimpleNamedExpression> aggregates = new ArrayList<>();
    List<SimpleNamedExpression> simpleDimensions = new ArrayList<>();
    List<SimpleNamedExpression> complexDimensions = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public List<SimpleNamedExpression> getAggregates() {
        return aggregates;
    }

    public void setAggregates(List<SimpleNamedExpression> pAggregates) {
        aggregates = pAggregates;
    }

    public List<SimpleNamedExpression> getSimpleDimensions() {
        return simpleDimensions;
    }

    public void setSimpleDimensions(List<SimpleNamedExpression> pSimpleDimensions) {
        simpleDimensions = pSimpleDimensions;
    }

    public List<SimpleNamedExpression> getComplexDimensions() {
        return complexDimensions;
    }

    public void setComplexDimensions(List<SimpleNamedExpression> pComplexDimensions) {
        complexDimensions = pComplexDimensions;
    }

    public List<SimpleNamedExpression> getSelectedExpressions() {
        List<SimpleNamedExpression> ret = new ArrayList<>();
        ret.addAll(getAggregates());
        ret.addAll(getSimpleDimensions());
        ret.addAll(getComplexDimensions());
        return ret;
    }

    public List<SimpleNamedExpression> getDimensions() {
        List<SimpleNamedExpression> ret = new ArrayList<>();
        ret.addAll(getSimpleDimensions());
        ret.addAll(getComplexDimensions());
        return ret;
    }
}
