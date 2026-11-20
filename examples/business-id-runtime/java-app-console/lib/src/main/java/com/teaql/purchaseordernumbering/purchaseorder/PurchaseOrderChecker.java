
package com.teaql.purchaseordernumbering.purchaseorder;

import com.teaql.purchaseordernumbering.platform.Platform;
import com.teaql.purchaseordernumbering.platform.PlatformChecker;
import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder.OrderNumber;
import io.teaql.core.UserContext;
import io.teaql.core.businessid.BusinessIdSlot;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

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
      boolean _contextFixEligible = needContextFix(_context, purchaseOrder);
      if(purchaseOrder.needPersist()){
      }
      if(purchaseOrder.newItem()){
        boolean orderNumberWasMissing = purchaseOrder.getOrderNumber() == null;
           try {
              _context.businessIds().ensure(
                  _context,
                  OrderNumber.definition(),
                  String.valueOf(_context.requireActiveRoot("Platform").id()),
                  type(),
                  new BusinessIdSlot() {
                     public String currentValue() {
                        OrderNumber current = purchaseOrder.getOrderNumber();
                        return current == null ? null : current.value();
                     }
                     public boolean newAggregate() { return true; }
                     public void assignCanonicalValue(String value) {
                        purchaseOrder.updateOrderNumber(OrderNumber.of(value));
                     }
                  });
              if (orderNumberWasMissing) {
                 _context.recordFixEvidence(new io.teaql.core.checker.FixEvidence(type(), "order_number", io.teaql.core.checker.FixEvidence.Source.CONTEXT, "businessIdAllocator"));
              }
           } catch (io.teaql.core.businessid.BusinessIdException error) {
              invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), error.getCode() + ": " + error.getMessage());
           }

      }else if(purchaseOrder.updateItem()){
        if(!purchaseOrder.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }
        if(!purchaseOrder.isPropertyLoaded("supplier")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "supplier"), "Mutation requires a fully loaded entity");
        }
        if(!purchaseOrder.isPropertyLoaded("order_number")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), "Mutation requires a fully loaded entity");
        }
        if(purchaseOrder.getUpdatedProperties().contains(PurchaseOrder.ORDER_NUMBER_PROPERTY)){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), "Business ID is immutable after create");
        }
      }
      checkPlatform(_context, purchaseOrder.getProperty(PurchaseOrder.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkSupplier(_context, purchaseOrder.getProperty(PurchaseOrder.SUPPLIER_PROPERTY), newLocation(_parentLocation, "supplier"));
      checkOrderNumber(_context, purchaseOrder.getOrderNumber(), newLocation(_parentLocation, "order_number"));
    }

    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
    public void checkSupplier(UserContext _context, String supplier, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, supplier);
    if((supplier == null)){
        return;
    }
    minStringCheck(_context, _parentLocation, 1, supplier);
    maxStringCheck(_context, _parentLocation, 80, supplier);

    }
    public void checkOrderNumber(UserContext _context, OrderNumber orderNumber, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, orderNumber);
    if((orderNumber == null)){
        return;
    }
    }
}