
package com.example.checkernaturalmessageservice.catalog;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class CatalogListExpression<T, E, U extends Catalog> extends SmartListExpression<T, E, U> {
    public CatalogListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public CatalogListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public CatalogExpression<T, U, U> first() {
       return new CatalogExpression(super.first());
    }

    public CatalogExpression<T, U, U> get(int index) {
      return new CatalogExpression(super.get(index));
    }
}