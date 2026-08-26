
package com.example.schoolmanagementservice.platform;

import com.example.schoolmanagementservice.school.School;
import com.example.schoolmanagementservice.school.SchoolListExpression;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import com.example.schoolmanagementservice.schooltype.SchoolTypeListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDateTime;
import java.util.function.Function;

public class PlatformExpression<T, E, U extends Platform> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public PlatformExpression(Expression<T, U> expression){
        super(expression);
    }

    public PlatformExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public PlatformExpression<T, U, U> updateId(Long id){
        return new PlatformExpression(this, $it -> {((Platform)$it).__internalSet("id", id); return this;});
     }

     public PlatformExpression<T, U, U> save(UserContext userContext){
        return new PlatformExpression(this, $it -> ((Platform)$it).auditAs("Saved by Expression").save(userContext));
     }

     public PlatformExpression<T, U, U> save(String intent, UserContext userContext){
        return new PlatformExpression(this, $it -> ((Platform)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", Platform::getName);
    }
    public PlatformExpression<T, U, U> updateName(String name){
       return new PlatformExpression(this, $it ->  ((Platform)$it).updateName(name));
    }

    public Expression<T, String> getBaseUrl(){
       return loaded("baseUrl", Platform::getBaseUrl);
    }
    public PlatformExpression<T, U, U> updateBaseUrl(String baseUrl){
       return new PlatformExpression(this, $it ->  ((Platform)$it).updateBaseUrl(baseUrl));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("createTime", Platform::getCreateTime);
    }
    public PlatformExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new PlatformExpression(this, $it ->  ((Platform)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("updateTime", Platform::getUpdateTime);
    }
    public PlatformExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new PlatformExpression(this, $it ->  ((Platform)$it).updateUpdateTime(updateTime));
    }

    public SchoolTypeListExpression<T, U, SchoolType> getSchoolTypeList(){
        return new SchoolTypeListExpression(loaded("schoolTypeList", Platform::getSchoolTypeList));
    }
    public SchoolListExpression<T, U, School> getSchoolList(){
        return new SchoolListExpression(loaded("schoolList", Platform::getSchoolList));
    }
    public PlatformExpression<T, U, U> addSchoolType(SchoolType schoolType){
       return new PlatformExpression(this, $it ->  ((Platform)$it).addSchoolType(schoolType));
    }
    public PlatformExpression<T, U, U> addSchool(School school){
       return new PlatformExpression(this, $it ->  ((Platform)$it).addSchool(school));
    }
}