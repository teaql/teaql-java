
package com.teaql.ordereditreference.salesorder;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class SalesOrderListExpression<T, E, U extends SalesOrder> extends SmartListExpression<T, E, U> {
    public SalesOrderListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public SalesOrderListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public SalesOrderExpression<T, U, U> first() {
       return new SalesOrderExpression(super.first());
    }

    public SalesOrderExpression<T, U, U> get(int index) {
      return new SalesOrderExpression(super.get(index));
    }
}