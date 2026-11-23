
package com.teaql.ordereditreference.orderitem;

import com.teaql.ordereditreference.salesorder.SalesOrder;
import com.teaql.ordereditreference.salesorder.SalesOrderChecker;
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
      boolean _contextFixEligible = needContextFix(_context, orderItem);
      if(orderItem.needPersist()){
      }
      if(orderItem.newItem()){
      }else if(orderItem.updateItem()){
        if(!orderItem.isPropertyLoaded("sales_order")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "sales_order"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("sku")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "sku"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("quantity")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "quantity"), "Mutation requires a fully loaded entity");
        }

      }
      checkSalesOrder(_context, orderItem.getProperty(OrderItem.SALES_ORDER_PROPERTY), newLocation(_parentLocation, "sales_order"));
      checkSku(_context, orderItem.getProperty(OrderItem.SKU_PROPERTY), newLocation(_parentLocation, "sku"));
      checkQuantity(_context, orderItem.getProperty(OrderItem.QUANTITY_PROPERTY), newLocation(_parentLocation, "quantity"));
    }

    public void checkSalesOrder(UserContext _context, SalesOrder salesOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, salesOrder);
    if((salesOrder == null)){
        return;
    }
    new SalesOrderChecker().checkAndFix(_context, salesOrder, _parentLocation);
    }
    public void checkSku(UserContext _context, String sku, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, sku);
    if((sku == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, sku);

    }
    public void checkQuantity(UserContext _context, Integer quantity, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, quantity);
    if((quantity == null)){
        return;
    }
    }
}