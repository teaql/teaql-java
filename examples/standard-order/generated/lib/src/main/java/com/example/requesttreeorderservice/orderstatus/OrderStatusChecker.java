
package com.example.requesttreeorderservice.orderstatus;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderChecker;
import com.example.requesttreeorderservice.platform.Platform;
import com.example.requesttreeorderservice.platform.PlatformChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;

public class OrderStatusChecker implements Checker<OrderStatus>{

    public String type(){
        return OrderStatus.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, OrderStatus orderStatus, ObjectLocation _parentLocation){
        if(needCheck(_context, orderStatus)){
            markAsChecked(_context, orderStatus);
            doCheck(_context, orderStatus, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, OrderStatus orderStatus, ObjectLocation _parentLocation){
      if((orderStatus == null)){
         return;
      }
      if(orderStatus.newItem()){
      }else if(orderStatus.updateItem()){
        if(!orderStatus.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }
        if(!orderStatus.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!orderStatus.isPropertyLoaded("code")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "code"), "Mutation requires a fully loaded entity");
        }
        if(!orderStatus.isPropertyLoaded("color")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "color"), "Mutation requires a fully loaded entity");
        }
        if(!orderStatus.isPropertyLoaded("displayOrder")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "display_order"), "Mutation requires a fully loaded entity");
        }

      }
      checkPlatform(_context, orderStatus.getProperty(OrderStatus.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkName(_context, orderStatus.getProperty(OrderStatus.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkCode(_context, orderStatus.getProperty(OrderStatus.CODE_PROPERTY), newLocation(_parentLocation, "code"));
      checkColor(_context, orderStatus.getProperty(OrderStatus.COLOR_PROPERTY), newLocation(_parentLocation, "color"));
      checkDisplayOrder(_context, orderStatus.getProperty(OrderStatus.DISPLAY_ORDER_PROPERTY), newLocation(_parentLocation, "display_order"));
      for(int i = 0; orderStatus.getCustomerOrderList() != null && i < orderStatus.getCustomerOrderList().size(); i++){
         CustomerOrder customerOrder = orderStatus.getCustomerOrderList().get(i);
         new CustomerOrderChecker().checkAndFix(_context, customerOrder, newLocation(_parentLocation, "customer_order_list", i));
      }
    }

    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
    public void checkCode(UserContext _context, String code, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, code);
    if((code == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, code);

    }
    public void checkColor(UserContext _context, String color, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, color);
    if((color == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, color);

    }
    public void checkDisplayOrder(UserContext _context, BigDecimal displayOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, displayOrder);
    if((displayOrder == null)){
        return;
    }
    }
}