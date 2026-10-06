
package com.teaql.tracechainservice.orderitem;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class OrderItemListExpression<T, E, U extends OrderItem> extends SmartListExpression<T, E, U> {
    public OrderItemListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public OrderItemListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public OrderItemExpression<T, U, U> first() {
       return new OrderItemExpression(super.first());
    }

    public OrderItemExpression<T, U, U> get(int index) {
      return new OrderItemExpression(super.get(index));
    }
}