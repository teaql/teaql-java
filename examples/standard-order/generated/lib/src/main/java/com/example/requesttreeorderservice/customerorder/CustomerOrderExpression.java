
package com.example.requesttreeorderservice.customerorder;

import com.example.requesttreeorderservice.customer.Customer;
import com.example.requesttreeorderservice.customer.CustomerExpression;
import com.example.requesttreeorderservice.orderitem.OrderItem;
import com.example.requesttreeorderservice.orderitem.OrderItemListExpression;
import com.example.requesttreeorderservice.orderstatus.OrderStatus;
import com.example.requesttreeorderservice.orderstatus.OrderStatusExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

public class CustomerOrderExpression<T, E, U extends CustomerOrder> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public CustomerOrderExpression(Expression<T, U> expression){
        super(expression);
    }

    public CustomerOrderExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public CustomerOrderExpression<T, U, U> updateId(Long id){
        return new CustomerOrderExpression(this, $it -> {((CustomerOrder)$it).__internalSet("id", id); return this;});
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


    public CustomerExpression<T, U, Customer> getCustomer(){
       return new CustomerExpression(loaded("customer", CustomerOrder::getCustomer));
    }

    public CustomerOrderExpression<T, U, U> updateCustomer(Customer customer){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateCustomer(customer));
    }

    public OrderStatusExpression<T, U, OrderStatus> getStatus(){
       return new OrderStatusExpression(loaded("status", CustomerOrder::getStatus));
    }

    public CustomerOrderExpression<T, U, U> updateStatusToNew(){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateStatusToNew());
    }
    public CustomerOrderExpression<T, U, U> updateStatusToPaid(){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateStatusToPaid());
    }
    public CustomerOrderExpression<T, U, U> updateStatusToCancelled(){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateStatusToCancelled());
    }

    public Expression<T, String> getOrderNumber(){
       return loaded("orderNumber", CustomerOrder::getOrderNumber);
    }
    public CustomerOrderExpression<T, U, U> updateOrderNumber(String orderNumber){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateOrderNumber(orderNumber));
    }

    public Expression<T, LocalDate> getOrderDate(){
       return loaded("orderDate", CustomerOrder::getOrderDate);
    }
    public CustomerOrderExpression<T, U, U> updateOrderDate(LocalDate orderDate){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateOrderDate(orderDate));
    }

    public Expression<T, BigDecimal> getTotalAmount(){
       return loaded("totalAmount", CustomerOrder::getTotalAmount);
    }
    public CustomerOrderExpression<T, U, U> updateTotalAmount(BigDecimal totalAmount){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateTotalAmount(totalAmount));
    }

    public OrderItemListExpression<T, U, OrderItem> getOrderItemList(){
        return new OrderItemListExpression(loaded("orderItemList", CustomerOrder::getOrderItemList));
    }
    public CustomerOrderExpression<T, U, U> addOrderItem(OrderItem orderItem){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).addOrderItem(orderItem));
    }
}