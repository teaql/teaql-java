
package com.example.requesttreeorderservice.customerorder;

import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.customer.CustomerChecker;
import com.example.requesttreeorderservice.orderitem.OrderItem;
import com.example.requesttreeorderservice.orderitem.OrderItemChecker;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import com.example.requesttreeorderservice.orderstatus.OrderStatusChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;
import java.time.LocalDate;

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
      if(customerOrder.newItem()){
      }else if(customerOrder.updateItem()){
        if(!customerOrder.isPropertyLoaded("customer")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "customer"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("status")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "status"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("orderNumber")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("orderDate")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_date"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("totalAmount")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "total_amount"), "Mutation requires a fully loaded entity");
        }

      }
      checkCustomer(_context, customerOrder.getProperty(CustomerOrder.CUSTOMER_PROPERTY), newLocation(_parentLocation, "customer"));
      checkStatus(_context, customerOrder.getProperty(CustomerOrder.STATUS_PROPERTY), newLocation(_parentLocation, "status"));
      checkOrderNumber(_context, customerOrder.getProperty(CustomerOrder.ORDER_NUMBER_PROPERTY), newLocation(_parentLocation, "order_number"));
      checkOrderDate(_context, customerOrder.getProperty(CustomerOrder.ORDER_DATE_PROPERTY), newLocation(_parentLocation, "order_date"));
      checkTotalAmount(_context, customerOrder.getProperty(CustomerOrder.TOTAL_AMOUNT_PROPERTY), newLocation(_parentLocation, "total_amount"));
      for(int i = 0; customerOrder.getOrderItemList() != null && i < customerOrder.getOrderItemList().size(); i++){
         OrderItem orderItem = customerOrder.getOrderItemList().get(i);
         new OrderItemChecker().checkAndFix(_context, orderItem, newLocation(_parentLocation, "order_item_list", i));
      }
    }

    public void checkCustomer(UserContext _context, Customer customer, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customer);
    if((customer == null)){
        return;
    }
    new CustomerChecker().checkAndFix(_context, customer, _parentLocation);
    }
    public void checkStatus(UserContext _context, OrderStatus status, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, status);
    if((status == null)){
        return;
    }
    new OrderStatusChecker().checkAndFix(_context, status, _parentLocation);
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
}