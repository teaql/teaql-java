
package com.example.inheritancematchingservice.college;

import com.example.inheritancematchingservice.school.SchoolExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import java.util.function.Function;

public class CollegeExpression<T, E, U extends College> extends SchoolExpression<T, E, U> implements BaseEntityExpression<T, U> {
    public CollegeExpression(Expression<T, U> expression){
        super(expression);
    }

    public CollegeExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CollegeExpression<T, U, U> updateId(Long id){
        return new CollegeExpression(this, $it -> {((College)$it).__internalSet("id", id); return this;});
     }

     public CollegeExpression<T, U, U> save(UserContext userContext){
        return new CollegeExpression(this, $it -> ((College)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CollegeExpression<T, U, U> save(String intent, UserContext userContext){
        return new CollegeExpression(this, $it -> ((College)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getDegreeLevel(){
       return loaded("degree_level", "getDegreeLevel", College::getDegreeLevel);
    }
    public CollegeExpression<T, U, U> updateDegreeLevel(String degreeLevel){
       return new CollegeExpression(this, $it ->  ((College)$it).updateDegreeLevel(degreeLevel));
    }

}