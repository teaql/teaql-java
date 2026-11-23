
package com.teaql.orderattachmentservice.commerceplatform;

import com.teaql.orderattachmentservice.customerorder.CustomerOrder;
import com.teaql.orderattachmentservice.customerorder.CustomerOrderListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class CommercePlatformExpression<T, E, U extends CommercePlatform> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CommercePlatformExpression(Expression<T, U> expression){
        super(expression);
    }

    public CommercePlatformExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CommercePlatformExpression<T, U, U> updateId(Long id){
        return new CommercePlatformExpression(this, $it -> {((CommercePlatform)$it).__internalSet("id", id); return this;});
     }

     public CommercePlatformExpression<T, U, U> save(UserContext userContext){
        return new CommercePlatformExpression(this, $it -> ((CommercePlatform)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CommercePlatformExpression<T, U, U> save(String intent, UserContext userContext){
        return new CommercePlatformExpression(this, $it -> ((CommercePlatform)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", CommercePlatform::getName);
    }
    public CommercePlatformExpression<T, U, U> updateName(String name){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).updateName(name));
    }

    public CustomerOrderListExpression<T, U, CustomerOrder> getCustomerOrderList(){
        return new CustomerOrderListExpression(loaded("customer_order_list", "getCustomerOrderList", CommercePlatform::getCustomerOrderList));
    }
    public CommercePlatformExpression<T, U, U> addCustomerOrder(CustomerOrder customerOrder){
       return new CommercePlatformExpression(this, $it ->  ((CommercePlatform)$it).addCustomerOrder(customerOrder));
    }
}