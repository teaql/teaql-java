
package com.example.schoolmanagementservice.schooltype;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class SchoolTypeListExpression<T, E, U extends SchoolType> extends SmartListExpression<T, E, U> {
    public SchoolTypeListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public SchoolTypeListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public SchoolTypeExpression<T, U, U> first() {
       return new SchoolTypeExpression(super.first());
    }

    public SchoolTypeExpression<T, U, U> get(int index) {
      return new SchoolTypeExpression(super.get(index));
    }
}