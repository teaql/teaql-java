
package com.teaql.checkerfixadvanced.tenant;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class TenantListExpression<T, E, U extends Tenant> extends SmartListExpression<T, E, U> {
    public TenantListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public TenantListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public TenantExpression<T, U, U> first() {
       return new TenantExpression(super.first());
    }

    public TenantExpression<T, U, U> get(int index) {
      return new TenantExpression(super.get(index));
    }
}