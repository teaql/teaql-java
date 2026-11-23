
package com.example.inheritancematchingservice.trainingacademy;

import com.example.inheritancematchingservice.academy.AcademyExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import java.util.function.Function;

public class TrainingAcademyExpression<T, E, U extends TrainingAcademy> extends AcademyExpression<T, E, U> implements BaseEntityExpression<T, U> {
    public TrainingAcademyExpression(Expression<T, U> expression){
        super(expression);
    }

    public TrainingAcademyExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public TrainingAcademyExpression<T, U, U> updateId(Long id){
        return new TrainingAcademyExpression(this, $it -> {((TrainingAcademy)$it).__internalSet("id", id); return this;});
     }

     public TrainingAcademyExpression<T, U, U> save(UserContext userContext){
        return new TrainingAcademyExpression(this, $it -> ((TrainingAcademy)$it).auditAs("Saved by Expression").save(userContext));
     }

     public TrainingAcademyExpression<T, U, U> save(String intent, UserContext userContext){
        return new TrainingAcademyExpression(this, $it -> ((TrainingAcademy)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getSpecialty(){
       return loaded("specialty", "getSpecialty", TrainingAcademy::getSpecialty);
    }
    public TrainingAcademyExpression<T, U, U> updateSpecialty(String specialty){
       return new TrainingAcademyExpression(this, $it ->  ((TrainingAcademy)$it).updateSpecialty(specialty));
    }

}