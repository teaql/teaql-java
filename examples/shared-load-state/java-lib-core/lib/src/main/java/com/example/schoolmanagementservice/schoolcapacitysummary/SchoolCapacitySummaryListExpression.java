
package com.example.schoolmanagementservice.schoolcapacitysummary;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class SchoolCapacitySummaryListExpression<T, E, U extends SchoolCapacitySummary> extends SmartListExpression<T, E, U> {
    public SchoolCapacitySummaryListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public SchoolCapacitySummaryListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public SchoolCapacitySummaryExpression<T, U, U> first() {
       return new SchoolCapacitySummaryExpression(super.first());
    }

    public SchoolCapacitySummaryExpression<T, U, U> get(int index) {
      return new SchoolCapacitySummaryExpression(super.get(index));
    }
}