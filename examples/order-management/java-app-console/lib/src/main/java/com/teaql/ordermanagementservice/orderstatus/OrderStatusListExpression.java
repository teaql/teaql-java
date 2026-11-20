
package com.teaql.ordermanagementservice.orderstatus;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class OrderStatusListExpression<T, E, U extends OrderStatus> extends SmartListExpression<T, E, U> {
    public OrderStatusListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public OrderStatusListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public OrderStatusExpression<T, U, U> first() {
       return new OrderStatusExpression(super.first());
    }

    public OrderStatusExpression<T, U, U> get(int index) {
      return new OrderStatusExpression(super.get(index));
    }
}