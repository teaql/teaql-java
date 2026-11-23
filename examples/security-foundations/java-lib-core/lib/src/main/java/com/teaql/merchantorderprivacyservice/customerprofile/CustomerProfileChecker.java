
package com.teaql.merchantorderprivacyservice.customerprofile;

import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrder;
import com.teaql.merchantorderprivacyservice.customerorder.CustomerOrderChecker;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
import com.teaql.merchantorderprivacyservice.merchant.MerchantChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class CustomerProfileChecker implements Checker<CustomerProfile>{

    public String type(){
        return CustomerProfile.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, CustomerProfile customerProfile, ObjectLocation _parentLocation){
        if(needCheck(_context, customerProfile)){
            markAsChecked(_context, customerProfile);
            doCheck(_context, customerProfile, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, CustomerProfile customerProfile, ObjectLocation _parentLocation){
      if((customerProfile == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, customerProfile);
      if(customerProfile.needPersist()){
      }
      if(customerProfile.newItem()){
      }else if(customerProfile.updateItem()){
        if(!customerProfile.isPropertyLoaded("merchant")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "merchant"), "Mutation requires a fully loaded entity");
        }
        if(!customerProfile.isPropertyLoaded("email")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "email"), "Mutation requires a fully loaded entity");
        }

      }
      checkMerchant(_context, customerProfile.getProperty(CustomerProfile.MERCHANT_PROPERTY), newLocation(_parentLocation, "merchant"));
      checkEmail(_context, customerProfile.getProperty(CustomerProfile.EMAIL_PROPERTY), newLocation(_parentLocation, "email"));
      for(int i = 0; customerProfile.getCustomerOrderList() != null && i < customerProfile.getCustomerOrderList().size(); i++){
         CustomerOrder customerOrder = customerProfile.getCustomerOrderList().get(i);
         new CustomerOrderChecker().checkAndFix(_context, customerOrder, newLocation(_parentLocation, "customer_order_list", i));
      }
    }

    public void checkMerchant(UserContext _context, Merchant merchant, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, merchant);
    if((merchant == null)){
        return;
    }
    new MerchantChecker().checkAndFix(_context, merchant, _parentLocation);
    }
    public void checkEmail(UserContext _context, String email, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, email);
    if((email == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, email);

    }
}