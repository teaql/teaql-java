
package com.example.inheritancematchingservice.academy;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class AcademyListExpression<T, E, U extends Academy> extends SmartListExpression<T, E, U> {
    public AcademyListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public AcademyListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public AcademyExpression<T, U, U> first() {
       return new AcademyExpression(super.first());
    }

    public AcademyExpression<T, U, U> get(int index) {
      return new AcademyExpression(super.get(index));
    }
}