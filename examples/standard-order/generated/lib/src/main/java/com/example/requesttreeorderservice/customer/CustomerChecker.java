
package com.example.requesttreeorderservice.customer;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderChecker;
import com.example.requesttreeorderservice.platform.Platform;
import com.example.requesttreeorderservice.platform.PlatformChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class CustomerChecker implements Checker<Customer>{

    public String type(){
        return Customer.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, Customer customer, ObjectLocation _parentLocation){
        if(needCheck(_context, customer)){
            markAsChecked(_context, customer);
            doCheck(_context, customer, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, Customer customer, ObjectLocation _parentLocation){
      if((customer == null)){
         return;
      }
      if(customer.newItem()){
      }else if(customer.updateItem()){
        if(!customer.isPropertyLoaded("platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "platform"), "Mutation requires a fully loaded entity");
        }
        if(!customer.isPropertyLoaded("name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "name"), "Mutation requires a fully loaded entity");
        }

      }
      checkPlatform(_context, customer.getProperty(Customer.PLATFORM_PROPERTY), newLocation(_parentLocation, "platform"));
      checkName(_context, customer.getProperty(Customer.NAME_PROPERTY), newLocation(_parentLocation, "name"));
      for(int i = 0; customer.getCustomerOrderList() != null && i < customer.getCustomerOrderList().size(); i++){
         CustomerOrder customerOrder = customer.getCustomerOrderList().get(i);
         new CustomerOrderChecker().checkAndFix(_context, customerOrder, newLocation(_parentLocation, "customer_order_list", i));
      }
    }

    public void checkPlatform(UserContext _context, Platform platform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, platform);
    if((platform == null)){
        return;
    }
    new PlatformChecker().checkAndFix(_context, platform, _parentLocation);
    }
    public void checkName(UserContext _context, String name, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, name);

    }
}