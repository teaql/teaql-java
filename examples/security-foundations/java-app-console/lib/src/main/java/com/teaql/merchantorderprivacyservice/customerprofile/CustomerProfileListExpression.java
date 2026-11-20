
package com.teaql.merchantorderprivacyservice.customerprofile;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class CustomerProfileListExpression<T, E, U extends CustomerProfile> extends SmartListExpression<T, E, U> {
    public CustomerProfileListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public CustomerProfileListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public CustomerProfileExpression<T, U, U> first() {
       return new CustomerProfileExpression(super.first());
    }

    public CustomerProfileExpression<T, U, U> get(int index) {
      return new CustomerProfileExpression(super.get(index));
    }
}