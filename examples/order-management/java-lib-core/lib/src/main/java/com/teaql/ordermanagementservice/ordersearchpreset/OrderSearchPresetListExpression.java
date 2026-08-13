
package com.teaql.ordermanagementservice.ordersearchpreset;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class OrderSearchPresetListExpression<T, E, U extends OrderSearchPreset> extends SmartListExpression<T, E, U> {
    public OrderSearchPresetListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public OrderSearchPresetListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public OrderSearchPresetExpression<T, U, U> first() {
       return new OrderSearchPresetExpression(super.first());
    }

    public OrderSearchPresetExpression<T, U, U> get(int index) {
      return new OrderSearchPresetExpression(super.get(index));
    }
}