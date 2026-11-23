
package com.teaql.runtimeexampleconformanceservice.workitem;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class WorkItemListExpression<T, E, U extends WorkItem> extends SmartListExpression<T, E, U> {
    public WorkItemListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public WorkItemListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public WorkItemExpression<T, U, U> first() {
       return new WorkItemExpression(super.first());
    }

    public WorkItemExpression<T, U, U> get(int index) {
      return new WorkItemExpression(super.get(index));
    }
}