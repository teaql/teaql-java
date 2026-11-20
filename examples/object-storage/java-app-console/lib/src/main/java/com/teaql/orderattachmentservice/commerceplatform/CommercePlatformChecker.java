
package com.teaql.orderattachmentservice.commerceplatform;

import com.teaql.orderattachmentservice.customerorder.CustomerOrder;
import com.teaql.orderattachmentservice.customerorder.CustomerOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class CommercePlatformChecker implements Checker<CommercePlatform>{

    public String type(){
        return CommercePlatform.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
        if(needCheck(_context, commercePlatform)){
            markAsChecked(_context, commercePlatform);
            doCheck(_context, commercePlatform, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
      if((commercePlatform == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, commercePlatform);
      if(commercePlatform.needPersist()){
      }
      if(commercePlatform.newItem()){
      }else if(commercePlatform.updateItem()){
        if(!commercePlatform.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, commercePlatform.getProperty(CommercePlatform.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; commercePlatform.getCustomerOrderList() != null && i < commercePlatform.getCustomerOrderList().size(); i++){
         CustomerOrder customerOrder = commercePlatform.getCustomerOrderList().get(i);
         new CustomerOrderChecker().checkAndFix(_context, customerOrder, newLocation(_parentLocation, "customer_order_list", i));
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