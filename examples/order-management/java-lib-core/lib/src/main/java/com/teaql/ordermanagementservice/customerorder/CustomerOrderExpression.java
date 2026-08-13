
package com.teaql.ordermanagementservice.customerorder;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformExpression;
import com.teaql.ordermanagementservice.customer.Customer;
import com.teaql.ordermanagementservice.customer.CustomerExpression;
import com.teaql.ordermanagementservice.orderline.OrderLine;
import com.teaql.ordermanagementservice.orderline.OrderLineListExpression;
import com.teaql.ordermanagementservice.orderstatus.OrderStatus;
import com.teaql.ordermanagementservice.orderstatus.OrderStatusExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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


    public Expression<T, String> getOrderNumber(){
       return apply(CustomerOrder::getOrderNumber);
    }
    public CustomerOrderExpression<T, U, U> updateOrderNumber(String orderNumber){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateOrderNumber(orderNumber));
    }

    public Expression<T, LocalDate> getOrderDate(){
       return apply(CustomerOrder::getOrderDate);
    }
    public CustomerOrderExpression<T, U, U> updateOrderDate(LocalDate orderDate){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateOrderDate(orderDate));
    }

    public Expression<T, BigDecimal> getTotalAmount(){
       return apply(CustomerOrder::getTotalAmount);
    }
    public CustomerOrderExpression<T, U, U> updateTotalAmount(BigDecimal totalAmount){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateTotalAmount(totalAmount));
    }

    public OrderStatusExpression<T, U, OrderStatus> getStatus(){
       return new OrderStatusExpression(this, $it ->  ((CustomerOrder)$it).getStatus());
    }

    public CustomerOrderExpression<T, U, U> updateStatusToPending(){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateStatusToPending());
    }
    public CustomerOrderExpression<T, U, U> updateStatusToProcessing(){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateStatusToProcessing());
    }
    public CustomerOrderExpression<T, U, U> updateStatusToShipped(){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateStatusToShipped());
    }
    public CustomerOrderExpression<T, U, U> updateStatusToCompleted(){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateStatusToCompleted());
    }

    public CustomerExpression<T, U, Customer> getCustomer(){
       return new CustomerExpression(this, $it ->  ((CustomerOrder)$it).getCustomer());
    }

    public CustomerOrderExpression<T, U, U> updateCustomer(Customer customer){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateCustomer(customer));
    }

    public CommercePlatformExpression<T, U, CommercePlatform> getCommercePlatform(){
       return new CommercePlatformExpression(this, $it ->  ((CustomerOrder)$it).getCommercePlatform());
    }

    public CustomerOrderExpression<T, U, U> updateCommercePlatform(CommercePlatform commercePlatform){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateCommercePlatform(commercePlatform));
    }

    public Expression<T, LocalDateTime> getCreateTime(){
       return apply(CustomerOrder::getCreateTime);
    }
    public CustomerOrderExpression<T, U, U> updateCreateTime(LocalDateTime createTime){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateCreateTime(createTime));
    }

    public Expression<T, LocalDateTime> getUpdateTime(){
       return apply(CustomerOrder::getUpdateTime);
    }
    public CustomerOrderExpression<T, U, U> updateUpdateTime(LocalDateTime updateTime){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).updateUpdateTime(updateTime));
    }

    public OrderLineListExpression<T, U, OrderLine> getOrderLineList(){
        return new OrderLineListExpression(this, $it ->  ((CustomerOrder)$it).getOrderLineList());
    }
    public CustomerOrderExpression<T, U, U> addOrderLine(OrderLine orderLine){
       return new CustomerOrderExpression(this, $it ->  ((CustomerOrder)$it).addOrderLine(orderLine));
    }
}