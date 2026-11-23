
package com.teaql.ordermanagementservice.customer;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformExpression;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.time.LocalDateTime;
import java.util.function.Function;

public class CustomerExpression<T, E, U extends Customer> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CustomerExpression(Expression<T, U> expression){
        super(expression);
    }

    public CustomerExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CustomerExpression<T, U, U> save(UserContext userContext){
        return new CustomerExpression(this, $it -> ((Customer)$it).auditAs("Saved by Expression").save(userContext));
     }

     public CustomerExpression<T, U, U> save(String intent, UserContext userContext){
        return new CustomerExpression(this, $it -> ((Customer)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", Customer::getName);
    }
    public CustomerExpression<T, U, U> updateName(String name){
       return new CustomerExpression(this, $it ->  ((Customer)$it).updateName(name));
    }

    public Expression<T, String> getEmail(){
       return loaded("email", "getEmail", Customer::getEmail);
    }
    public CustomerExpression<T, U, U> updateEmail(String email){
       return new CustomerExpression(this, $it ->  ((Customer)$it).updateEmail(email));
    }

    public CommercePlatformExpression<T, U, CommercePlatform> getCommercePlatform(){
       return new CommercePlatformExpression(loaded("commerce_platform", "getCommercePlatform", Customer::getCommercePlatform));
    }

    public CustomerExpression<T, U, U> updateCommercePlatform(CommercePlatform commercePlatform){
       return new CustomerExpression(this, $it ->  ((Customer)$it).updateCommercePlatform(commercePlatform));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return loaded("create_time", "getCreateTime", Customer::getCreateTime);
    }
    public CustomerExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new CustomerExpression(this, $it ->  ((Customer)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return loaded("update_time", "getUpdateTime", Customer::getUpdateTime);
    }
    public CustomerExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new CustomerExpression(this, $it ->  ((Customer)$it).updateUpdateTime(updateTime));
    }

    public CustomerOrderListExpression<T, U, CustomerOrder> getCustomerOrderList(){
        return new CustomerOrderListExpression(loaded("customer_order_list", "getCustomerOrderList", Customer::getCustomerOrderList));
    }
    public CustomerExpression<T, U, U> addCustomerOrder(CustomerOrder customerOrder){
       return new CustomerExpression(this, $it ->  ((Customer)$it).addCustomerOrder(customerOrder));
    }
}