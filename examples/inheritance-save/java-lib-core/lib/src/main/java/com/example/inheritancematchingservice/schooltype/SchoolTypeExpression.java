
package com.example.inheritancematchingservice.schooltype;

import com.example.inheritancematchingservice.platform.Platform;
import com.example.inheritancematchingservice.platform.PlatformExpression;
import com.example.inheritancematchingservice.school.School;
import com.example.inheritancematchingservice.school.SchoolListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class SchoolTypeExpression<T, E, U extends SchoolType> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public SchoolTypeExpression(Expression<T, U> expression){
        super(expression);
    }

    public SchoolTypeExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public SchoolTypeExpression<T, U, U> updateId(Long id){
        return new SchoolTypeExpression(this, $it -> {((SchoolType)$it).__internalSet("id", id); return this;});
     }

     public SchoolTypeExpression<T, U, U> save(UserContext userContext){
        return new SchoolTypeExpression(this, $it -> ((SchoolType)$it).auditAs("Saved by Expression").save(userContext));
     }

     public SchoolTypeExpression<T, U, U> save(String intent, UserContext userContext){
        return new SchoolTypeExpression(this, $it -> ((SchoolType)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", "getPlatform", SchoolType::getPlatform));
    }

    public SchoolTypeExpression<T, U, U> updatePlatform(Platform platform){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).updatePlatform(platform));
    }

    public Expression<T, String> getName(){
       return loaded("name", "getName", SchoolType::getName);
    }
    public SchoolTypeExpression<T, U, U> updateName(String name){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).updateName(name));
    }

    public Expression<T, String> getCode(){
       return loaded("code", "getCode", SchoolType::getCode);
    }
    public SchoolTypeExpression<T, U, U> updateCode(String code){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).updateCode(code));
    }

    public SchoolListExpression<T, U, School> getSchoolList(){
        return new SchoolListExpression(loaded("school_list", "getSchoolList", SchoolType::getSchoolList));
    }
    public SchoolTypeExpression<T, U, U> addSchool(School school){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).addSchool(school));
    }
}