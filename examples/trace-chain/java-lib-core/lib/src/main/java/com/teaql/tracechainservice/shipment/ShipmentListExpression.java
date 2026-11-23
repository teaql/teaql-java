
package com.teaql.tracechainservice.shipment;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class ShipmentListExpression<T, E, U extends Shipment> extends SmartListExpression<T, E, U> {
    public ShipmentListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public ShipmentListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public ShipmentExpression<T, U, U> first() {
       return new ShipmentExpression(super.first());
    }

    public ShipmentExpression<T, U, U> get(int index) {
      return new ShipmentExpression(super.get(index));
    }
}