
package com.teaql.ordermanagementservice.ordersearchpreset;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDateTime;

public class OrderSearchPresetChecker implements Checker<OrderSearchPreset>{

    public String type(){
        return OrderSearchPreset.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _ctx, OrderSearchPreset orderSearchPreset, ObjectLocation _parentLocation){
        if(needCheck(_ctx, orderSearchPreset)){
            markAsChecked(_ctx, orderSearchPreset);
            doCheck(_ctx, orderSearchPreset, _parentLocation);
        }
    }

    public void doCheck(UserContext _ctx, OrderSearchPreset orderSearchPreset, ObjectLocation _parentLocation){
      if((orderSearchPreset == null)){
         return;
      }
      if(orderSearchPreset.newItem()){
        if(orderSearchPreset.getCreateTime() == null){
           orderSearchPreset.updateCreateTime(java.time.LocalDateTime.now());
        }if(orderSearchPreset.getUpdateTime() == null){
           orderSearchPreset.updateUpdateTime(java.time.LocalDateTime.now());
        }
      }else if(orderSearchPreset.updateItem()){
        orderSearchPreset.updateUpdateTime(java.time.LocalDateTime.now());
      }
      checkName(_ctx, orderSearchPreset.getProperty(OrderSearchPreset.NAME_PROPERTY), newLocation(_parentLocation, OrderSearchPreset.NAME_PROPERTY));
      checkFilterJson(_ctx, orderSearchPreset.getProperty(OrderSearchPreset.FILTER_JSON_PROPERTY), newLocation(_parentLocation, OrderSearchPreset.FILTER_JSON_PROPERTY));
      checkRequestId(_ctx, orderSearchPreset.getProperty(OrderSearchPreset.REQUEST_ID_PROPERTY), newLocation(_parentLocation, OrderSearchPreset.REQUEST_ID_PROPERTY));
      checkOwnerUserId(_ctx, orderSearchPreset.getProperty(OrderSearchPreset.OWNER_USER_ID_PROPERTY), newLocation(_parentLocation, OrderSearchPreset.OWNER_USER_ID_PROPERTY));
      checkCommercePlatform(_ctx, orderSearchPreset.getProperty(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY));
      checkCreateTime(_ctx, orderSearchPreset.getProperty(OrderSearchPreset.CREATE_TIME_PROPERTY), newLocation(_parentLocation, OrderSearchPreset.CREATE_TIME_PROPERTY));
      checkUpdateTime(_ctx, orderSearchPreset.getProperty(OrderSearchPreset.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, OrderSearchPreset.UPDATE_TIME_PROPERTY));
    }

    public void checkName(UserContext _ctx, String name, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, name);

    }
    public void checkFilterJson(UserContext _ctx, String filterJson, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, filterJson);
    if((filterJson == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, filterJson);

    }
    public void checkRequestId(UserContext _ctx, String requestId, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, requestId);
    if((requestId == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, requestId);

    }
    public void checkOwnerUserId(UserContext _ctx, String ownerUserId, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, ownerUserId);
    if((ownerUserId == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, ownerUserId);

    }
    public void checkCommercePlatform(UserContext _ctx, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, commercePlatform);
    if((commercePlatform == null)){
        return;
    }
    new CommercePlatformChecker().checkAndFix(_ctx, commercePlatform, _parentLocation);
    }
    public void checkCreateTime(UserContext _ctx, LocalDateTime createTime, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, createTime);
    if((createTime == null)){
        return;
    }
    }
    public void checkUpdateTime(UserContext _ctx, LocalDateTime updateTime, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, updateTime);
    if((updateTime == null)){
        return;
    }
    }
}