
package com.example.checkernaturalmessageservice.entry;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class EntryListExpression<T, E, U extends Entry> extends SmartListExpression<T, E, U> {
    public EntryListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public EntryListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public EntryExpression<T, U, U> first() {
       return new EntryExpression(super.first());
    }

    public EntryExpression<T, U, U> get(int index) {
      return new EntryExpression(super.get(index));
    }
}