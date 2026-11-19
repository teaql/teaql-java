
package com.teaql.ordermanagementservice.customerorder;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformChecker;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customer.CustomerChecker;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineChecker;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import com.teaql.ordermanagementservice.orderstatus.OrderStatusChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerOrderChecker implements Checker<CustomerOrder>{

    public String type(){
        return CustomerOrder.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
        if(needCheck(_context, customerOrder)){
            markAsChecked(_context, customerOrder);
            doCheck(_context, customerOrder, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
      if((customerOrder == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, customerOrder);
      if(customerOrder.needPersist()){
      }
      if(customerOrder.newItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "create_time"), () -> {
           customerOrder.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "update_time"), () -> {
           customerOrder.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
      }else if(customerOrder.updateItem()){
        if(_contextFixEligible){
           applyContextFix(_context, newLocation(_parentLocation, "update_time"), () -> {
           customerOrder.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time",
              "now".equalsIgnoreCase("now") ? io.teaql.core.checker.FixEvidence.Source.CLOCK : io.teaql.core.checker.FixEvidence.Source.CONTEXT,
              "now".equalsIgnoreCase("now") ? "graphClock" : "contextFunction:now"));
           });
        }
        if(!customerOrder.isPropertyLoaded("order_number")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("order_date")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_date"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("total_amount")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "total_amount"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("status")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "status"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("customer")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "customer"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("commerce_platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "commerce_platform"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("update_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkOrderNumber(_context, customerOrder.getProperty(CustomerOrder.ORDER_NUMBER_PROPERTY), newLocation(_parentLocation, "order_number"));
      checkOrderDate(_context, customerOrder.getProperty(CustomerOrder.ORDER_DATE_PROPERTY), newLocation(_parentLocation, "order_date"));
      checkTotalAmount(_context, customerOrder.getProperty(CustomerOrder.TOTAL_AMOUNT_PROPERTY), newLocation(_parentLocation, "total_amount"));
      checkStatus(_context, customerOrder.getProperty(CustomerOrder.STATUS_PROPERTY), newLocation(_parentLocation, "status"));
      checkCustomer(_context, customerOrder.getProperty(CustomerOrder.CUSTOMER_PROPERTY), newLocation(_parentLocation, "customer"));
      checkCommercePlatform(_context, customerOrder.getProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, "commerce_platform"));
      checkCreateTime(_context, customerOrder.getProperty(CustomerOrder.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, customerOrder.getProperty(CustomerOrder.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
      for(int i = 0; customerOrder.getOrderLineList() != null && i < customerOrder.getOrderLineList().size(); i++){
         OrderLine orderLine = customerOrder.getOrderLineList().get(i);
         new OrderLineChecker().checkAndFix(_context, orderLine, newLocation(_parentLocation, "order_line_list", i));
      }
    }

    public void checkOrderNumber(UserContext _context, String orderNumber, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, orderNumber);
    if((orderNumber == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, orderNumber);

    }
    public void checkOrderDate(UserContext _context, LocalDate orderDate, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, orderDate);
    if((orderDate == null)){
        return;
    }
    }
    public void checkTotalAmount(UserContext _context, BigDecimal totalAmount, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, totalAmount);
    if((totalAmount == null)){
        return;
    }
    }
    public void checkStatus(UserContext _context, OrderStatus status, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, status);
    if((status == null)){
        return;
    }
    new OrderStatusChecker().checkAndFix(_context, status, _parentLocation);
    }
    public void checkCustomer(UserContext _context, Customer customer, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customer);
    if((customer == null)){
        return;
    }
    new CustomerChecker().checkAndFix(_context, customer, _parentLocation);
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