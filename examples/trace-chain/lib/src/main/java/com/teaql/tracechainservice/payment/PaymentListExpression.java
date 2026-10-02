
package com.teaql.tracechainservice.payment;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class PaymentListExpression<T, E, U extends Payment> extends SmartListExpression<T, E, U> {
    public PaymentListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public PaymentListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public PaymentExpression<T, U, U> first() {
       return new PaymentExpression(super.first());
    }

    public PaymentExpression<T, U, U> get(int index) {
      return new PaymentExpression(super.get(index));
    }
}