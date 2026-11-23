
package com.teaql.ordermanagementservice.orderstatus;

import com.teaql.ordermanagementservice.commerceplatform.CommercePlatform;
import com.teaql.ordermanagementservice.commerceplatform.CommercePlatformExpression;
import com.teaql.ordermanagementservice.customerorder.CustomerOrder;
import com.teaql.ordermanagementservice.customerorder.CustomerOrderListExpression;
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

     public OrderStatusExpression<T, U, U> save(UserContext userContext){
        return new OrderStatusExpression(this, $it -> ((OrderStatus)$it).auditAs("Saved by Expression").save(userContext));
     }

     public OrderStatusExpression<T, U, U> save(String intent, UserContext userContext){
        return new OrderStatusExpression(this, $it -> ((OrderStatus)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", OrderStatus::getName);
    }
    public OrderStatusExpression<T, U, U> updateName(String name){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateName(name));
    }

    public Expression<T, String> getCode(){
       return loaded("code", "getCode", OrderStatus::getCode);
    }
    public OrderStatusExpression<T, U, U> updateCode(String code){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateCode(code));
    }

    public Expression<T, String> getColor(){
       return loaded("color", "getColor", OrderStatus::getColor);
    }
    public OrderStatusExpression<T, U, U> updateColor(String color){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateColor(color));
    }

    public Expression<T, BigDecimal> getDisplayOrder(){
       return loaded("display_order", "getDisplayOrder", OrderStatus::getDisplayOrder);
    }
    public OrderStatusExpression<T, U, U> updateDisplayOrder(BigDecimal displayOrder){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateDisplayOrder(displayOrder));
    }

    public CommercePlatformExpression<T, U, CommercePlatform> getCommercePlatform(){
       return new CommercePlatformExpression(loaded("commerce_platform", "getCommercePlatform", OrderStatus::getCommercePlatform));
    }

    public OrderStatusExpression<T, U, U> updateCommercePlatform(CommercePlatform commercePlatform){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).updateCommercePlatform(commercePlatform));
    }

    public CustomerOrderListExpression<T, U, CustomerOrder> getCustomerOrderList(){
        return new CustomerOrderListExpression(loaded("customer_order_list", "getCustomerOrderList", OrderStatus::getCustomerOrderList));
    }
    public OrderStatusExpression<T, U, U> addCustomerOrder(CustomerOrder customerOrder){
       return new OrderStatusExpression(this, $it ->  ((OrderStatus)$it).addCustomerOrder(customerOrder));
    }
}