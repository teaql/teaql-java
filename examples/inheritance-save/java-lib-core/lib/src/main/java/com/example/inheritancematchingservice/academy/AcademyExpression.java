
package com.example.inheritancematchingservice.academy;

import com.example.inheritancematchingservice.school.SchoolExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import java.util.function.Function;

public class AcademyExpression<T, E, U extends Academy> extends SchoolExpression<T, E, U> implements BaseEntityExpression<T, U> {
    public AcademyExpression(Expression<T, U> expression){
        super(expression);
    }

    public AcademyExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public AcademyExpression<T, U, U> updateId(Long id){
        return new AcademyExpression(this, $it -> {((Academy)$it).__internalSet("id", id); return this;});
     }

     public AcademyExpression<T, U, U> save(UserContext userContext){
        return new AcademyExpression(this, $it -> ((Academy)$it).auditAs("Saved by Expression").save(userContext));
     }

     public AcademyExpression<T, U, U> save(String intent, UserContext userContext){
        return new AcademyExpression(this, $it -> ((Academy)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getCampusCode(){
       return loaded("campus_code", "getCampusCode", Academy::getCampusCode);
    }
    public AcademyExpression<T, U, U> updateCampusCode(String campusCode){
       return new AcademyExpression(this, $it ->  ((Academy)$it).updateCampusCode(campusCode));
    }

}