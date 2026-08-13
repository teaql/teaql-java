
package com.teaql.ordermanagementservice.customerorder;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class CustomerOrderListExpression<T, E, U extends CustomerOrder> extends SmartListExpression<T, E, U> {
    public CustomerOrderListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public CustomerOrderListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public CustomerOrderExpression<T, U, U> first() {
       return new CustomerOrderExpression(super.first());
    }

    public CustomerOrderExpression<T, U, U> get(int index) {
      return new CustomerOrderExpression(super.get(index));
    }
}