
package com.teaql.runtimeexampleconformanceservice.platform;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class PlatformListExpression<T, E, U extends Platform> extends SmartListExpression<T, E, U> {
    public PlatformListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public PlatformListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public PlatformExpression<T, U, U> first() {
       return new PlatformExpression(super.first());
    }

    public PlatformExpression<T, U, U> get(int index) {
      return new PlatformExpression(super.get(index));
    }
}