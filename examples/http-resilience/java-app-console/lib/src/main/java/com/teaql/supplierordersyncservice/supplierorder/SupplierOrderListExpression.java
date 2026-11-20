
package com.teaql.supplierordersyncservice.supplierorder;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class SupplierOrderListExpression<T, E, U extends SupplierOrder> extends SmartListExpression<T, E, U> {
    public SupplierOrderListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public SupplierOrderListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public SupplierOrderExpression<T, U, U> first() {
       return new SupplierOrderExpression(super.first());
    }

    public SupplierOrderExpression<T, U, U> get(int index) {
      return new SupplierOrderExpression(super.get(index));
    }
}