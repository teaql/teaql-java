
package com.example.freightquotesettlement.freightquote;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class FreightQuoteListExpression<T, E, U extends FreightQuote> extends SmartListExpression<T, E, U> {
    public FreightQuoteListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public FreightQuoteListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public FreightQuoteExpression<T, U, U> first() {
       return new FreightQuoteExpression(super.first());
    }

    public FreightQuoteExpression<T, U, U> get(int index) {
      return new FreightQuoteExpression(super.get(index));
    }
}