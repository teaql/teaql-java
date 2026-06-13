package io.teaql.core;

import java.util.List;
import java.util.Objects;

import io.teaql.core.utils.ListUtil;

public class SubQuerySearchCriteria implements SearchCriteria, PropertyAware {
    private String propertyName;
    private SearchRequest dependsOn;
    private String dependsOnPropertyName;

    public SubQuerySearchCriteria(
            String pPropertyName, SearchRequest pDependsOn, String pDependsOnPropertyName) {
        propertyName = pPropertyName;
        dependsOn = pDependsOn;
        dependsOnPropertyName = pDependsOnPropertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String pPropertyName) {
        propertyName = pPropertyName;
    }

    public SearchRequest getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(SearchRequest pDependsOn) {
        dependsOn = pDependsOn;
    }

    public String getDependsOnPropertyName() {
        return dependsOnPropertyName;
    }

    public void setDependsOnPropertyName(String pDependsOnPropertyName) {
        dependsOnPropertyName = pDependsOnPropertyName;
    }

    @Override
    public List<String> properties(UserContext ctx) {
        return ListUtil.of(propertyName);
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof SubQuerySearchCriteria that)) return false;
        return Objects.equals(getPropertyName(), that.getPropertyName())
                && Objects.equals(getDependsOn(), that.getDependsOn())
                && Objects.equals(getDependsOnPropertyName(), that.getDependsOnPropertyName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPropertyName(), getDependsOn(), getDependsOnPropertyName());
    }
}
