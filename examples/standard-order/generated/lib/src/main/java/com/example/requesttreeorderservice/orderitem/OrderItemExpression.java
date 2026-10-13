
package com.example.requesttreeorderservice.orderitem;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
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

    public Expression<T, String> getProductName(){
       return loaded("productName", OrderItem::getProductName);
    }
    public OrderItemExpression<T, U, U> updateProductName(String productName){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateProductName(productName));
    }

    public Expression<T, Integer> getQuantity(){
       return loaded("quantity", OrderItem::getQuantity);
    }
    public OrderItemExpression<T, U, U> updateQuantity(Integer quantity){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateQuantity(quantity));
    }

    public Expression<T, BigDecimal> getUnitPrice(){
       return loaded("unitPrice", OrderItem::getUnitPrice);
    }
    public OrderItemExpression<T, U, U> updateUnitPrice(BigDecimal unitPrice){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateUnitPrice(unitPrice));
    }

}