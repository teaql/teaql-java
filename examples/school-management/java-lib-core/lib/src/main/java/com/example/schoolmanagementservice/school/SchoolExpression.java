
package com.example.schoolmanagementservice.school;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformExpression;
import com.example.schoolmanagementservice.schooltype.SchoolType;
import com.example.schoolmanagementservice.schooltype.SchoolTypeExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;

public class SchoolExpression<T, E, U extends School> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public SchoolExpression(Expression<T, U> expression){
        super(expression);
    }

    public SchoolExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public SchoolExpression<T, U, U> save(UserContext userContext){
        return new SchoolExpression(this, $it -> ((School)$it).auditAs("Saved by Expression").save(userContext));
     }

     public SchoolExpression<T, U, U> save(String intent, UserContext userContext){
        return new SchoolExpression(this, $it -> ((School)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", "getPlatform", School::getPlatform));
    }

    public SchoolExpression<T, U, U> updatePlatform(Platform platform){
       return new SchoolExpression(this, $it ->  ((School)$it).updatePlatform(platform));
    }

    public SchoolTypeExpression<T, U, SchoolType> getSchoolType(){
       return new SchoolTypeExpression(loaded("school_type", "getSchoolType", School::getSchoolType));
    }

    public SchoolExpression<T, U, U> updateSchoolTypeToPrimary(){
       return new SchoolExpression(this, $it ->  ((School)$it).updateSchoolTypeToPrimary());
    }
    public SchoolExpression<T, U, U> updateSchoolTypeToSecondary(){
       return new SchoolExpression(this, $it ->  ((School)$it).updateSchoolTypeToSecondary());
    }

    public Expression<T, String> getName(){
       return loaded("name", "getName", School::getName);
    }
    public SchoolExpression<T, U, U> updateName(String name){
       return new SchoolExpression(this, $it ->  ((School)$it).updateName(name));
    }

    public Expression<T, String> getAddress(){
       return loaded("address", "getAddress", School::getAddress);
    }
    public SchoolExpression<T, U, U> updateAddress(String address){
       return new SchoolExpression(this, $it ->  ((School)$it).updateAddress(address));
    }

    public Expression<T, LocalDate> getEstablishedDate(){
       return loaded("established_date", "getEstablishedDate", School::getEstablishedDate);
    }
    public SchoolExpression<T, U, U> updateEstablishedDate(LocalDate establishedDate){
       return new SchoolExpression(this, $it ->  ((School)$it).updateEstablishedDate(establishedDate));
    }

    public Expression<T, Integer> getStudentCapacity(){
       return loaded("student_capacity", "getStudentCapacity", School::getStudentCapacity);
    }
    public SchoolExpression<T, U, U> updateStudentCapacity(Integer studentCapacity){
       return new SchoolExpression(this, $it ->  ((School)$it).updateStudentCapacity(studentCapacity));
    }

    public Expression<T, Boolean> isActive(){
       return loaded("active", "isActive", School::isActive);
    }
    public SchoolExpression<T, U, U> updateActive(Boolean active){
       return new SchoolExpression(this, $it ->  ((School)$it).updateActive(active));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("create_time", "getCreateTime", School::getCreateTime);
    }
    public SchoolExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new SchoolExpression(this, $it ->  ((School)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("update_time", "getUpdateTime", School::getUpdateTime);
    }
    public SchoolExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new SchoolExpression(this, $it ->  ((School)$it).updateUpdateTime(updateTime));
    }

}