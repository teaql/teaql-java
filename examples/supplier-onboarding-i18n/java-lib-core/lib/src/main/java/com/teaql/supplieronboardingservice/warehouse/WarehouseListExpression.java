
package com.teaql.supplieronboardingservice.warehouse;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class WarehouseListExpression<T, E, U extends Warehouse> extends SmartListExpression<T, E, U> {
    public WarehouseListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public WarehouseListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public WarehouseExpression<T, U, U> first() {
       return new WarehouseExpression(super.first());
    }

    public WarehouseExpression<T, U, U> get(int index) {
      return new WarehouseExpression(super.get(index));
    }
}