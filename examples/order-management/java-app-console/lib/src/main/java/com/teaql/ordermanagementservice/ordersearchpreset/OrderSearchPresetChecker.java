
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

    public void checkAndFix(UserContext _context, OrderSearchPreset orderSearchPreset, ObjectLocation _parentLocation){
        if(needCheck(_context, orderSearchPreset)){
            markAsChecked(_context, orderSearchPreset);
            doCheck(_context, orderSearchPreset, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, OrderSearchPreset orderSearchPreset, ObjectLocation _parentLocation){
      if((orderSearchPreset == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, orderSearchPreset);
      if(orderSearchPreset.needPersist()){
      }
      if(orderSearchPreset.newItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "create_time"), () -> {
           orderSearchPreset.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "update_time"), () -> {
           orderSearchPreset.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
      }else if(orderSearchPreset.updateItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "update_time"), () -> {
           orderSearchPreset.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
        if(!orderSearchPreset.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!orderSearchPreset.isPropertyLoaded("filter_json")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "filter_json"), "Mutation requires a fully loaded entity");
        }
        if(!orderSearchPreset.isPropertyLoaded("request_id")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "request_id"), "Mutation requires a fully loaded entity");
        }
        if(!orderSearchPreset.isPropertyLoaded("owner_user_id")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "owner_user_id"), "Mutation requires a fully loaded entity");
        }
        if(!orderSearchPreset.isPropertyLoaded("commerce_platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "commerce_platform"), "Mutation requires a fully loaded entity");
        }
        if(!orderSearchPreset.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }
        if(!orderSearchPreset.isPropertyLoaded("update_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, orderSearchPreset.getProperty(OrderSearchPreset.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkFilterJson(_context, orderSearchPreset.getProperty(OrderSearchPreset.FILTER_JSON_PROPERTY), newLocation(_parentLocation, "filter_json"));
      checkRequestId(_context, orderSearchPreset.getProperty(OrderSearchPreset.REQUEST_ID_PROPERTY), newLocation(_parentLocation, "request_id"));
      checkOwnerUserId(_context, orderSearchPreset.getProperty(OrderSearchPreset.OWNER_USER_ID_PROPERTY), newLocation(_parentLocation, "owner_user_id"));
      checkCommercePlatform(_context, orderSearchPreset.getProperty(OrderSearchPreset.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, "commerce_platform"));
      checkCreateTime(_context, orderSearchPreset.getProperty(OrderSearchPreset.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, orderSearchPreset.getProperty(OrderSearchPreset.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
    }

    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
    public void checkFilterJson(UserContext _context, String filterJson, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, filterJson);
    if((filterJson == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, filterJson);

    }
    public void checkRequestId(UserContext _context, String requestId, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, requestId);
    if((requestId == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, requestId);

    }
    public void checkOwnerUserId(UserContext _context, String ownerUserId, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, ownerUserId);
    if((ownerUserId == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, ownerUserId);

    }
    public void checkCommercePlatform(UserContext _context, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, commercePlatform);
    if((commercePlatform == null)){
        return;
    }
    new CommercePlatformChecker().checkAndFix(_context, commercePlatform, _parentLocation);
    }
    public void checkCreateTime(UserContext _context, LocalDateTime createTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, createTime);
    if((createTime == null)){
        return;
    }
    }
    public void checkUpdateTime(UserContext _context, LocalDateTime updateTime, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, updateTime);
    if((updateTime == null)){
        return;
    }
    }
}