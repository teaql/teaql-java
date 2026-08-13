
package com.teaql.ordermanagementservice.commerceplatform;

import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customer.CustomerChecker;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderChecker;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineChecker;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPreset;
import com.teaql.ordermanagementservice.ordersearchpreset.OrderSearchPresetChecker;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import com.teaql.ordermanagementservice.orderstatus.OrderStatusChecker;
import com.teaql.ordermanagementservice.product.Product;
import com.teaql.ordermanagementservice.product.ProductChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDateTime;

public class CommercePlatformChecker implements Checker<CommercePlatform>{

    public String type(){
        return CommercePlatform.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _ctx, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
        if(needCheck(_ctx, commercePlatform)){
            markAsChecked(_ctx, commercePlatform);
            doCheck(_ctx, commercePlatform, _parentLocation);
        }
    }

    public void doCheck(UserContext _ctx, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
      if((commercePlatform == null)){
         return;
      }
      if(commercePlatform.newItem()){
        if(commercePlatform.getCreateTime() == null){
           commercePlatform.updateCreateTime(java.time.LocalDateTime.now());
        }if(commercePlatform.getUpdateTime() == null){
           commercePlatform.updateUpdateTime(java.time.LocalDateTime.now());
        }
      }else if(commercePlatform.updateItem()){
        commercePlatform.updateUpdateTime(java.time.LocalDateTime.now());
      }
      checkName(_ctx, commercePlatform.getProperty(CommercePlatform.NAME_PROPERTY), newLocation(_parentLocation, CommercePlatform.NAME_PROPERTY));
      checkCreateTime(_ctx, commercePlatform.getProperty(CommercePlatform.CREATE_TIME_PROPERTY), newLocation(_parentLocation, CommercePlatform.CREATE_TIME_PROPERTY));
      checkUpdateTime(_ctx, commercePlatform.getProperty(CommercePlatform.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, CommercePlatform.UPDATE_TIME_PROPERTY));
      for(int i = 0; commercePlatform.getCustomerList() != null && i < commercePlatform.getCustomerList().size(); i++){
         Customer customer = commercePlatform.getCustomerList().get(i);
         new CustomerChecker().checkAndFix(_ctx, customer, newLocation(_parentLocation, CommercePlatform.CUSTOMER_LIST_PROPERTY, i));
      }
      for(int i = 0; commercePlatform.getOrderStatusList() != null && i < commercePlatform.getOrderStatusList().size(); i++){
         OrderStatus orderStatus = commercePlatform.getOrderStatusList().get(i);
         new OrderStatusChecker().checkAndFix(_ctx, orderStatus, newLocation(_parentLocation, CommercePlatform.ORDER_STATUS_LIST_PROPERTY, i));
      }
      for(int i = 0; commercePlatform.getCustomerOrderList() != null && i < commercePlatform.getCustomerOrderList().size(); i++){
         CustomerOrder customerOrder = commercePlatform.getCustomerOrderList().get(i);
         new CustomerOrderChecker().checkAndFix(_ctx, customerOrder, newLocation(_parentLocation, CommercePlatform.CUSTOMER_ORDER_LIST_PROPERTY, i));
      }
      for(int i = 0; commercePlatform.getProductList() != null && i < commercePlatform.getProductList().size(); i++){
         Product product = commercePlatform.getProductList().get(i);
         new ProductChecker().checkAndFix(_ctx, product, newLocation(_parentLocation, CommercePlatform.PRODUCT_LIST_PROPERTY, i));
      }
      for(int i = 0; commercePlatform.getOrderLineList() != null && i < commercePlatform.getOrderLineList().size(); i++){
         OrderLine orderLine = commercePlatform.getOrderLineList().get(i);
         new OrderLineChecker().checkAndFix(_ctx, orderLine, newLocation(_parentLocation, CommercePlatform.ORDER_LINE_LIST_PROPERTY, i));
      }
      for(int i = 0; commercePlatform.getOrderSearchPresetList() != null && i < commercePlatform.getOrderSearchPresetList().size(); i++){
         OrderSearchPreset orderSearchPreset = commercePlatform.getOrderSearchPresetList().get(i);
         new OrderSearchPresetChecker().checkAndFix(_ctx, orderSearchPreset, newLocation(_parentLocation, CommercePlatform.ORDER_SEARCH_PRESET_LIST_PROPERTY, i));
      }
    }

    public void checkName(UserContext _ctx, String name, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, name);

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