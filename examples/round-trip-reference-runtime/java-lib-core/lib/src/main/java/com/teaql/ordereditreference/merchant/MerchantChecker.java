
package com.teaql.ordereditreference.merchant;

import com.teaql.ordereditreference.salesorder.SalesOrder;
import com.teaql.ordereditreference.salesorder.SalesOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class MerchantChecker implements Checker<Merchant>{

    public String type(){
        return Merchant.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Merchant merchant, ObjectLocation _parentLocation){
        if(needCheck(_context, merchant)){
            markAsChecked(_context, merchant);
            doCheck(_context, merchant, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Merchant merchant, ObjectLocation _parentLocation){
      if((merchant == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, merchant);
      if(merchant.needPersist()){
      }
      if(merchant.newItem()){
      }else if(merchant.updateItem()){
        if(!merchant.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, merchant.getProperty(Merchant.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; merchant.getSalesOrderList() != null && i < merchant.getSalesOrderList().size(); i++){
         SalesOrder salesOrder = merchant.getSalesOrderList().get(i);
         new SalesOrderChecker().checkAndFix(_context, salesOrder, newLocation(_parentLocation, "sales_order_list", i));
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