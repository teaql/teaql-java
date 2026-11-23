
package com.example.exactdecimalservice.decimalentry;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class DecimalEntryListExpression<T, E, U extends DecimalEntry> extends SmartListExpression<T, E, U> {
    public DecimalEntryListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public DecimalEntryListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public DecimalEntryExpression<T, U, U> first() {
       return new DecimalEntryExpression(super.first());
    }

    public DecimalEntryExpression<T, U, U> get(int index) {
      return new DecimalEntryExpression(super.get(index));
    }
}