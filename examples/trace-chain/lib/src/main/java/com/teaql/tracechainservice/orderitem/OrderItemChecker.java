
package com.teaql.tracechainservice.orderitem;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class OrderItemChecker implements Checker<OrderItem>{

    public String type(){
        return OrderItem.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, OrderItem orderItem, ObjectLocation _parentLocation){
        if(needCheck(_context, orderItem)){
            markAsChecked(_context, orderItem);
            doCheck(_context, orderItem, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, OrderItem orderItem, ObjectLocation _parentLocation){
      if((orderItem == null)){
         return;
      }
      if(orderItem.newItem()){
      }else if(orderItem.updateItem()){
        if(!orderItem.isPropertyLoaded("customerOrder")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "customer_order"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkCustomerOrder(_context, orderItem.getProperty(OrderItem.CUSTOMER_ORDER_PROPERTY), newLocation(_parentLocation, "customer_order"));
      checkName(_context, orderItem.getProperty(OrderItem.NAME_PROPERTY), newLocation(_parentLocation, "name"));
    }

    public void checkCustomerOrder(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customerOrder);
    if((customerOrder == null)){
        return;
    }
    new CustomerOrderChecker().checkAndFix(_context, customerOrder, _parentLocation);
    }
    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
}