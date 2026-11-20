
package com.teaql.ordermanagementservice.commerceplatform;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class CommercePlatformListExpression<T, E, U extends CommercePlatform> extends SmartListExpression<T, E, U> {
    public CommercePlatformListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public CommercePlatformListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public CommercePlatformExpression<T, U, U> first() {
       return new CommercePlatformExpression(super.first());
    }

    public CommercePlatformExpression<T, U, U> get(int index) {
      return new CommercePlatformExpression(super.get(index));
    }
}