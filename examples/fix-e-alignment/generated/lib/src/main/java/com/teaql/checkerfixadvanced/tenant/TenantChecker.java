
package com.teaql.checkerfixadvanced.tenant;

import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.orderitem.OrderItemChecker;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class TenantChecker implements Checker<Tenant>{

    public String type(){
        return Tenant.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Tenant tenant, ObjectLocation _parentLocation){
        if(needCheck(_context, tenant)){
            markAsChecked(_context, tenant);
            doCheck(_context, tenant, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Tenant tenant, ObjectLocation _parentLocation){
      if((tenant == null)){
         return;
      }
      if(tenant.newItem()){
      }else if(tenant.updateItem()){
        if(!tenant.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, tenant.getProperty(Tenant.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; tenant.getPurchaseOrderList() != null && i < tenant.getPurchaseOrderList().size(); i++){
         PurchaseOrder purchaseOrder = tenant.getPurchaseOrderList().get(i);
         new PurchaseOrderChecker().checkAndFix(_context, purchaseOrder, newLocation(_parentLocation, "purchase_order_list", i));
      }
      for(int i = 0; tenant.getOrderItemList() != null && i < tenant.getOrderItemList().size(); i++){
         OrderItem orderItem = tenant.getOrderItemList().get(i);
         new OrderItemChecker().checkAndFix(_context, orderItem, newLocation(_parentLocation, "order_item_list", i));
      }
    }

    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    minStringCheck(_context, _parentLocation, 1, name);
    maxStringCheck(_context, _parentLocation, 80, name);

    }
}