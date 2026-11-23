
package com.teaql.ordereditreference.salesorder;

import com.teaql.ordereditreference.merchant.Merchant;
import com.teaql.ordereditreference.merchant.MerchantChecker;
import com.teaql.ordereditreference.orderitem.OrderItem;
import com.teaql.ordereditreference.orderitem.OrderItemChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class SalesOrderChecker implements Checker<SalesOrder>{

    public String type(){
        return SalesOrder.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, SalesOrder salesOrder, ObjectLocation _parentLocation){
        if(needCheck(_context, salesOrder)){
            markAsChecked(_context, salesOrder);
            doCheck(_context, salesOrder, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, SalesOrder salesOrder, ObjectLocation _parentLocation){
      if((salesOrder == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, salesOrder);
      if(salesOrder.needPersist()){
      }
      if(salesOrder.newItem()){
      }else if(salesOrder.updateItem()){
        if(!salesOrder.isPropertyLoaded("merchant")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "merchant"), "Mutation requires a fully loaded entity");
        }
        if(!salesOrder.isPropertyLoaded("order_number")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), "Mutation requires a fully loaded entity");
        }

      }
      checkMerchant(_context, salesOrder.getProperty(SalesOrder.MERCHANT_PROPERTY), newLocation(_parentLocation, "merchant"));
      checkOrderNumber(_context, salesOrder.getProperty(SalesOrder.ORDER_NUMBER_PROPERTY), newLocation(_parentLocation, "order_number"));
      for(int i = 0; salesOrder.getOrderItemList() != null && i < salesOrder.getOrderItemList().size(); i++){
         OrderItem orderItem = salesOrder.getOrderItemList().get(i);
         new OrderItemChecker().checkAndFix(_context, orderItem, newLocation(_parentLocation, "order_item_list", i));
      }
    }

    public void checkMerchant(UserContext _context, Merchant merchant, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, merchant);
    if((merchant == null)){
        return;
    }
    new MerchantChecker().checkAndFix(_context, merchant, _parentLocation);
    }
    public void checkOrderNumber(UserContext _context, String orderNumber, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, orderNumber);
    if((orderNumber == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, orderNumber);

    }
}