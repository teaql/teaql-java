
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

    public void checkAndFix(UserContext _ctx, CustomerOrder customerOrder, ObjectLocation _parentLocation){
        if(needCheck(_ctx, customerOrder)){
            markAsChecked(_ctx, customerOrder);
            doCheck(_ctx, customerOrder, _parentLocation);
        }
    }

    public void doCheck(UserContext _ctx, CustomerOrder customerOrder, ObjectLocation _parentLocation){
      if((customerOrder == null)){
         return;
      }
      if(customerOrder.newItem()){
        if(customerOrder.getCreateTime() == null){
           customerOrder.updateCreateTime(java.time.LocalDateTime.now());
        }if(customerOrder.getUpdateTime() == null){
           customerOrder.updateUpdateTime(java.time.LocalDateTime.now());
        }
      }else if(customerOrder.updateItem()){
        customerOrder.updateUpdateTime(java.time.LocalDateTime.now());
      }
      checkOrderNumber(_ctx, customerOrder.getProperty(CustomerOrder.ORDER_NUMBER_PROPERTY), newLocation(_parentLocation, CustomerOrder.ORDER_NUMBER_PROPERTY));
      checkOrderDate(_ctx, customerOrder.getProperty(CustomerOrder.ORDER_DATE_PROPERTY), newLocation(_parentLocation, CustomerOrder.ORDER_DATE_PROPERTY));
      checkTotalAmount(_ctx, customerOrder.getProperty(CustomerOrder.TOTAL_AMOUNT_PROPERTY), newLocation(_parentLocation, CustomerOrder.TOTAL_AMOUNT_PROPERTY));
      checkStatus(_ctx, customerOrder.getProperty(CustomerOrder.STATUS_PROPERTY), newLocation(_parentLocation, CustomerOrder.STATUS_PROPERTY));
      checkCustomer(_ctx, customerOrder.getProperty(CustomerOrder.CUSTOMER_PROPERTY), newLocation(_parentLocation, CustomerOrder.CUSTOMER_PROPERTY));
      checkCommercePlatform(_ctx, customerOrder.getProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, CustomerOrder.COMMERCE_PLATFORM_PROPERTY));
      checkCreateTime(_ctx, customerOrder.getProperty(CustomerOrder.CREATE_TIME_PROPERTY), newLocation(_parentLocation, CustomerOrder.CREATE_TIME_PROPERTY));
      checkUpdateTime(_ctx, customerOrder.getProperty(CustomerOrder.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, CustomerOrder.UPDATE_TIME_PROPERTY));
      for(int i = 0; customerOrder.getOrderLineList() != null && i < customerOrder.getOrderLineList().size(); i++){
         OrderLine orderLine = customerOrder.getOrderLineList().get(i);
         new OrderLineChecker().checkAndFix(_ctx, orderLine, newLocation(_parentLocation, CustomerOrder.ORDER_LINE_LIST_PROPERTY, i));
      }
    }

    public void checkOrderNumber(UserContext _ctx, String orderNumber, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, orderNumber);
    if((orderNumber == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, orderNumber);

    }
    public void checkOrderDate(UserContext _ctx, LocalDate orderDate, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, orderDate);
    if((orderDate == null)){
        return;
    }
    }
    public void checkTotalAmount(UserContext _ctx, BigDecimal totalAmount, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, totalAmount);
    if((totalAmount == null)){
        return;
    }
    }
    public void checkStatus(UserContext _ctx, OrderStatus status, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, status);
    if((status == null)){
        return;
    }
    new OrderStatusChecker().checkAndFix(_ctx, status, _parentLocation);
    }
    public void checkCustomer(UserContext _ctx, Customer customer, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, customer);
    if((customer == null)){
        return;
    }
    new CustomerChecker().checkAndFix(_ctx, customer, _parentLocation);
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