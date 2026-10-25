
package com.teaql.checkerfixadvanced.tenant;

import com.teaql.checkerfixadvanced.orderitem.OrderItem;
import com.teaql.checkerfixadvanced.orderitem.OrderItemListExpression;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrder;
import com.teaql.checkerfixadvanced.purchaseorder.PurchaseOrderListExpression;
import io.teaql.core.UserContext;
import io.teaql.core.value.BaseEntityExpression;
import io.teaql.core.value.Expression;
import io.teaql.core.value.ExpressionAdaptor;
import java.util.function.Function;

public class TenantExpression<T, E, U extends Tenant> extends ExpressionAdaptor<T, E, U> implements BaseEntityExpression<T, U> {
    public TenantExpression(Expression<T, U> expression){
        super(expression);
    }

    public TenantExpression(Expression<T, E> expression, Function<E, U> function){
        super(expression, function);
    }

     public TenantExpression<T, U, U> updateId(Long id){
        return new TenantExpression(this, $it -> {((Tenant)$it).__internalSet("id", id); return this;});
     }

     public TenantExpression<T, U, U> save(UserContext userContext){
        return new TenantExpression(this, $it -> ((Tenant)$it).auditAs("Saved by Expression").save(userContext));
     }

     public TenantExpression<T, U, U> save(String intent, UserContext userContext){
        return new TenantExpression(this, $it -> ((Tenant)$it).auditAs(intent).save(userContext));
     }

     public boolean isNull() {
        return resolve() == null;
     }


    public Expression<T, String> getName(){
       return loaded("name", "getName", Tenant::getName);
    }
    public TenantExpression<T, U, U> updateName(String name){
       return new TenantExpression(this, $it ->  ((Tenant)$it).updateName(name));
    }

    public PurchaseOrderListExpression<T, U, PurchaseOrder> getPurchaseOrderList(){
        return new PurchaseOrderListExpression(loaded("purchase_order_list", "getPurchaseOrderList", Tenant::getPurchaseOrderList));
    }
    public OrderItemListExpression<T, U, OrderItem> getOrderItemList(){
        return new OrderItemListExpression(loaded("order_item_list", "getOrderItemList", Tenant::getOrderItemList));
    }
    public TenantExpression<T, U, U> addPurchaseOrder(PurchaseOrder purchaseOrder){
       return new TenantExpression(this, $it ->  ((Tenant)$it).addPurchaseOrder(purchaseOrder));
    }
    public TenantExpression<T, U, U> addOrderItem(OrderItem orderItem){
       return new TenantExpression(this, $it ->  ((Tenant)$it).addOrderItem(orderItem));
    }
}