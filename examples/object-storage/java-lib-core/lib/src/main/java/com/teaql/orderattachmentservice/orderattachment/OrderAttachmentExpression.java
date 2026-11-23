
package com.teaql.orderattachmentservice.orderattachment;

import com.teaql.orderattachmentservice.customerorder.CustomerOrder;
import com.teaql.orderattachmentservice.customerorder.CustomerOrderExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class OrderAttachmentExpression<T, E, U extends OrderAttachment> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public OrderAttachmentExpression(Expression<T, U> expression){
        super(expression);
    }

    public OrderAttachmentExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public OrderAttachmentExpression<T, U, U> updateId(Long id){
        return new OrderAttachmentExpression(this, $it -> {((OrderAttachment)$it).__internalSet("id", id); return this;});
     }

     public OrderAttachmentExpression<T, U, U> save(UserContext userContext){
        return new OrderAttachmentExpression(this, $it -> ((OrderAttachment)$it).auditAs("Saved by Expression").save(userContext));
     }

     public OrderAttachmentExpression<T, U, U> save(String intent, UserContext userContext){
        return new OrderAttachmentExpression(this, $it -> ((OrderAttachment)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public CustomerOrderExpression<T, U, CustomerOrder> getCustomerOrder(){
       return new CustomerOrderExpression(loaded("customer_order", "getCustomerOrder", OrderAttachment::getCustomerOrder));
    }

    public OrderAttachmentExpression<T, U, U> updateCustomerOrder(CustomerOrder customerOrder){
       return new OrderAttachmentExpression(this, $it ->  ((OrderAttachment)$it).updateCustomerOrder(customerOrder));
    }

    public Expression<T, String> getFileName(){
       return loaded("file_name", "getFileName", OrderAttachment::getFileName);
    }
    public OrderAttachmentExpression<T, U, U> updateFileName(String fileName){
       return new OrderAttachmentExpression(this, $it ->  ((OrderAttachment)$it).updateFileName(fileName));
    }

    public Expression<T, String> getContentType(){
       return loaded("content_type", "getContentType", OrderAttachment::getContentType);
    }
    public OrderAttachmentExpression<T, U, U> updateContentType(String contentType){
       return new OrderAttachmentExpression(this, $it ->  ((OrderAttachment)$it).updateContentType(contentType));
    }

    public Expression<T, Integer> getContentLength(){
       return loaded("content_length", "getContentLength", OrderAttachment::getContentLength);
    }
    public OrderAttachmentExpression<T, U, U> updateContentLength(Integer contentLength){
       return new OrderAttachmentExpression(this, $it ->  ((OrderAttachment)$it).updateContentLength(contentLength));
    }

    public Expression<T, String> getContentSha256(){
       return loaded("content_sha256", "getContentSha256", OrderAttachment::getContentSha256);
    }
    public OrderAttachmentExpression<T, U, U> updateContentSha256(String contentSha256){
       return new OrderAttachmentExpression(this, $it ->  ((OrderAttachment)$it).updateContentSha256(contentSha256));
    }

    public Expression<T, String> getObjectKey(){
       return loaded("object_key", "getObjectKey", OrderAttachment::getObjectKey);
    }
    public OrderAttachmentExpression<T, U, U> updateObjectKey(String objectKey){
       return new OrderAttachmentExpression(this, $it ->  ((OrderAttachment)$it).updateObjectKey(objectKey));
    }

}