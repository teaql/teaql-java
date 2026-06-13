package io.teaql.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.teaql.core.utils.CollectionUtil;
import io.teaql.core.utils.ArrayUtil;
import io.teaql.core.utils.ObjectUtil;

import io.teaql.core.criteria.Operator;

public class Parameter implements Expression {
    private String name;
    private Object value;
    private Operator operator;

    public Parameter(String name, Object value, Operator operator) {
        this.name = name;
        this.operator = operator;
        List values = flatValues(value);
        if (operator.hasMultiValue()) {
            this.value = values;
        }
        else {
            Object first = CollectionUtil.getFirst(values);
            this.value = first;
        }
    }

    private Parameter(String name, Object value, boolean multiValue) {
        this.name = name;
        List values = flatValues(value);
        if (multiValue) {
            this.value = values.toArray();
        }
        else {
            Object first = CollectionUtil.getFirst(values);
            this.value = first;
        }
    }

    private Parameter(String name, Object value) {
        this(name, value, true);
    }

    public static List flatValues(Object value) {
        List ret = new ArrayList();
        visit(ret, value);
        return ret;
    }

    private static void visit(List ret, Object pValue) {
        if (ObjectUtil.isEmpty(pValue)) {
            return;
        }
        if (ArrayUtil.isArray(pValue)) {
            int length = ArrayUtil.length(pValue);
            for (int i = 0; i < length; i++) {
                visit(ret, ArrayUtil.get(pValue, i));
            }
        }
        else if (pValue instanceof Iterator) {
            Iterator it = (Iterator) pValue;
            while (it.hasNext()) {
                visit(ret, it.next());
            }
        }
        else if (pValue instanceof Iterable) {
            visit(ret, ((Iterable<?>) pValue).iterator());
        }
        else if (pValue instanceof Entity) {
            ret.add(((Entity) pValue).getId());
        }
        else {
            ret.add(pValue);
        }
    }

    public Object getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator pOperator) {
        operator = pOperator;
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof Parameter parameter)) return false;
        return Objects.equals(getName(), parameter.getName())
                && Objects.equals(getValue(), parameter.getValue())
                && getOperator() == parameter.getOperator();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue(), getOperator());
    }
}
