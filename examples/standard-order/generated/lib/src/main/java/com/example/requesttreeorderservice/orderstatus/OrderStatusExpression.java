
package com.example.requesttreeorderservice.orderstatus;

import com.example.requesttreeorderservice.customerorder.CustomerOrder;
import com.example.requesttreeorderservice.customerorder.CustomerOrderListExpression;
import com.example.requesttreeorderservice.platform.Platform;
import com.example.requesttreeorderservice.platform.PlatformExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.math.BigDecimal;
import java.util.function.Function;

public class OrderStatusExpression<T, E, U extends OrderStatus> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public OrderStatusExpression(Expression<T, U> expression){
        super(expression);
    }

    public OrderStatusExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public OrderStatusExpression<T, U, U> updateId(Long id){
        return new OrderStatusExpression(this, $it -> {((OrderStatus)$it).__internalSet("id", id); return this;});
     }

     public OrderStatusExpression<T, U, U> save(UserContext userContext){
        return new OrderStatusExpression(this, $it -> ((OrderStatus)$it).auditAs("Saved by Expression").save(userContext));
     }

     public OrderStatusExpression<T, U, U> save(String intent, UserContext userContext){
        return new OrderStatusExpression(this, $it -> ((OrderStatus)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public PlatformExpression<T, U, Platform> getPlatform(){
       return new PlatformExpression(loaded("platform", OrderStatus::getPlatform));
    }

    public OrderStatusExpression<T, U, U> updatePlatform(Platform platform){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updatePlatform(platform));
    }

    public Expression<T, String> getName(){
       return loaded("name", OrderStatus::getName);
    }
    public OrderStatusExpression<T, U, U> updateName(String name){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateName(name));
    }

    public Expression<T, String> getCode(){
       return loaded("code", OrderStatus::getCode);
    }
    public OrderStatusExpression<T, U, U> updateCode(String code){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateCode(code));
    }

    public Expression<T, String> getColor(){
       return loaded("color", OrderStatus::getColor);
    }
    public OrderStatusExpression<T, U, U> updateColor(String color){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateColor(color));
    }

    public Expression<T, BigDecimal> getDisplayOrder(){
       return loaded("displayOrder", OrderStatus::getDisplayOrder);
    }
    public OrderStatusExpression<T, U, U> updateDisplayOrder(BigDecimal displayOrder){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateDisplayOrder(displayOrder));
    }

    public CustomerOrderListExpression<T, U, CustomerOrder> getCustomerOrderList(){
        return new CustomerOrderListExpression(loaded("customerOrderList", OrderStatus::getCustomerOrderList));
    }
    public OrderStatusExpression<T, U, U> addCustomerOrder(CustomerOrder customerOrder){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).addCustomerOrder(customerOrder));
    }
}