
package com.teaql.tracechainservice.orderitem;

import com.teaql.tracechainservice.customerorder.CustomerOrder;
import com.teaql.tracechainservice.customerorder.CustomerOrderExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class OrderItemExpression<T, E, U extends OrderItem> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public OrderItemExpression(Expression<T, U> expression){
        super(expression);
    }

    public OrderItemExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public OrderItemExpression<T, U, U> updateId(Long id){
        return new OrderItemExpression(this, $it -> {((OrderItem)$it).__internalSet("id", id); return this;});
     }

     public OrderItemExpression<T, U, U> save(UserContext userContext){
        return new OrderItemExpression(this, $it -> ((OrderItem)$it).auditAs("Saved by Expression").save(userContext));
     }

     public OrderItemExpression<T, U, U> save(String intent, UserContext userContext){
        return new OrderItemExpression(this, $it -> ((OrderItem)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public CustomerOrderExpression<T, U, CustomerOrder> getCustomerOrder(){
       return new CustomerOrderExpression(loaded("customerOrder", OrderItem::getCustomerOrder));
    }

    public OrderItemExpression<T, U, U> updateCustomerOrder(CustomerOrder customerOrder){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateCustomerOrder(customerOrder));
    }

    public Expression<T, String> getName(){
       return loaded("name", OrderItem::getName);
    }
    public OrderItemExpression<T, U, U> updateName(String name){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateName(name));
    }

}