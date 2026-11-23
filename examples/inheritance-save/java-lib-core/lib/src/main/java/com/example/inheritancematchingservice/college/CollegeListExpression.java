
package com.example.inheritancematchingservice.college;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class CollegeListExpression<T, E, U extends College> extends SmartListExpression<T, E, U> {
    public CollegeListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public CollegeListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public CollegeExpression<T, U, U> first() {
       return new CollegeExpression(super.first());
    }

    public CollegeExpression<T, U, U> get(int index) {
      return new CollegeExpression(super.get(index));
    }
}