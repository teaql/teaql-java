
package com.teaql.checkerfixadvanced.purchaseorder;

import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.orderitem.OrderItemChecker;
import com.teaql.checkerfixadvanced.tenant.Tenant;
import com.teaql.checkerfixadvanced.tenant.TenantChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDateTime;

public class PurchaseOrderChecker implements Checker<PurchaseOrder>{

    public String type(){
        return PurchaseOrder.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, PurchaseOrder purchaseOrder, ObjectLocation _parentLocation){
        if(needCheck(_context, purchaseOrder)){
            markAsChecked(_context, purchaseOrder);
            doCheck(_context, purchaseOrder, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, PurchaseOrder purchaseOrder, ObjectLocation _parentLocation){
      if((purchaseOrder == null)){
         return;
      }
      if(purchaseOrder.newItem()){
        if(purchaseOrder.getTenant() == null){
           purchaseOrder.updateTenant(Tenant.refer(_context.requireActiveRoot("Tenant").id()));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "tenant", io.teaql.core.checker.FixEvidence.Source.CONTEXT, "activeRoot:Tenant"));
        }else{
           _context.verifyActiveRoot("Tenant", purchaseOrder.getTenant().getId());
        }
        if(purchaseOrder.getCreateTime() == null){
           purchaseOrder.updateCreateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "create_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }if(purchaseOrder.getUpdateTime() == null){
           purchaseOrder.updateUpdateTime(_context.evaluate("now"));
           _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        }
      }else if(purchaseOrder.updateItem()){
        if(purchaseOrder.getTenant() != null){
           _context.verifyActiveRoot("Tenant", purchaseOrder.getTenant().getId());
        }
        purchaseOrder.updateUpdateTime(_context.evaluate("now"));
        _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "update_time", io.teaql.core.checker.FixEvidence.Source.CLOCK, "graphClock"));
        if(!purchaseOrder.isPropertyLoaded("tenant")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "tenant"), "Mutation requires a fully loaded entity");
        }
        if(!purchaseOrder.isPropertyLoaded("contact_phone")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "contact_phone"), "Mutation requires a fully loaded entity");
        }
        if(!purchaseOrder.isPropertyLoaded("internal_note")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "internal_note"), "Mutation requires a fully loaded entity");
        }
        if(!purchaseOrder.isPropertyLoaded("create_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "create_time"), "Mutation requires a fully loaded entity");
        }
        if(!purchaseOrder.isPropertyLoaded("update_time")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "update_time"), "Mutation requires a fully loaded entity");
        }

      }
      checkTenant(_context, purchaseOrder.getProperty(PurchaseOrder.TENANT_PROPERTY), newLocation(_parentLocation, "tenant"));
      checkContactPhone(_context, purchaseOrder.getProperty(PurchaseOrder.CONTACT_PHONE_PROPERTY), newLocation(_parentLocation, "contact_phone"));
      checkInternalNote(_context, purchaseOrder.getProperty(PurchaseOrder.INTERNAL_NOTE_PROPERTY), newLocation(_parentLocation, "internal_note"));
      checkCreateTime(_context, purchaseOrder.getProperty(PurchaseOrder.CREATE_TIME_PROPERTY), newLocation(_parentLocation, "create_time"));
      checkUpdateTime(_context, purchaseOrder.getProperty(PurchaseOrder.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, "update_time"));
      for(int i = 0; purchaseOrder.getOrderItemList() != null && i < purchaseOrder.getOrderItemList().size(); i++){
         OrderItem orderItem = purchaseOrder.getOrderItemList().get(i);
         new OrderItemChecker().checkAndFix(_context, orderItem, newLocation(_parentLocation, "order_item_list", i));
      }
    }

    public void checkTenant(UserContext _context, Tenant tenant, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, tenant);
    if((tenant == null)){
        return;
    }
    new TenantChecker().checkAndFix(_context, tenant, _parentLocation);
    }
    public void checkContactPhone(UserContext _context, String contactPhone, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, contactPhone);
    if((contactPhone == null)){
        return;
    }
    minStringCheck(_context, _parentLocation, 1, contactPhone);
    maxStringCheck(_context, _parentLocation, 32, contactPhone);

    }
    public void checkInternalNote(UserContext _context, String internalNote, ObjectLocation _parentLocation){
    if((internalNote == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, internalNote);

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