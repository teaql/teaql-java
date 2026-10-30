
package com.teaql.checkerfixadvanced.orderitem;

import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderChecker;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import com.teaql.checkerfixadvanced.tenant.TenantChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
      if(orderItem.needPersist()){
        verifyContextRoot(_context, orderItem, "tenant", "Tenant", newLocation(_parentLocation, "tenant"));

      }
      if(orderItem.newItem()){
        if(orderItem.getTenant() == null){
           orderItem.updateTenant(Tenant.refer(_context.requireActiveRoot("Tenant").id()));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "tenant", io.teaql.core.checker.FixEvidence.Source.CONTEXT, "activeRoot:Tenant"));
        }else{
           _context.verifyActiveRoot("Tenant", orderItem.getTenant().getId());
        }
        if(orderItem.getCreateTime() == null){
           orderItem.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }if(orderItem.getUpdateTime() == null){
           orderItem.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }
      }else if(orderItem.updateItem()){
        orderItem.updateUpdateTime(_context.evaluate("now"));
        _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        if(!orderItem.isPropertyLoaded("purchase_order")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "purchase_order"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("tenant")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "tenant"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("product_name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "product_name"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("unit_price")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "unit_price"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }
        if(!orderItem.isPropertyLoaded("update_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkPurchaseOrder(_context, orderItem.getProperty(OrderItem.PURCHASE_ORDER_PROPERTY), newLocation(_parentLocation, "purchase_order"));
      checkTenant(_context, orderItem.getProperty(OrderItem.TENANT_PROPERTY), newLocation(_parentLocation, "tenant"));
      checkProductName(_context, orderItem.getProperty(OrderItem.PRODUCT_NAME_PROPERTY), newLocation(_parentLocation, "product_name"));
      checkUnitPrice(_context, orderItem.getProperty(OrderItem.UNIT_PRICE_PROPERTY), newLocation(_parentLocation, "unit_price"));
      checkCreateTime(_context, orderItem.getProperty(OrderItem.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, orderItem.getProperty(OrderItem.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
    }

    public void checkPurchaseOrder(UserContext _context, PurchaseOrder purchaseOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, purchaseOrder);
    if((purchaseOrder == null)){
        return;
    }
    new PurchaseOrderChecker().checkAndFix(_context, purchaseOrder, _parentLocation);
    }
    public void checkTenant(UserContext _context, Tenant tenant, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, tenant);
    if((tenant == null)){
        return;
    }
    new TenantChecker().checkAndFix(_context, tenant, _parentLocation);
    }
    public void checkProductName(UserContext _context, String productName, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, productName);
    if((productName == null)){
        return;
    }
    minStringCheck(_context, _parentLocation, 1, productName);
    maxStringCheck(_context, _parentLocation, 80, productName);

    }
    public void checkUnitPrice(UserContext _context, BigDecimal unitPrice, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, unitPrice);
    if((unitPrice == null)){
        return;
    }
    minNumberCheck(_context, _parentLocation, 0.01, unitPrice);
    maxNumberCheck(_context, _parentLocation, 1000000, unitPrice);

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