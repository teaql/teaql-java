
package com.teaql.tracechainservice.paymentattempt;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class PaymentAttemptListExpression<T, E, U extends PaymentAttempt> extends SmartListExpression<T, E, U> {
    public PaymentAttemptListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public PaymentAttemptListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public PaymentAttemptExpression<T, U, U> first() {
       return new PaymentAttemptExpression(super.first());
    }

    public PaymentAttemptExpression<T, U, U> get(int index) {
      return new PaymentAttemptExpression(super.get(index));
    }
}