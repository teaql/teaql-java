
package com.teaql.supplierordersyncservice.supplier;

import com.teaql.supplierordersyncservice.supplierorder.SupplierOrder;
import com.teaql.supplierordersyncservice.supplierorder.SupplierOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class SupplierChecker implements Checker<Supplier>{

    public String type(){
        return Supplier.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Supplier supplier, ObjectLocation _parentLocation){
        if(needCheck(_context, supplier)){
            markAsChecked(_context, supplier);
            doCheck(_context, supplier, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Supplier supplier, ObjectLocation _parentLocation){
      if((supplier == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, supplier);
      if(supplier.needPersist()){
      }
      if(supplier.newItem()){
      }else if(supplier.updateItem()){
        if(!supplier.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, supplier.getProperty(Supplier.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; supplier.getSupplierOrderList() != null && i < supplier.getSupplierOrderList().size(); i++){
         SupplierOrder supplierOrder = supplier.getSupplierOrderList().get(i);
         new SupplierOrderChecker().checkAndFix(_context, supplierOrder, newLocation(_parentLocation, "supplier_order_list", i));
      }
    }

    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
}