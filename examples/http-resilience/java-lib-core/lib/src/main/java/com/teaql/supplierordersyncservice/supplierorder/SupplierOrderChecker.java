
package com.teaql.supplierordersyncservice.supplierorder;

import com.teaql.supplierordersyncservice.supplier.Supplier;
import com.teaql.supplierordersyncservice.supplier.SupplierChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class SupplierOrderChecker implements Checker<SupplierOrder>{

    public String type(){
        return SupplierOrder.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, SupplierOrder supplierOrder, ObjectLocation _parentLocation){
        if(needCheck(_context, supplierOrder)){
            markAsChecked(_context, supplierOrder);
            doCheck(_context, supplierOrder, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, SupplierOrder supplierOrder, ObjectLocation _parentLocation){
      if((supplierOrder == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, supplierOrder);
      if(supplierOrder.needPersist()){
      }
      if(supplierOrder.newItem()){
      }else if(supplierOrder.updateItem()){
        if(!supplierOrder.isPropertyLoaded("supplier")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "supplier"), "Mutation requires a fully loaded entity");
        }
        if(!supplierOrder.isPropertyLoaded("external_reference")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "external_reference"), "Mutation requires a fully loaded entity");
        }
        if(!supplierOrder.isPropertyLoaded("sync_status")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "sync_status"), "Mutation requires a fully loaded entity");
        }

      }
      checkSupplier(_context, supplierOrder.getProperty(SupplierOrder.SUPPLIER_PROPERTY), newLocation(_parentLocation, "supplier"));
      checkExternalReference(_context, supplierOrder.getProperty(SupplierOrder.EXTERNAL_REFERENCE_PROPERTY), newLocation(_parentLocation, "external_reference"));
      checkSyncStatus(_context, supplierOrder.getProperty(SupplierOrder.SYNC_STATUS_PROPERTY), newLocation(_parentLocation, "sync_status"));
    }

    public void checkSupplier(UserContext _context, Supplier supplier, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, supplier);
    if((supplier == null)){
        return;
    }
    new SupplierChecker().checkAndFix(_context, supplier, _parentLocation);
    }
    public void checkExternalReference(UserContext _context, String externalReference, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, externalReference);
    if((externalReference == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, externalReference);

    }
    public void checkSyncStatus(UserContext _context, String syncStatus, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, syncStatus);
    if((syncStatus == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, syncStatus);

    }
}