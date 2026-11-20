
package com.teaql.orderattachmentservice.orderattachment;

import com.teaql.orderattachmentservice.customerorder.CustomerOrder;
import com.teaql.orderattachmentservice.customerorder.CustomerOrderChecker;
import io.teaql.core.UserContext;
import io.teaql.core.checker.Checker;
import io.teaql.core.checker.ObjectLocation;

public class OrderAttachmentChecker implements Checker<OrderAttachment>{

    public String type(){
        return OrderAttachment.INTERNAL_TYPE;
    }

    public void checkAndFix(UserContext _context, OrderAttachment orderAttachment, ObjectLocation _parentLocation){
        if(needCheck(_context, orderAttachment)){
            markAsChecked(_context, orderAttachment);
            doCheck(_context, orderAttachment, _parentLocation);
        }
    }

    public void doCheck(UserContext _context, OrderAttachment orderAttachment, ObjectLocation _parentLocation){
      if((orderAttachment == null)){
         return;
      }
      boolean _contextFixEligible = needContextFix(_context, orderAttachment);
      if(orderAttachment.needPersist()){
      }
      if(orderAttachment.newItem()){
      }else if(orderAttachment.updateItem()){
        if(!orderAttachment.isPropertyLoaded("customer_order")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "customer_order"), "Mutation requires a fully loaded entity");
        }
        if(!orderAttachment.isPropertyLoaded("file_name")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "file_name"), "Mutation requires a fully loaded entity");
        }
        if(!orderAttachment.isPropertyLoaded("content_type")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "content_type"), "Mutation requires a fully loaded entity");
        }
        if(!orderAttachment.isPropertyLoaded("content_length")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "content_length"), "Mutation requires a fully loaded entity");
        }
        if(!orderAttachment.isPropertyLoaded("content_sha256")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "content_sha256"), "Mutation requires a fully loaded entity");
        }
        if(!orderAttachment.isPropertyLoaded("object_key")){
           invalidTypeCheck(_context, newLocation(_parentLocation, "object_key"), "Mutation requires a fully loaded entity");
        }

      }
      checkCustomerOrder(_context, orderAttachment.getProperty(OrderAttachment.CUSTOMER_ORDER_PROPERTY), newLocation(_parentLocation, "customer_order"));
      checkFileName(_context, orderAttachment.getProperty(OrderAttachment.FILE_NAME_PROPERTY), newLocation(_parentLocation, "file_name"));
      checkContentType(_context, orderAttachment.getProperty(OrderAttachment.CONTENT_TYPE_PROPERTY), newLocation(_parentLocation, "content_type"));
      checkContentLength(_context, orderAttachment.getProperty(OrderAttachment.CONTENT_LENGTH_PROPERTY), newLocation(_parentLocation, "content_length"));
      checkContentSha256(_context, orderAttachment.getProperty(OrderAttachment.CONTENT_SHA256_PROPERTY), newLocation(_parentLocation, "content_sha256"));
      checkObjectKey(_context, orderAttachment.getProperty(OrderAttachment.OBJECT_KEY_PROPERTY), newLocation(_parentLocation, "object_key"));
    }

    public void checkCustomerOrder(UserContext _context, CustomerOrder customerOrder, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, customerOrder);
    if((customerOrder == null)){
        return;
    }
    new CustomerOrderChecker().checkAndFix(_context, customerOrder, _parentLocation);
    }
    public void checkFileName(UserContext _context, String fileName, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, fileName);
    if((fileName == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, fileName);

    }
    public void checkContentType(UserContext _context, String contentType, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, contentType);
    if((contentType == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, contentType);

    }
    public void checkContentLength(UserContext _context, Integer contentLength, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, contentLength);
    if((contentLength == null)){
        return;
    }
    }
    public void checkContentSha256(UserContext _context, String contentSha256, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, contentSha256);
    if((contentSha256 == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, contentSha256);

    }
    public void checkObjectKey(UserContext _context, String objectKey, ObjectLocation _parentLocation){
    requiredCheck(_context, _parentLocation, objectKey);
    if((objectKey == null)){
        return;
    }
    maxStringCheck(_context, _parentLocation, 100, objectKey);

    }
}