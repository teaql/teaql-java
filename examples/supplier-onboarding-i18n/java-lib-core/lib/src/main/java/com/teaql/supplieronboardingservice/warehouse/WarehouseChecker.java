
package com.teaql.supplieronboardingservice.warehouse;

import com.teaql.supplieronboardingservice.supplier.Supplier;
import com.teaql.supplieronboardingservice.supplier.SupplierChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class WarehouseChecker implements Checker<Warehouse>{

    public String type(){
        return Warehouse.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Warehouse warehouse, ObjectLocation _parentLocation){
        if(needCheck(_context, warehouse)){
            markAsChecked(_context, warehouse);
            doCheck(_context, warehouse, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Warehouse warehouse, ObjectLocation _parentLocation){
      if((warehouse == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, warehouse);
      if(warehouse.needPersist()){
      }
      if(warehouse.newItem()){
      }else if(warehouse.updateItem()){
        if(!warehouse.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, warehouse.getProperty(Warehouse.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; warehouse.getSupplierList() != null && i < warehouse.getSupplierList().size(); i++){
         Supplier supplier = warehouse.getSupplierList().get(i);
         new SupplierChecker().checkAndFix(_context, supplier, newLocation(_parentLocation, "supplier_list", i));
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