
package com.teaql.ordermanagementservice.customer;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class CustomerListExpression<T, E, U extends Customer> extends SmartListExpression<T, E, U> {
    public CustomerListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public CustomerListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public CustomerExpression<T, U, U> first() {
       return new CustomerExpression(super.first());
    }

    public CustomerExpression<T, U, U> get(int index) {
      return new CustomerExpression(super.get(index));
    }
}