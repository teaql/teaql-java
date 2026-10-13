
package com.example.requesttreeorderservice.platform;

import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.customer.CustomerChecker;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import com.example.requesttreeorderservice.orderstatus.OrderStatusChecker;
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
      if(platform.newItem()){
      }else if(platform.updateItem()){
        if(!platform.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkName(_context, platform.getProperty(Platform.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; platform.getOrderStatusList() != null && i < platform.getOrderStatusList().size(); i++){
         OrderStatus orderStatus = platform.getOrderStatusList().get(i);
         new OrderStatusChecker().checkAndFix(_context, orderStatus, newLocation(_parentLocation, "order_status_list", i));
      }
      for(int i = 0; platform.getCustomerList() != null && i < platform.getCustomerList().size(); i++){
         Customer customer = platform.getCustomerList().get(i);
         new CustomerChecker().checkAndFix(_context, customer, newLocation(_parentLocation, "customer_list", i));
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