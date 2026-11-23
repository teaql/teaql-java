
package com.teaql.orderattachmentservice.customerorder;

import com.teaql.orderattachmentservice.commerceplatform.CommercePlatform;
import com.teaql.orderattachmentservice.commerceplatform.CommercePlatformChecker;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachment;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachmentChecker;
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
        if(!customerOrder.isPropertyLoaded("order_number")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "order_number"), "Mutation requires a fully loaded entity");
        }
        if(!customerOrder.isPropertyLoaded("commerce_platform")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "commerce_platform"), "Mutation requires a fully loaded entity");
        }

      }
      checkOrderNumber(_context, customerOrder.getProperty(CustomerOrder.ORDER_NUMBER_PROPERTY), newLocation(_parentLocation, "order_number"));
      checkCommercePlatform(_context, customerOrder.getProperty(CustomerOrder.COMMERCE_PLATFORM_PROPERTY), newLocation(_parentLocation, "commerce_platform"));
      for(int i = 0; customerOrder.getOrderAttachmentList() != null && i < customerOrder.getOrderAttachmentList().size(); i++){
         OrderAttachment orderAttachment = customerOrder.getOrderAttachmentList().get(i);
         new OrderAttachmentChecker().checkAndFix(_context, orderAttachment, newLocation(_parentLocation, "order_attachment_list", i));
      }
    }

    public void checkOrderNumber(UserContext _context, String orderNumber, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, orderNumber);
    if((orderNumber == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, orderNumber);

    }
    public void checkCommercePlatform(UserContext _context, CommercePlatform commercePlatform, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, commercePlatform);
    if((commercePlatform == null)){
        return;
    }
    new CommercePlatformChecker().checkAndFix(_context, commercePlatform, _parentLocation);
    }
}