
package com.teaql.purchaseordernumbering.platform;

import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrder;
import com.teaql.purchaseordernumbering.purchaseorder.PurchaseOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class PlatformChecker implements Checker<Platform>{

    public String type(){
        return Platform.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Platform platform, ObjectLocation _parentLocation){
        if(needCheck(_context, platform)){
            markAsChecked(_context, platform);
            doCheck(_context, platform, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Platform platform, ObjectLocation _parentLocation){
      if((platform == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, platform);
      if(platform.needPersist()){
      }
      if(platform.newItem()){
      }else if(platform.updateItem()){
        if(!platform.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, platform.getProperty(Platform.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; platform.getPurchaseOrderList() != null && i < platform.getPurchaseOrderList().size(); i++){
         PurchaseOrder purchaseOrder = platform.getPurchaseOrderList().get(i);
         new PurchaseOrderChecker().checkAndFix(_context, purchaseOrder, newLocation(_parentLocation, "purchase_order_list", i));
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