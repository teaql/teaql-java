package io.teaql.core;

import java.util.Objects;

public class TypeCriteria implements SearchCriteria {
    private Parameter typeParameter;

    public TypeCriteria(Parameter pTypeParameter) {
        typeParameter = pTypeParameter;
    }

    public TypeCriteria() {
    }

    public Parameter getTypeParameter() {
        return typeParameter;
    }

    public void setTypeParameter(Parameter pTypeParameter) {
        typeParameter = pTypeParameter;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof TypeCriteria that)) return false;
        return Objects.equals(getTypeParameter(), that.getTypeParameter());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getTypeParameter());
    }
}
