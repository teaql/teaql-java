
package com.teaql.ordermanagementservice.orderline;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class OrderLineListExpression<T, E, U extends OrderLine> extends SmartListExpression<T, E, U> {
    public OrderLineListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public OrderLineListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public OrderLineExpression<T, U, U> first() {
       return new OrderLineExpression(super.first());
    }

    public OrderLineExpression<T, U, U> get(int index) {
      return new OrderLineExpression(super.get(index));
    }
}