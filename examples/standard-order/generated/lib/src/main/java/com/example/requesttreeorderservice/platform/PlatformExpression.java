
package com.example.requesttreeorderservice.platform;

import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.customer.CustomerListExpression;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import com.example.requesttreeorderservice.orderstatus.OrderStatusListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class PlatformExpression<T, E, U extends Platform> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public PlatformExpression(Expression<T, U> expression){
        super(expression);
    }

    public PlatformExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public PlatformExpression<T, U, U> updateId(Long id){
        return new PlatformExpression(this, $it -> {((Platform)$it).__internalSet("id", id); return this;});
     }

     public PlatformExpression<T, U, U> save(UserContext userContext){
        return new PlatformExpression(this, $it -> ((Platform)$it).auditAs("Saved by Expression").save(userContext));
     }

     public PlatformExpression<T, U, U> save(String intent, UserContext userContext){
        return new PlatformExpression(this, $it -> ((Platform)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", Platform::getName);
    }
    public PlatformExpression<T, U, U> updateName(String name){
       return new PlatformExpression(this, $it ->  ((Platform)$it).updateName(name));
    }

    public OrderStatusListExpression<T, U, OrderStatus> getOrderStatusList(){
        return new OrderStatusListExpression(loaded("orderStatusList", Platform::getOrderStatusList));
    }
    public CustomerListExpression<T, U, Customer> getCustomerList(){
        return new CustomerListExpression(loaded("customerList", Platform::getCustomerList));
    }
    public PlatformExpression<T, U, U> addOrderStatus(OrderStatus orderStatus){
       return new PlatformExpression(this, $it ->  ((Platform)$it).addOrderStatus(orderStatus));
    }
    public PlatformExpression<T, U, U> addCustomer(Customer customer){
       return new PlatformExpression(this, $it ->  ((Platform)$it).addCustomer(customer));
    }
}