
package com.teaql.merchantorderprivacyservice.customerorder;

import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfile;
import com.teaql.merchantorderprivacyservice.customerprofile.CustomerProfileChecker;
import com.teaql.merchantorderprivacyservice.merchant.Merchant;
import com.teaql.merchantorderprivacyservice.merchant.MerchantChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class CustomerOrderChecker implements Checker<CustomerOrder>{

    public String type(){
        return CustomerOrder.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
        if(needCheck(_context, customerOrder)){
            markAsChecked(_context, customerOrder);
            doCheck(_context, customerOrder, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
      if((customerOrder == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, customerOrder);
      if(customerOrder.needPersist()){
      }
      if(customerOrder.newItem()){
      }else if(customerOrder.updateItem()){
        if(!customerOrder.isPropertyLoaded("merchant")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "merchant"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("customer_profile")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "customer_profile"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("order_number")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("fulfillment_status")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "fulfillment_status"), "Mutation requires a fully loaded entity");
        }

      }
      checkMerchant(_context, customerOrder.getProperty(CustomerOrder.MERCHANT_PROPERTY), newLocation(_parentLocation, "merchant"));
      checkCustomerProfile(_context, customerOrder.getProperty(CustomerOrder.CUSTOMER_PROFILE_PROPERTY), newLocation(_parentLocation, "customer_profile"));
      checkOrderNumber(_context, customerOrder.getProperty(CustomerOrder.ORDER_NUMBER_PROPERTY), newLocation(_parentLocation, "order_number"));
      checkFulfillmentStatus(_context, customerOrder.getProperty(CustomerOrder.FULFILLMENT_STATUS_PROPERTY), newLocation(_parentLocation, "fulfillment_status"));
    }

    public void checkMerchant(UserContext _context, Merchant merchant, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, merchant);
    if((merchant == null)){
        return;
    }
    new MerchantChecker().checkAndFix(_context, merchant, _parentLocation);
    }
    public void checkCustomerProfile(UserContext _context, CustomerProfile customerProfile, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customerProfile);
    if((customerProfile == null)){
        return;
    }
    new CustomerProfileChecker().checkAndFix(_context, customerProfile, _parentLocation);
    }
    public void checkOrderNumber(UserContext _context, String orderNumber, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, orderNumber);
    if((orderNumber == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, orderNumber);

    }
    public void checkFulfillmentStatus(UserContext _context, String fulfillmentStatus, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, fulfillmentStatus);
    if((fulfillmentStatus == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, fulfillmentStatus);

    }
}