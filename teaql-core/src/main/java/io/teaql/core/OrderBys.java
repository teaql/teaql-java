package io.teaql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderBys implements Expression {
    private List<OrderBy> orderBys = new ArrayList<>();

    public List<OrderBy> getOrderBys() {
        return orderBys;
    }

    public void setOrderBys(List<OrderBy> pOrderBys) {
        orderBys = pOrderBys;
    }

    public OrderBys addOrderBy(OrderBy orderBy) {
        if (orderBy != null) {
            orderBys.add(orderBy);
        }
        return this;
    }

    @Override
    public List<String> properties(UserContext ctx) {
        List<String> ret = new ArrayList<>();

        for (Expression expression : orderBys) {
            List<String> properties = expression.properties(ctx);
            if (properties != null) {
                ret.addAll(properties);
            }
        }
        return ret;
    }

    public boolean isEmpty() {
        return orderBys.isEmpty();
    }

    @Override
    public boolean equals(Object pO) {
        if (this == pO) return true;
        if (!(pO instanceof OrderBys orderBys1)) return false;
        return Objects.equals(getOrderBys(), orderBys1.getOrderBys());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getOrderBys());
    }
}
