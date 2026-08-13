
package com.teaql.ordermanagementservice.product;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class ProductListExpression<T, E, U extends Product> extends SmartListExpression<T, E, U> {
    public ProductListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public ProductListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public ProductExpression<T, U, U> first() {
       return new ProductExpression(super.first());
    }

    public ProductExpression<T, U, U> get(int index) {
      return new ProductExpression(super.get(index));
    }
}