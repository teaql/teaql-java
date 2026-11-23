
package com.teaql.ordereditreference.orderitem;

import com.teaql.ordereditreference.salesorder.SalesOrder;
import com.teaql.ordereditreference.salesorder.SalesOrderExpression;
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

     public OrderItemExpression<T, U, U> save(UserContext userContext){
        return new OrderItemExpression(this, $it -> ((OrderItem)$it).auditAs("Saved by Expression").save(userContext));
     }

     public OrderItemExpression<T, U, U> save(String intent, UserContext userContext){
        return new OrderItemExpression(this, $it -> ((OrderItem)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public SalesOrderExpression<T, U, SalesOrder> getSalesOrder(){
       return new SalesOrderExpression(loaded("sales_order", "getSalesOrder", OrderItem::getSalesOrder));
    }

    public OrderItemExpression<T, U, U> updateSalesOrder(SalesOrder salesOrder){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateSalesOrder(salesOrder));
    }

    public Expression<T, String> getSku(){
       return loaded("sku", "getSku", OrderItem::getSku);
    }
    public OrderItemExpression<T, U, U> updateSku(String sku){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateSku(sku));
    }

    public Expression<T, Integer> getQuantity(){
       return loaded("quantity", "getQuantity", OrderItem::getQuantity);
    }
    public OrderItemExpression<T, U, U> updateQuantity(Integer quantity){
       return new OrderItemExpression(this, $it ->  ((OrderItem)$it).updateQuantity(quantity));
    }

}