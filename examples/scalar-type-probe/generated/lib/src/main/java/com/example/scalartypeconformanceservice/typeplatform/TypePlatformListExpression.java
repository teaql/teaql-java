
package com.example.scalartypeconformanceservice.typeplatform;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class TypePlatformListExpression<T, E, U extends TypePlatform> extends SmartListExpression<T, E, U> {
    public TypePlatformListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public TypePlatformListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public TypePlatformExpression<T, U, U> first() {
       return new TypePlatformExpression(super.first());
    }

    public TypePlatformExpression<T, U, U> get(int index) {
      return new TypePlatformExpression(super.get(index));
    }
}