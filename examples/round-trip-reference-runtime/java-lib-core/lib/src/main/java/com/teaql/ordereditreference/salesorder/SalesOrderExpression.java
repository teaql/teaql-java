
package com.teaql.ordereditreference.salesorder;

import com.teaql.ordereditreference.merchant.Merchant;
import com.teaql.ordereditreference.merchant.MerchantExpression;
import com.teaql.ordereditreference.orderitem.OrderItem;
import com.teaql.ordereditreference.orderitem.OrderItemListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class SalesOrderExpression<T, E, U extends SalesOrder> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public SalesOrderExpression(Expression<T, U> expression){
        super(expression);
    }

    public SalesOrderExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public SalesOrderExpression<T, U, U> save(UserContext userContext){
        return new SalesOrderExpression(this, $it -> ((SalesOrder)$it).auditAs("Saved by Expression").save(userContext));
     }

     public SalesOrderExpression<T, U, U> save(String intent, UserContext userContext){
        return new SalesOrderExpression(this, $it -> ((SalesOrder)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public MerchantExpression<T, U, Merchant> getMerchant(){
       return new MerchantExpression(loaded("merchant", "getMerchant", SalesOrder::getMerchant));
    }

    public SalesOrderExpression<T, U, U> updateMerchant(Merchant merchant){
       return new SalesOrderExpression(this, $it ->  ((SalesOrder)$it).updateMerchant(merchant));
    }

    public Expression<T, String> getOrderNumber(){
       return loaded("order_number", "getOrderNumber", SalesOrder::getOrderNumber);
    }
    public SalesOrderExpression<T, U, U> updateOrderNumber(String orderNumber){
       return new SalesOrderExpression(this, $it ->  ((SalesOrder)$it).updateOrderNumber(orderNumber));
    }

    public OrderItemListExpression<T, U, OrderItem> getOrderItemList(){
        return new OrderItemListExpression(loaded("order_item_list", "getOrderItemList", SalesOrder::getOrderItemList));
    }
    public SalesOrderExpression<T, U, U> addOrderItem(OrderItem orderItem){
       return new SalesOrderExpression(this, $it ->  ((SalesOrder)$it).addOrderItem(orderItem));
    }
}