
package com.teaql.ordermanagementservice.customer;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformChecker;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;
import java.time.LocalDateTime;

public class CustomerChecker implements Checker<Customer>{

    public String type(){
        return Customer.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _ctx, Customer customer, ObjectLocation _parentLocation){
        if(needCheck(_ctx, customer)){
            markAsChecked(_ctx, customer);
            doCheck(_ctx, customer, _parentLocation);
        }
    }

    public void doCheck(UserContext _ctx, Customer customer, ObjectLocation _parentLocation){
      if((customer == null)){
         return;
      }
      if(customer.newItem()){
        if(customer.getCreateTime() == null){
           customer.updateCreateTime(java.time.LocalDateTime.now());
        }if(customer.getUpdateTime() == null){
           customer.updateUpdateTime(java.time.LocalDateTime.now());
        }
      }else if(customer.updateItem()){
        customer.updateUpdateTime(java.time.LocalDateTime.now());
      }
      checkName(_ctx, customer.getProperty(Customer.NAME_PROPERTY), newLocation(_parentLocation, Customer.NAME_PROPERTY));
      checkEmail(_ctx, customer.getProperty(Customer.EMAIL_PROPERTY), newLocation(_parentLocation, Customer.EMAIL_PROPERTY));
      checkCommercePlatform(_ctx, customer.getProperty(Customer.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, Customer.COMMERCE_PLATFORM_PROPERTY));
      checkCreateTime(_ctx, customer.getProperty(Customer.CREATE_TIME_PROPERTY), newLocation(_parentLocation, Customer.CREATE_TIME_PROPERTY));
      checkUpdateTime(_ctx, customer.getProperty(Customer.UPDATE_TIME_PROPERTY), newLocation(_parentLocation, Customer.UPDATE_TIME_PROPERTY));
      for(int i = 0; customer.getCustomerOrderList() != null && i < customer.getCustomerOrderList().size(); i++){
         CustomerOrder customerOrder = customer.getCustomerOrderList().get(i);
         new CustomerOrderChecker().checkAndFix(_ctx, customerOrder, newLocation(_parentLocation, Customer.CUSTOMER_ORDER_LIST_PROPERTY, i));
      }
    }

    public void checkName(UserContext _ctx, String name, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, name);
    if((name == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, name);

    }
    public void checkEmail(UserContext _ctx, String email, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, email);
    if((email == null)){
        return;
    }
    maxStringCheck(_ctx, _parentLocation, 100, email);

    }
    public void checkCommercePlatform(UserContext _ctx, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, commercePlatform);
    if((commercePlatform == null)){
        return;
    }
    new CommercePlatformChecker().checkAndFix(_ctx, commercePlatform, _parentLocation);
    }
    public void checkCreateTime(UserContext _ctx, LocalDateTime createTime, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, createTime);
    if((createTime == null)){
        return;
    }
    }
    public void checkUpdateTime(UserContext _ctx, LocalDateTime updateTime, ObjectLocation _parentLocation){
    requiredCheck(_ctx, _parentLocation, updateTime);
    if((updateTime == null)){
        return;
    }
    }
}