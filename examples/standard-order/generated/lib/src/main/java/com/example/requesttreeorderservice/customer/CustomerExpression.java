
package com.example.requesttreeorderservice.customer;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderListExpression;
import com.example.requesttreeorderservice.platform.Platform;
import com.example.requesttreeorderservice.platform.PlatformExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class CustomerExpression<T, E, U extends Customer> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CustomerExpression(Expression<T, U> expression){
        super(expression);
    }

    public CustomerExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CustomerExpression<T, U, U> updateId(Long id){
        return new CustomerExpression(this, $it -> {((Customer)$it).__internalSet("id", id); return this;});
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


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", Customer::getPlatform));
    }

    public CustomerExpression<T, U, U> updatePlatform(Platform platform){
       return new CustomerExpression(this, $it ->  ((Customer)$it).updatePlatform(platform));
    }

    public Expression<T, String> getName(){
       return loaded("name", Customer::getName);
    }
    public CustomerExpression<T, U, U> updateName(String name){
       return new CustomerExpression(this, $it ->  ((Customer)$it).updateName(name));
    }

    public CustomerOrderListExpression<T, U, CustomerOrder> getCustomerOrderList(){
        return new CustomerOrderListExpression(loaded("customerOrderList", Customer::getCustomerOrderList));
    }
    public CustomerExpression<T, U, U> addCustomerOrder(CustomerOrder customerOrder){
       return new CustomerExpression(this, $it ->  ((Customer)$it).addCustomerOrder(customerOrder));
    }
}