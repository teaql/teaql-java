
package com.teaql.ordermanagementservice.ordersearchpreset;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDateTime;
import java.util.function.Function;

public class OrderSearchPresetExpression<T, E, U extends OrderSearchPreset> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public OrderSearchPresetExpression(Expression<T, U> expression){
        super(expression);
    }

    public OrderSearchPresetExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public OrderSearchPresetExpression<T, U, U> save(UserContext userContext){
        return new OrderSearchPresetExpression(this, $it -> ((OrderSearchPreset)$it).auditAs("Saved by Expression").save(userContext));
     }

     public OrderSearchPresetExpression<T, U, U> save(String intent, UserContext userContext){
        return new OrderSearchPresetExpression(this, $it -> ((OrderSearchPreset)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", OrderSearchPreset::getName);
    }
    public OrderSearchPresetExpression<T, U, U> updateName(String name){
       return new OrderSearchPresetExpression(this, $it ->  ((OrderSearchPreset)$it).updateName(name));
    }

    public Expression<T, String> getFilterJson(){
       return loaded("filter_json", "getFilterJson", OrderSearchPreset::getFilterJson);
    }
    public OrderSearchPresetExpression<T, U, U> updateFilterJson(String filterJson){
       return new OrderSearchPresetExpression(this, $it ->  ((OrderSearchPreset)$it).updateFilterJson(filterJson));
    }

    public Expression<T, String> getRequestId(){
       return loaded("request_id", "getRequestId", OrderSearchPreset::getRequestId);
    }
    public OrderSearchPresetExpression<T, U, U> updateRequestId(String requestId){
       return new OrderSearchPresetExpression(this, $it ->  ((OrderSearchPreset)$it).updateRequestId(requestId));
    }

    public Expression<T, String> getOwnerUserId(){
       return loaded("owner_user_id", "getOwnerUserId", OrderSearchPreset::getOwnerUserId);
    }
    public OrderSearchPresetExpression<T, U, U> updateOwnerUserId(String ownerUserId){
       return new OrderSearchPresetExpression(this, $it ->  ((OrderSearchPreset)$it).updateOwnerUserId(ownerUserId));
    }

    public CommercePlatformExpression<T, U, CommercePlatform> getCommercePlatform(){
       return new CommercePlatformExpression(loaded("commerce_platform", "getCommercePlatform", OrderSearchPreset::getCommercePlatform));
    }

    public OrderSearchPresetExpression<T, U, U> updateCommercePlatform(CommercePlatform commercePlatform){
       return new OrderSearchPresetExpression(this, $it ->  ((OrderSearchPreset)$it).updateCommercePlatform(commercePlatform));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("create_time", "getCreateTime", OrderSearchPreset::getCreateTime);
    }
    public OrderSearchPresetExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new OrderSearchPresetExpression(this, $it ->  ((OrderSearchPreset)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("update_time", "getUpdateTime", OrderSearchPreset::getUpdateTime);
    }
    public OrderSearchPresetExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new OrderSearchPresetExpression(this, $it ->  ((OrderSearchPreset)$it).updateUpdateTime(updateTime));
    }

}