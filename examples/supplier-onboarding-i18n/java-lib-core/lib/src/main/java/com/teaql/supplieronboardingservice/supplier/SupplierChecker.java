
package com.teaql.supplieronboardingservice.supplier;

import com.teaql.supplieronboardingservice.warehouse.Warehouse;
import com.teaql.supplieronboardingservice.warehouse.WarehouseChecker;
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
        if(!supplier.isPropertyLoaded("warehouse")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "warehouse"), "Mutation requires a fully loaded entity");
        }
        if(!supplier.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }
        if(!supplier.isPropertyLoaded("minimum_order_quantity")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "minimum_order_quantity"), "Mutation requires a fully loaded entity");
        }

      }
      checkWarehouse(_context, supplier.getProperty(Supplier.WAREHOUSE_PROPERTY), newLocation(_parentLocation, "warehouse"));
      checkName(_context, supplier.getProperty(Supplier.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      checkMinimumOrderQuantity(_context, supplier.getProperty(Supplier.MINIMUM_ORDER_QUANTITY_PROPERTY), newLocation(_parentLocation, "minimum_order_quantity"));
    }

    public void checkWarehouse(UserContext _context, Warehouse warehouse, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, warehouse);
    if((warehouse == null)){
        return;
    }
    new WarehouseChecker().checkAndFix(_context, warehouse, _parentLocation);
    }
    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    minStringCheck(_context, _parentLocation, 1, name);
    maxStringCheck(_context, _parentLocation, 80, name);

    }
    public void checkMinimumOrderQuantity(UserContext _context, Integer minimumOrderQuantity, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, minimumOrderQuantity);
    if((minimumOrderQuantity == null)){
        return;
    }
    minNumberCheck(_context, _parentLocation, 10, minimumOrderQuantity);
    maxNumberCheck(_context, _parentLocation, 10000, minimumOrderQuantity);

    }
}