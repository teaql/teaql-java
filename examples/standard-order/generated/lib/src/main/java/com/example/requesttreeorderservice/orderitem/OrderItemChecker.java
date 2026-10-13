
package com.example.requesttreeorderservice.orderitem;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;

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
        if(!orderItem.isPropertyLoaded("productName")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "product_name"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("quantity")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "quantity"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("unitPrice")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "unit_price"), "Mutation requires a fully loaded entity");
        }

      }
      checkCustomerOrder(_context, orderItem.getProperty(OrderItem.CUSTOMER_ORDER_PROPERTY), newLocation(_parentLocation, "customer_order"));
      checkProductName(_context, orderItem.getProperty(OrderItem.PRODUCT_NAME_PROPERTY), newLocation(_parentLocation, "product_name"));
      checkQuantity(_context, orderItem.getProperty(OrderItem.QUANTITY_PROPERTY), newLocation(_parentLocation, "quantity"));
      checkUnitPrice(_context, orderItem.getProperty(OrderItem.UNIT_PRICE_PROPERTY), newLocation(_parentLocation, "unit_price"));
    }

    public void checkCustomerOrder(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customerOrder);
    if((customerOrder == null)){
        return;
    }
    new CustomerOrderChecker().checkAndFix(_context, customerOrder, _parentLocation);
    }
    public void checkProductName(UserContext _context, String productName, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, productName);
    if((productName == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, productName);

    }
    public void checkQuantity(UserContext _context, Integer quantity, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, quantity);
    if((quantity == null)){
        return;
    }
    }
    public void checkUnitPrice(UserContext _context, BigDecimal unitPrice, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, unitPrice);
    if((unitPrice == null)){
        return;
    }
    }
}