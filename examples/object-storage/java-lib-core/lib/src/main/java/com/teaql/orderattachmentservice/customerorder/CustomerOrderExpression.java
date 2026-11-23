
package com.teaql.orderattachmentservice.customerorder;

import com.teaql.orderattachmentservice.commerceplatform.CommercePlatform;
import com.teaql.orderattachmentservice.commerceplatform.CommercePlatformExpression;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachment;
import com.teaql.orderattachmentservice.orderattachment.OrderAttachmentListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class CustomerOrderExpression<T, E, U extends CustomerOrder> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CustomerOrderExpression(Expression<T, U> expression){
        super(expression);
    }

    public CustomerOrderExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CustomerOrderExpression<T, U, U> save(UserContext userContext){
        return new CustomerOrderExpression(this, $it -> ((CustomerOrder)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CustomerOrderExpression<T, U, U> save(String intent, UserContext userContext){
        return new CustomerOrderExpression(this, $it -> ((CustomerOrder)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getOrderNumber(){
       return loaded("order_number", "getOrderNumber", CustomerOrder::getOrderNumber);
    }
    public CustomerOrderExpression<T, U, U> updateOrderNumber(String orderNumber){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateOrderNumber(orderNumber));
    }

    public CommercePlatformExpression<T, U, CommercePlatform> getCommercePlatform(){
       return new CommercePlatformExpression(loaded("commerce_platform", "getCommercePlatform", CustomerOrder::getCommercePlatform));
    }

    public CustomerOrderExpression<T, U, U> updateCommercePlatform(CommercePlatform commercePlatform){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateCommercePlatform(commercePlatform));
    }

    public OrderAttachmentListExpression<T, U, OrderAttachment> getOrderAttachmentList(){
        return new OrderAttachmentListExpression(loaded("order_attachment_list", "getOrderAttachmentList", CustomerOrder::getOrderAttachmentList));
    }
    public CustomerOrderExpression<T, U, U> addOrderAttachment(OrderAttachment orderAttachment){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).addOrderAttachment(orderAttachment));
    }
}