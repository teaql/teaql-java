
package com.example.schoolmanagementservice.schooltype;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformExpression;
import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.school.SchoolListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
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
       return new PlatformExpression(loaded("platform", SchoolType::getPlatform));
    }

    public SchoolTypeExpression<T, U, U> updatePlatform(Platform platform){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).updatePlatform(platform));
    }

    public Expression<T, String> getName(){
       return loaded("name", SchoolType::getName);
    }
    public SchoolTypeExpression<T, U, U> updateName(String name){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).updateName(name));
    }

    public Expression<T, String> getCode(){
       return loaded("code", SchoolType::getCode);
    }
    public SchoolTypeExpression<T, U, U> updateCode(String code){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).updateCode(code));
    }

    public Expression<T, BigDecimal> getDisplayOrder(){
       return loaded("displayOrder", SchoolType::getDisplayOrder);
    }
    public SchoolTypeExpression<T, U, U> updateDisplayOrder(BigDecimal displayOrder){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).updateDisplayOrder(displayOrder));
    }

    public SchoolListExpression<T, U, School> getSchoolList(){
        return new SchoolListExpression(loaded("schoolList", SchoolType::getSchoolList));
    }
    public SchoolTypeExpression<T, U, U> addSchool(School school){
       return new SchoolTypeExpression(this, $it ->  ((SchoolType)$it).addSchool(school));
    }
}