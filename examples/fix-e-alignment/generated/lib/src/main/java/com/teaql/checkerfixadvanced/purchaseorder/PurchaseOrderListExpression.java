
package com.teaql.checkerfixadvanced.purchaseorder;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class PurchaseOrderListExpression<T, E, U extends PurchaseOrder> extends SmartListExpression<T, E, U> {
    public PurchaseOrderListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public PurchaseOrderListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public PurchaseOrderExpression<T, U, U> first() {
       return new PurchaseOrderExpression(super.first());
    }

    public PurchaseOrderExpression<T, U, U> get(int index) {
      return new PurchaseOrderExpression(super.get(index));
    }
}