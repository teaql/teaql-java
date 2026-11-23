
package com.example.scalartypeconformanceservice.scalartypeprobe;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class ScalarTypeProbeListExpression<T, E, U extends ScalarTypeProbe> extends SmartListExpression<T, E, U> {
    public ScalarTypeProbeListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public ScalarTypeProbeListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public ScalarTypeProbeExpression<T, U, U> first() {
       return new ScalarTypeProbeExpression(super.first());
    }

    public ScalarTypeProbeExpression<T, U, U> get(int index) {
      return new ScalarTypeProbeExpression(super.get(index));
    }
}