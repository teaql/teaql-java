
package com.example.schoolmanagementservice.schoolcapacitysummary;

import com.example.schoolmanagementservice.platform.Platform;
import com.example.schoolmanagementservice.platform.PlatformExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class SchoolCapacitySummaryExpression<T, E, U extends SchoolCapacitySummary> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public SchoolCapacitySummaryExpression(Expression<T, U> expression){
        super(expression);
    }

    public SchoolCapacitySummaryExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public SchoolCapacitySummaryExpression<T, U, U> updateId(Long id){
        return new SchoolCapacitySummaryExpression(this, $it -> {((SchoolCapacitySummary)$it).__internalSet("id", id); return this;});
     }

     public SchoolCapacitySummaryExpression<T, U, U> save(UserContext userContext){
        return new SchoolCapacitySummaryExpression(this, $it -> ((SchoolCapacitySummary)$it).auditAs("Saved by Expression").save(userContext));
     }

     public SchoolCapacitySummaryExpression<T, U, U> save(String intent, UserContext userContext){
        return new SchoolCapacitySummaryExpression(this, $it -> ((SchoolCapacitySummary)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", "getPlatform", SchoolCapacitySummary::getPlatform));
    }

    public SchoolCapacitySummaryExpression<T, U, U> updatePlatform(Platform platform){
       return new SchoolCapacitySummaryExpression(this, $it ->  ((SchoolCapacitySummary)$it).updatePlatform(platform));
    }

    public Expression<T, String> getName(){
       return loaded("name", "getName", SchoolCapacitySummary::getName);
    }
    public SchoolCapacitySummaryExpression<T, U, U> updateName(String name){
       return new SchoolCapacitySummaryExpression(this, $it ->  ((SchoolCapacitySummary)$it).updateName(name));
    }

    public Expression<T, Integer> getTotalCapacity(){
       return loaded("total_capacity", "getTotalCapacity", SchoolCapacitySummary::getTotalCapacity);
    }
    public SchoolCapacitySummaryExpression<T, U, U> updateTotalCapacity(Integer totalCapacity){
       return new SchoolCapacitySummaryExpression(this, $it ->  ((SchoolCapacitySummary)$it).updateTotalCapacity(totalCapacity));
    }

    public Expression<T, Integer> getSchoolCount(){
       return loaded("school_count", "getSchoolCount", SchoolCapacitySummary::getSchoolCount);
    }
    public SchoolCapacitySummaryExpression<T, U, U> updateSchoolCount(Integer schoolCount){
       return new SchoolCapacitySummaryExpression(this, $it ->  ((SchoolCapacitySummary)$it).updateSchoolCount(schoolCount));
    }

}