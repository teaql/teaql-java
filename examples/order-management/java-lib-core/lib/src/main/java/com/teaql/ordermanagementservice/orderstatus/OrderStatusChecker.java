
package com.teaql.ordermanagementservice.orderstatus;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformChecker;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;

public class OrderStatusChecker implements Checker<OrderStatus>{

    public String type(){
        return OrderStatus.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _ctx, OrderStatus orderStatus, ObjectLocation _parentLocation){
        if(needCheck(_ctx, orderStatus)){
            markAsChecked(_ctx, orderStatus);
            doCheck(_ctx, orderStatus, _parentLocation);
        }
    }

    public void doCheck(UserContext _ctx, OrderStatus orderStatus, ObjectLocation _parentLocation){
      if((orderStatus == null)){
         return;
      }
      if(orderStatus.newItem()){
      }else if(orderStatus.updateItem()){
      }
      checkName(_ctx, orderStatus.getProperty(OrderStatus.NAME_PROPERTY), newLocation(_parentLocation, OrderStatus.NAME_PROPERTY));
      checkCode(_ctx, orderStatus.getProperty(OrderStatus.CODE_PROPERTY), newLocation(_parentLocation, OrderStatus.CODE_PROPERTY));
      checkColor(_ctx, orderStatus.getProperty(OrderStatus.COLOR_PROPERTY), newLocation(_parentLocation, OrderStatus.COLOR_PROPERTY));
      checkDisplayOrder(_ctx, orderStatus.getProperty(OrderStatus.DISPLAY_ORDER_PROPERTY), newLocation(_parentLocation, OrderStatus.DISPLAY_ORDER_PROPERTY));
      checkCommercePlatform(_ctx, orderStatus.getProperty(OrderStatus.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, OrderStatus.COMMERCE_PLATFORM_PROPERTY));
      for(int i = 0; orderStatus.getCustomerOrderList() != null && i < orderStatus.getCustomerOrderList().size(); i++){
         CustomerOrder customerOrder = orderStatus.getCustomerOrderList().get(i);
         new CustomerOrderChecker().checkAndFix(_ctx, customerOrder, newLocation(_parentLocation, OrderStatus.CUSTOMER_ORDER_LIST_PROPERTY, i));
      }
    }

    public void checkName(UserContext _ctx, String name, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, name);

    }
    public void checkCode(UserContext _ctx, String code, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, code);
    if((code == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, code);

    }
    public void checkColor(UserContext _ctx, String color, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, color);
    if((color == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, color);

    }
    public void checkDisplayOrder(UserContext _ctx, BigDecimal displayOrder, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, displayOrder);
    if((displayOrder == null)){
        return;
    }
    }
    public void checkCommercePlatform(UserContext _ctx, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, commercePlatform);
    if((commercePlatform == null)){
        return;
    }
    new CommercePlatformChecker().checkAndFix(_ctx, commercePlatform, _parentLocation);
    }
}