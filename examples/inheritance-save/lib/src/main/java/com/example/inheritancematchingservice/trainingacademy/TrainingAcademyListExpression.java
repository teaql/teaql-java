
package com.example.inheritancematchingservice.trainingacademy;

import io.teaql.core.SmartList;
import io.teaql.core.value.Expression;
import io.teaql.core.value.SmartListExpression;
import java.util.function.Function;

public class TrainingAcademyListExpression<T, E, U extends TrainingAcademy> extends SmartListExpression<T, E, U> {
    public TrainingAcademyListExpression(Expression<T, SmartList<U>> expression){
        super(expression);
    }

    public TrainingAcademyListExpression(Expression<T, E> expression, Function<E, SmartList<U>> function){
        super(expression, function);
    }

    public TrainingAcademyExpression<T, U, U> first() {
       return new TrainingAcademyExpression(super.first());
    }

    public TrainingAcademyExpression<T, U, U> get(int index) {
      return new TrainingAcademyExpression(super.get(index));
    }
}