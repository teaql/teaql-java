
package com.example.inheritancematchingservice.platform;

import com.example.inheritancematchingservice.school.School;
import com.example.inheritancematchingservice.school.SchoolListExpression;
import com.example.inheritancematchingservice.schooltype.SchoolType;
import com.example.inheritancematchingservice.schooltype.SchoolTypeListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
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
       return loaded("name", "getName", Platform::getName);
    }
    public PlatformExpression<T, U, U> updateName(String name){
       return new PlatformExpression(this, $it ->  ((Platform)$it).updateName(name));
    }

    public Expression<T, String> getBaseUrl(){
       return loaded("base_url", "getBaseUrl", Platform::getBaseUrl);
    }
    public PlatformExpression<T, U, U> updateBaseUrl(String baseUrl){
       return new PlatformExpression(this, $it ->  ((Platform)$it).updateBaseUrl(baseUrl));
    }

    public SchoolTypeListExpression<T, U, SchoolType> getSchoolTypeList(){
        return new SchoolTypeListExpression(loaded("school_type_list", "getSchoolTypeList", Platform::getSchoolTypeList));
    }
    public SchoolListExpression<T, U, School> getSchoolList(){
        return new SchoolListExpression(loaded("school_list", "getSchoolList", Platform::getSchoolList));
    }
    public PlatformExpression<T, U, U> addSchoolType(SchoolType schoolType){
       return new PlatformExpression(this, $it ->  ((Platform)$it).addSchoolType(schoolType));
    }
    public PlatformExpression<T, U, U> addSchool(School school){
       return new PlatformExpression(this, $it ->  ((Platform)$it).addSchool(school));
    }
}