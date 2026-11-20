
package com.teaql.ordereditreference.merchant;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class MerchantListExpression<T, E, U extends Merchant> extends SmartListExpression<T, E, U> {
    public MerchantListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public MerchantListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public MerchantExpression<T, U, U> first() {
       return new MerchantExpression(super.first());
    }

    public MerchantExpression<T, U, U> get(int index) {
      return new MerchantExpression(super.get(index));
    }
}