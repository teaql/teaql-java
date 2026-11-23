
package com.teaql.supplierordersyncservice.supplier;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class SupplierListExpression<T, E, U extends Supplier> extends SmartListExpression<T, E, U> {
    public SupplierListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public SupplierListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public SupplierExpression<T, U, U> first() {
       return new SupplierExpression(super.first());
    }

    public SupplierExpression<T, U, U> get(int index) {
      return new SupplierExpression(super.get(index));
    }
}