
package com.teaql.tracechainservice.shipment;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class ShipmentChecker implements Checker<Shipment>{

    public String type(){
        return Shipment.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Shipment shipment, ObjectLocation _parentLocation){
        if(needCheck(_context, shipment)){
            markAsChecked(_context, shipment);
            doCheck(_context, shipment, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Shipment shipment, ObjectLocation _parentLocation){
      if((shipment == null)){
         return;
      }
      if(shipment.newItem()){
      }else if(shipment.updateItem()){
        if(!shipment.isPropertyLoaded("customerOrder")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "customer_order"), "Mutation requires a fully loaded entity");
        }
        if(!shipment.isPropertyLoaded("referenceCode")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "reference_code"), "Mutation requires a fully loaded entity");
        }

      }
      checkCustomerOrder(_context, shipment.getProperty(Shipment.CUSTOMER_ORDER_PROPERTY), newLocation(_parentLocation, "customer_order"));
      checkReferenceCode(_context, shipment.getProperty(Shipment.REFERENCE_CODE_PROPERTY), newLocation(_parentLocation, "reference_code"));
    }

    public void checkCustomerOrder(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customerOrder);
    if((customerOrder == null)){
        return;
    }
    new CustomerOrderChecker().checkAndFix(_context, customerOrder, _parentLocation);
    }
    public void checkReferenceCode(UserContext _context, String referenceCode, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, referenceCode);
    if((referenceCode == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, referenceCode);

    }
}