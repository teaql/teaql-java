
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

    public void checkAndFix(UserContext _context, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
        if(needCheck(_context, commercePlatform)){
            markAsChecked(_context, commercePlatform);
            doCheck(_context, commercePlatform, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
      if((commercePlatform == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, commercePlatform);
      if(commercePlatform.needPersist()){
      }
      if(commercePlatform.newItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "create_time"), () -> {
           commercePlatform.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "update_time"), () -> {
           commercePlatform.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
      }else if(commercePlatform.updateItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "update_time"), () -> {
           commercePlatform.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
        if(!commercePlatform.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!commercePlatform.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }
        if(!commercePlatform.isPropertyLoaded("update_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, commercePlatform.getProperty(CommercePlatform.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkCreateTime(_context, commercePlatform.getProperty(CommercePlatform.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, commercePlatform.getProperty(CommercePlatform.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
      for(int i = 0; commercePlatform.getCustomerList() != null && i < commercePlatform.getCustomerList().size(); i++){
         Customer customer = commercePlatform.getCustomerList().get(i);
         new CustomerChecker().checkAndFix(_context, customer, newLocation(_parentLocation, "customer_list", i));
      }
      for(int i = 0; commercePlatform.getOrderStatusList() != null && i < commercePlatform.getOrderStatusList().size(); i++){
         OrderStatus orderStatus = commercePlatform.getOrderStatusList().get(i);
         new OrderStatusChecker().checkAndFix(_context, orderStatus, newLocation(_parentLocation, "order_status_list", i));
      }
      for(int i = 0; commercePlatform.getCustomerOrderList() != null && i < commercePlatform.getCustomerOrderList().size(); i++){
         CustomerOrder customerOrder = commercePlatform.getCustomerOrderList().get(i);
         new CustomerOrderChecker().checkAndFix(_context, customerOrder, newLocation(_parentLocation, "customer_order_list", i));
      }
      for(int i = 0; commercePlatform.getProductList() != null && i < commercePlatform.getProductList().size(); i++){
         Product product = commercePlatform.getProductList().get(i);
         new ProductChecker().checkAndFix(_context, product, newLocation(_parentLocation, "product_list", i));
      }
      for(int i = 0; commercePlatform.getOrderLineList() != null && i < commercePlatform.getOrderLineList().size(); i++){
         OrderLine orderLine = commercePlatform.getOrderLineList().get(i);
         new OrderLineChecker().checkAndFix(_context, orderLine, newLocation(_parentLocation, "order_line_list", i));
      }
      for(int i = 0; commercePlatform.getOrderSearchPresetList() != null && i < commercePlatform.getOrderSearchPresetList().size(); i++){
         OrderSearchPreset orderSearchPreset = commercePlatform.getOrderSearchPresetList().get(i);
         new OrderSearchPresetChecker().checkAndFix(_context, orderSearchPreset, newLocation(_parentLocation, "order_search_preset_list", i));
      }
    }

    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

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