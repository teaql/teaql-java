
package com.example.schoolmanagementservice.school;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class SchoolListExpression<T, E, U extends School> extends SmartListExpression<T, E, U> {
    public SchoolListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public SchoolListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public SchoolExpression<T, U, U> first() {
       return new SchoolExpression(super.first());
    }

    public SchoolExpression<T, U, U> get(int index) {
      return new SchoolExpression(super.get(index));
    }
}